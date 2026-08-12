package com.bytedance.android.aabresguard.executors;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.bytedance.android.aabresguard.BaseTest;
import com.bytedance.android.aabresguard.bundle.AppBundleAnalyzer;
import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand;
import com.google.common.collect.ImmutableSet;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link ClassObfuscator} integrated through the obfuscate-bundle command.
 */
public class ClassObfuscatorTest extends BaseTest {

    @Test
    public void test_classObfuscationRenamesMatchingClasses() throws Exception {
        File rawAabFile = loadResourceFile("demo/demo.aab");
        File outputFile = new File(getTempDirPath().toFile(), "obfuscated-class.aab");
        ObfuscateBundleCommand.fromFlags(
                new com.android.tools.build.bundletool.flags.FlagParser().parse(
                        "--bundle=" + rawAabFile.getAbsolutePath(),
                        "--output=" + outputFile.getAbsolutePath(),
                        "--config=" + loadResourcePath("demo/config-class-obfuscation.xml"),
                        "--disable-sign=true"
                )
        ).execute();
        assertThat(outputFile.exists()).isTrue();

        Path mappingFile = outputFile.toPath().getParent().resolve(ClassObfuscator.CLASS_MAPPING_NAME);
        assertThat(mappingFile.toFile().exists()).isTrue();

        String mapping = new String(Files.readAllBytes(mappingFile));
        assertThat(mapping).contains("com.ss.android.ugc.model.Model1 ->");
        assertThat(mapping).contains("com.ss.android.ugc.model.Model2 ->");
        assertThat(mapping).contains("com.ss.android.ugc.model.Model3 ->");
        assertThat(mapping).doesNotContain("com.ss.android.ugc.R");

        // Verify the renamed short names actually appear inside the rewritten DEX files.
        AppBundle appBundle = new AppBundleAnalyzer(outputFile.toPath()).analyze();
        Set<String> remainingModelClasses = new HashSet<>();
        Set<String> obfuscatedShortNames = new HashSet<>();
        for (BundleModule module : appBundle.getModules().values()) {
            for (com.android.tools.build.bundletool.model.ModuleEntry entry : module.getEntries()) {
                String path = entry.getPath().toString();
                if (path.startsWith("dex/") && path.endsWith(".dex")) {
                    DexBackedDexFile dexFile = readDex(outputFile.toPath(), module, entry);
                    for (org.jf.dexlib2.iface.ClassDef classDef : dexFile.getClasses()) {
                        String dotted = classDef.getType().substring(1, classDef.getType().length() - 1).replace('/', '.');
                        if (dotted.startsWith("com.ss.android.ugc.model.")) {
                            remainingModelClasses.add(dotted);
                            obfuscatedShortNames.add(dotted.substring(dotted.lastIndexOf('.') + 1));
                        }
                    }
                }
            }
        }
        assertThat(remainingModelClasses).containsNoneOf(
                "com.ss.android.ugc.model.Model1",
                "com.ss.android.ugc.model.Model2",
                "com.ss.android.ugc.model.Model3");
        assertThat(obfuscatedShortNames).containsExactly("a", "b", "c");
    }

    private DexBackedDexFile readDex(Path aabPath, BundleModule module,
                                     com.android.tools.build.bundletool.model.ModuleEntry entry) throws IOException {
        String zipPath = module.getName().getName() + "/" + entry.getPath().toString();
        try (ZipFile zipFile = new ZipFile(aabPath.toFile())) {
            try (java.io.InputStream is = zipFile.getInputStream(zipFile.getEntry(zipPath));
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return new DexBackedDexFile(Opcodes.getDefault(), baos.toByteArray());
            }
        }
    }
}
