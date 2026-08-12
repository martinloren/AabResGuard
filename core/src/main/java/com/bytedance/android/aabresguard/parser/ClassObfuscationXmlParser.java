package com.bytedance.android.aabresguard.parser;

import com.bytedance.android.aabresguard.model.xml.ClassObfuscationConfig;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.nio.file.Path;
import java.util.Iterator;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Parser for <class-obfuscation> section of the config XML.
 */
public class ClassObfuscationXmlParser {
    private final Path configPath;

    public ClassObfuscationXmlParser(Path configPath) {
        checkFileExistsAndReadable(configPath);
        this.configPath = configPath;
    }

    public ClassObfuscationConfig parse() throws DocumentException {
        ClassObfuscationConfig config = new ClassObfuscationConfig();
        SAXReader reader = AabResGuardXmlParser.createSAXReader();
        Document doc = reader.read(configPath.toFile());
        Element root = doc.getRootElement();
        for (Iterator<Element> i = root.elementIterator("class-obfuscation"); i.hasNext(); ) {
            Element element = i.next();
            String isActive = element.attributeValue("isactive");
            if (isActive != null && isActive.equals("true")) {
                config.setActive(true);
            }
            for (Iterator<Element> rules = element.elementIterator("rule"); rules.hasNext(); ) {
                String value = rules.next().attributeValue("value");
                config.addRule(value);
            }
            for (Iterator<Element> whitelist = element.elementIterator("whitelist"); whitelist.hasNext(); ) {
                String value = whitelist.next().attributeValue("value");
                config.addWhitelist(value);
            }
        }
        return config;
    }
}
