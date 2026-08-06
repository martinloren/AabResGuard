package com.bytedance.android.aabresguard.parser;

import com.bytedance.android.aabresguard.model.xml.StringFilterConfig;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.nio.file.Path;
import java.util.Iterator;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Created by jiangzilai on 2019-10-20.
 */
public class StringFilterXmlParser {
    private final Path configPath;

    public StringFilterXmlParser(Path configPath) {
        checkFileExistsAndReadable(configPath);
        this.configPath = configPath;
    }

    public StringFilterConfig parse() throws DocumentException {
        StringFilterConfig config = new StringFilterConfig();
        SAXReader reader = AabResGuardXmlParser.createSAXReader();
        Document doc = reader.read(configPath.toFile());
        Element root = doc.getRootElement();

        for (Iterator<Element> i = root.elementIterator("filter-str"); i.hasNext(); ) {
            Element element = i.next();
            String isActive = element.attributeValue("isactive");
            if (isActive != null && isActive.toLowerCase().equals("true")) {
                config.setActive(true);
            }
            for (Iterator<Element> rules = element.elementIterator("path"); rules.hasNext(); ) {
                Element ruleElement = rules.next();
                String path = ruleElement.attributeValue("value");
                config.setPath(path);
            }
            for (Iterator<Element> rules = element.elementIterator("language"); rules.hasNext(); ) {
                Element ruleElement = rules.next();
                String path = ruleElement.attributeValue("value");
                config.getLanguageWhiteList().add(path);
            }
        }
        return config;
    }
}
