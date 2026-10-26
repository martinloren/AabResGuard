package com.bytedance.android.aabresguard.executors;

import com.android.aapt.Resources;
import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.model.BundleModuleName;
import com.android.tools.build.bundletool.model.ModuleEntry;
import com.bytedance.android.aabresguard.model.xml.ClassObfuscationConfig;
import com.bytedance.android.aabresguard.obfuscation.ResGuardStringBuilder;
import com.bytedance.android.aabresguard.utils.Utils;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.protobuf.InvalidProtocolBufferException;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction31c;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction21c;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction31c;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.EncodedValueRewriter;
import org.jf.dexlib2.rewriter.InstructionRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Obfuscates class names of custom views (and other selected classes) inside the
 * compiled DEX files and in binary XML layouts. The mapping is written to
 * class-mapping.txt in the output directory so that later builds can stay stable.
 */
public class ClassObfuscator implements Closeable {
    public static final String CLASS_MAPPING_NAME = "class-mapping.txt";
    private static final Logger logger = Logger.getLogger(ClassObfuscator.class.getName());

    private final AppBundle rawAppBundle;
    private final ClassObfuscationConfig config;
    private final Path outputDir;
    private final Path mappingPath;

    private final Set<Pattern> rulePatterns;
    private final Set<Pattern> whitelistPatterns;

    // old dotted class name -> new dotted class name
    private final Map<String, String> classMapping = new HashMap<>();
    // per-package name generators and used simple names
    private final Map<String, ResGuardStringBuilder> nameBuilders = new HashMap<>();
    private final Map<String, Set<String>> usedSimpleNames = new HashMap<>();

    public ClassObfuscator(Path bundlePath, AppBundle rawAppBundle, ClassObfuscationConfig config,
                           Path outputDir, Path mappingPath) {
        this.rawAppBundle = rawAppBundle;
        this.config = config;
        this.outputDir = outputDir;
        this.mappingPath = mappingPath;
        this.rulePatterns = config != null ? config.getRules().stream()
                .map(Utils::convertToPatternString)
                .map(Pattern::compile)
                .collect(Collectors.toSet()) : Collections.emptySet();
        this.whitelistPatterns = config != null ? config.getWhitelist().stream()
                .map(Utils::convertToPatternString)
                .map(Pattern::compile)
                .collect(Collectors.toSet()) : Collections.emptySet();
    }

    @Override
    public void close() {
        // nothing to close; content is read from in-memory ModuleEntry ByteSources
    }

    public AppBundle obfuscate() throws IOException {
        if (config == null || !config.isActive()) {
            return rawAppBundle;
        }
        loadInputMapping();
        collectClassMapping();
        if (classMapping.isEmpty()) {
            return rawAppBundle;
        }

        Map<BundleModuleName, BundleModule> obfuscatedModules = new HashMap<>();
        for (Map.Entry<BundleModuleName, BundleModule> entry : rawAppBundle.getModules().entrySet()) {
            obfuscatedModules.put(entry.getKey(), obfuscateModule(entry.getValue()));
        }
        AppBundle appBundle = rawAppBundle.toBuilder()
                .setModules(ImmutableMap.copyOf(obfuscatedModules))
                .build();
        writeClassMapping();
        System.out.println("obfuscate class names done, " + classMapping.size() + " classes renamed");
        return appBundle;
    }

