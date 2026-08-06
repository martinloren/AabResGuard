package com.bytedance.android.aabresguard.parser;

import com.bytedance.android.aabresguard.model.xml.FileFilterConfig;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.nio.file.Path;
import java.util.Iterator;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class FileFilterXmlParser {
    private final Path configPath;

    public FileFilterXmlParser(Path configPath) {
        checkFileExistsAndReadable(configPath);
        this.configPath = configPath;
    }

    public FileFilterConfig parse() throws DocumentException {
        FileFilterConfig fileFilter = new FileFilterConfig();

        SAXReader reader = AabResGuardXmlParser.createSAXReader();
        Document doc = reader.read(configPath.toFile());
        Element root = doc.getRootElement();
        for (Iterator<Element> i = root.elementIterator("filter"); i.hasNext(); ) {
            Element element = i.next();
            String isActiveValue = element.attributeValue("isactive");
            if (isActiveValue != null && isActiveValue.equals("true")) {
                fileFilter.setActive(true);
            }
            for (Iterator<Element> rules = element.elementIterator("rule"); rules.hasNext(); ) {
                Element ruleElement = rules.next();
                String rule = ruleElement.attributeValue("value");
                if (rule != null) {
                    fileFilter.addRule(rule);
                }
            }
        }
        return fileFilter;
    }
}