    private void loadInputMapping() throws IOException {
        Path input = resolveInputMappingPath();
        if (input == null || !Files.exists(input)) {
            return;
        }
        for (String line : Files.readAllLines(input)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("class mapping:")) {
                continue;
            }
            int arrow = line.indexOf(" -> ");
            if (arrow < 0) {
                continue;
            }
            String oldName = line.substring(0, arrow).trim();
            String newName = line.substring(arrow + 4).trim();
            if (!oldName.isEmpty() && !newName.isEmpty()) {
                classMapping.put(oldName, newName);
            }
        }
    }

    private Path resolveInputMappingPath() {
        if (mappingPath != null) {
            return mappingPath;
        }
        return outputDir.resolve(CLASS_MAPPING_NAME);
    }

    private void collectClassMapping() throws IOException {
        // First pass: collect every class definition and group simple names by package.
        Map<String, Set<String>> existingSimpleNamesByPackage = new HashMap<>();
        List<ClassRef> toObfuscate = new ArrayList<>();
        Set<String> definedClasses = new HashSet<>();
        for (BundleModule module : rawAppBundle.getModules().values()) {
            for (ModuleEntry entry : module.getEntries()) {
                String path = entry.getPath().toString();
                if (!path.startsWith("dex/") || !path.endsWith(".dex")) {
                    continue;
                }
                byte[] bytes = readEntryBytes(entry);
                DexBackedDexFile dexFile = new DexBackedDexFile(Opcodes.getDefault(), bytes);
                for (ClassDef classDef : dexFile.getClasses()) {
                    String dotted = descriptorToDotted(classDef.getType());
                    String packageName = packageOf(dotted);
                    definedClasses.add(dotted);
                    existingSimpleNamesByPackage
                            .computeIfAbsent(packageName, k -> new HashSet<>())
                            .add(simpleNameOf(dotted));
                    if (shouldObfuscate(dotted)) {
                        toObfuscate.add(new ClassRef(packageName, dotted));
                    }
                }
            }
        }

        // Reserve every simple name the input mapping already handed out before a single
        // new name is generated. Reserving lazily, only for the classes the loop below
        // happens to reach, let a class added after the mapping was written take a name
        // that an already mapped class still owned; the DEX writer then aborted with
        // "Class <name> has already been interned".
        reserveMappedSimpleNames(existingSimpleNamesByPackage, definedClasses);

        // Second pass: assign new short names, skipping every name already in use.
        for (ClassRef ref : toObfuscate) {
            Set<String> used = usedNamesFor(ref.packageName, existingSimpleNamesByPackage);
            if (classMapping.containsKey(ref.dottedName)) {
                continue;
            }
            ResGuardStringBuilder builder = nameBuilders
                    .computeIfAbsent(ref.packageName, k -> {
                        ResGuardStringBuilder b = new ResGuardStringBuilder();
                        b.reset(null);
                        return b;
                    });
            String newSimpleName = builder.getReplaceString(used);
            used.add(newSimpleName);
            String newDotted = ref.packageName.isEmpty() ? newSimpleName : ref.packageName + "." + newSimpleName;
            classMapping.put(ref.dottedName, newDotted);
        }
        verifyUniqueTargetNames(definedClasses);
    }

    /**
     * Marks the target simple names of the input mapping as used, so generated names can
     * never clash with a class that was renamed by a previous run.
     */
    private void reserveMappedSimpleNames(Map<String, Set<String>> existingSimpleNamesByPackage,
                                          Set<String> definedClasses) {
        dropDuplicateTargetNames(definedClasses);
        for (Map.Entry<String, String> entry : classMapping.entrySet()) {
            String targetSimpleName = simpleNameOf(entry.getValue());
            usedNamesFor(packageOf(entry.getValue()), existingSimpleNamesByPackage).add(targetSimpleName);
            usedNamesFor(packageOf(entry.getKey()), existingSimpleNamesByPackage).add(targetSimpleName);
        }
    }

    /**
     * A hand-edited or previously mis-generated mapping can map two classes to the same
     * target. Keep the first claim so it stays stable and let the remaining classes be
     * renamed again, otherwise the bundle would contain a duplicated class definition.
     */
    private void dropDuplicateTargetNames(Set<String> definedClasses) {
        Map<String, List<String>> sourcesByTarget = new TreeMap<>();
        for (Map.Entry<String, String> entry : classMapping.entrySet()) {
            if (definedClasses.contains(entry.getKey())) {
                sourcesByTarget.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        for (Map.Entry<String, List<String>> entry : sourcesByTarget.entrySet()) {
            List<String> sources = entry.getValue();
            if (sources.size() < 2) {
                continue;
            }
            Collections.sort(sources);
            for (String droppedSource : sources.subList(1, sources.size())) {
                classMapping.remove(droppedSource);
                logger.warning(String.format("Drop duplicate class mapping: %s -> %s (already used by %s)",
                        droppedSource, entry.getKey(), sources.get(0)));
            }
        }
    }

    /**
     * Fails with an actionable message when two classes would end up with the same name.
     */
    private void verifyUniqueTargetNames(Set<String> definedClasses) {
        Map<String, String> sourcesByTarget = new HashMap<>();
        for (Map.Entry<String, String> entry : classMapping.entrySet()) {
            if (!definedClasses.contains(entry.getKey())) {
                continue;
            }
            String previousSource = sourcesByTarget.put(entry.getValue(), entry.getKey());
            if (previousSource != null) {
                throw new IllegalStateException(String.format(
                        "Class obfuscation assigned the same name '%s' to both %s and %s",
                        entry.getValue(), previousSource, entry.getKey()));
            }
        }
    }

    private Set<String> usedNamesFor(String packageName,
                                     Map<String, Set<String>> existingSimpleNamesByPackage) {
        return usedSimpleNames.computeIfAbsent(packageName, k ->
                new HashSet<>(existingSimpleNamesByPackage.getOrDefault(packageName, Collections.emptySet())));
    }

    private boolean shouldObfuscate(String dottedName) {
        boolean matchesRule = false;
        for (Pattern rule : rulePatterns) {
            if (rule.matcher(dottedName).matches()) {
                matchesRule = true;
                break;
            }
        }
        if (!matchesRule) {
            return false;
        }
        for (Pattern white : whitelistPatterns) {
            if (white.matcher(dottedName).matches()) {
                System.out.println(String.format("[class whitelist] skip class: %s", dottedName));
                return false;
            }
        }
        return true;
    }

    private byte[] readEntryBytes(ModuleEntry entry) throws IOException {
        try (java.io.InputStream is = entry.getContent().openStream()) {
            return org.apache.commons.io.IOUtils.toByteArray(is);
        }
    }

    private BundleModule obfuscateModule(BundleModule module) throws IOException {
        BundleModule.Builder builder = module.toBuilder();
        List<ModuleEntry> newEntries = new ArrayList<>();
        for (ModuleEntry entry : module.getEntries()) {
            String path = entry.getPath().toString();
            byte[] content = readEntryBytes(entry);
            if (path.startsWith("dex/") && path.endsWith(".dex")) {
                content = rewriteDex(content);
            } else if (path.startsWith("res/") && path.endsWith(".xml")) {
                content = rewriteXml(content, path);
            }
            newEntries.add(ModuleEntry.builder()
                    .setPath(entry.getPath())
                    .setContent(ByteSource.wrap(content))
                    .build());
        }
        builder.setRawEntries(newEntries);
        return builder.build();
    }

    private byte[] rewriteDex(byte[] bytes) throws IOException {
        DexBackedDexFile dexFile = new DexBackedDexFile(Opcodes.getDefault(), bytes);
        DexRewriter rewriter = new DexRewriter(new RewriterModule() {
            @Override
            public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
                return type -> {
                    String rewritten = rewriteType(type);
                    return rewritten != null ? rewritten : type;
                };
            }

            @Override
            public Rewriter<Instruction> getInstructionRewriter(Rewriters rewriters) {
                InstructionRewriter parent = new InstructionRewriter(rewriters);
                return instruction -> {
                    Instruction rewritten = parent.rewrite(instruction);
                    if (rewritten.getOpcode() == org.jf.dexlib2.Opcode.CONST_STRING && rewritten instanceof Instruction21c) {
                        Instruction21c instr = (Instruction21c) rewritten;
                        if (instr.getReference() instanceof StringReference) {
                            String oldValue = ((StringReference) instr.getReference()).getString();
                            String newValue = classMapping.get(oldValue);
                            if (newValue != null) {
                                return new ImmutableInstruction21c(
                                        org.jf.dexlib2.Opcode.CONST_STRING,
                                        instr.getRegisterA(),
                                        new ImmutableStringReference(newValue));
                            }
                        }
                    } else if (rewritten.getOpcode() == org.jf.dexlib2.Opcode.CONST_STRING_JUMBO && rewritten instanceof Instruction31c) {
                        Instruction31c instr = (Instruction31c) rewritten;
                        if (instr.getReference() instanceof StringReference) {
                            String oldValue = ((StringReference) instr.getReference()).getString();
                            String newValue = classMapping.get(oldValue);
                            if (newValue != null) {
                                return new ImmutableInstruction31c(
                                        org.jf.dexlib2.Opcode.CONST_STRING_JUMBO,
                                        instr.getRegisterA(),
                                        new ImmutableStringReference(newValue));
                            }
                        }
                    }
                    return rewritten;
                };
            }

            @Override
            public Rewriter<EncodedValue> getEncodedValueRewriter(Rewriters rewriters) {
                EncodedValueRewriter parent = new EncodedValueRewriter(rewriters);
                return value -> {
                    EncodedValue rewritten = parent.rewrite(value);
                    if (rewritten instanceof StringEncodedValue) {
                        String oldValue = ((StringEncodedValue) rewritten).getValue();
                        String newValue = classMapping.get(oldValue);
                        if (newValue != null) {
                            return new ImmutableStringEncodedValue(newValue);
                        }
                    }
                    return rewritten;
                };
            }
        });
        DexFile rewritten = rewriter.getDexFileRewriter().rewrite(dexFile);
        Path temp = Files.createTempFile("aabresguard-class", ".dex");
        try {
            DexFileFactory.writeDexFile(temp.toString(), rewritten);
            return Files.readAllBytes(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private String rewriteType(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        int arrayDepth = 0;
        String base = type;
        while (base.startsWith("[")) {
            arrayDepth++;
            base = base.substring(1);
        }
        if (!base.startsWith("L") || !base.endsWith(";")) {
            return null;
        }
        String dotted = base.substring(1, base.length() - 1).replace('/', '.');
        String newDotted = classMapping.get(dotted);
        if (newDotted == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrayDepth; i++) {
            sb.append('[');
        }
        sb.append('L').append(newDotted.replace('.', '/')).append(';');
        return sb.toString();
    }

    private byte[] rewriteXml(byte[] bytes, String path) {
        Resources.XmlNode node;
        try {
            node = Resources.XmlNode.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            // res/raw/*.xml entries are stored as plain-text or legacy binary
            // (AXML) XML rather than as XmlNode proto; nothing to rewrite in them.
            logger.info("skip non-proto XML file: " + path);
            return bytes;
        }
        try {
            Resources.XmlNode rewritten = rewriteXmlNode(node);
            return rewritten.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to rewrite binary XML for class obfuscation: " + path, e);
        }
    }

    private Resources.XmlNode rewriteXmlNode(Resources.XmlNode node) {
        if (node.hasElement()) {
            Resources.XmlElement element = node.getElement();
            Resources.XmlElement.Builder elementBuilder = element.toBuilder();
            String newName = classMapping.get(element.getName());
            if (newName != null) {
                elementBuilder.setName(newName);
            }
            elementBuilder.clearAttribute();
            for (Resources.XmlAttribute attr : element.getAttributeList()) {
                String newValue = classMapping.get(attr.getValue());
                if (newValue != null) {
                    elementBuilder.addAttribute(attr.toBuilder().setValue(newValue).build());
                } else {
                    elementBuilder.addAttribute(attr);
                }
            }
            elementBuilder.clearChild();
            for (Resources.XmlNode child : element.getChildList()) {
                elementBuilder.addChild(rewriteXmlNode(child));
            }
            return node.toBuilder().setElement(elementBuilder).build();
        } else if (node.getNodeCase() == Resources.XmlNode.NodeCase.TEXT) {
            String newText = classMapping.get(node.getText());
            return newText != null ? node.toBuilder().setText(newText).build() : node;
        }
        return node;
    }

    private void writeClassMapping() throws IOException {
        Path mappingFile = outputDir.resolve(CLASS_MAPPING_NAME);
        if (Files.exists(mappingFile)) {
            logger.warning("Class mapping file already exists, deleting: " + mappingFile);
            Files.delete(mappingFile);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(mappingFile)) {
            writer.write("class mapping:\n");
            for (Map.Entry<String, String> entry : new TreeMap<>(classMapping).entrySet()) {
                writer.write(String.format("\t%s -> %s\n", entry.getKey(), entry.getValue()));
            }
        }
    }

    private static String descriptorToDotted(String descriptor) {
        String type = descriptor;
        // strip array prefixes; object classes are the only ones we rename
        while (type.startsWith("[")) {
            type = type.substring(1);
        }
        if (type.startsWith("L") && type.endsWith(";")) {
            type = type.substring(1, type.length() - 1);
        }
        return type.replace('/', '.');
    }

    private static String packageOf(String dottedName) {
        int lastDot = dottedName.lastIndexOf('.');
        return lastDot >= 0 ? dottedName.substring(0, lastDot) : "";
    }

    private static String simpleNameOf(String dottedName) {
        int lastDot = dottedName.lastIndexOf('.');
        return lastDot >= 0 ? dottedName.substring(lastDot + 1) : dottedName;
    }

    private static final class ClassRef {
        final String packageName;
        final String dottedName;

        ClassRef(String packageName, String dottedName) {
            this.packageName = packageName;
            this.dottedName = dottedName;
        }
    }
}
