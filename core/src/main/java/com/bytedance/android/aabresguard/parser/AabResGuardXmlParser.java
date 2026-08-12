package com.bytedance.android.aabresguard.parser;

import com.bytedance.android.aabresguard.model.xml.AabResGuardConfig;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import java.nio.file.Path;
import java.util.Iterator;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class AabResGuardXmlParser {
    private final Path configPath;

    public AabResGuardXmlParser(Path configPath) {
        checkFileExistsAndReadable(configPath);
        this.configPath = configPath;
    }

    static SAXReader createSAXReader() {
        SAXReader reader = new SAXReader();
        try {
            // harden against XXE attacks
            reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (SAXException e) {
            // the underlying parser does not support this feature, ignore
        }
        return reader;
    }

    public AabResGuardConfig parse() throws DocumentException {
        AabResGuardConfig aabResGuardConfig = new AabResGuardConfig();
        SAXReader reader = createSAXReader();
        Document doc = reader.read(configPath.toFile());
        Element root = doc.getRootElement();
        for (Iterator<Element> i = root.elementIterator("issue"); i.hasNext(); ) {
            Element element = i.next();
            String id = element.attributeValue("id");
            if (id == null || !id.equals("whitelist")) {
                continue;
            }
            String isActive = element.attributeValue("isactive");
            if (isActive != null && isActive.equals("true")) {
                aabResGuardConfig.setUseWhiteList(true);
            }
            for (Iterator<Element> rules = element.elementIterator("path"); rules.hasNext(); ) {
                Element ruleElement = rules.next();
                String rule = ruleElement.attributeValue("value");
                if (rule != null) {
                    aabResGuardConfig.addWhiteList(rule);
                }
            }
        }

        // file filter
        aabResGuardConfig.setFileFilter(new FileFilterXmlParser(configPath).parse());

        // string filter
        aabResGuardConfig.setStringFilterConfig(new StringFilterXmlParser(configPath).parse());

        // class obfuscation
        aabResGuardConfig.setClassObfuscationConfig(new ClassObfuscationXmlParser(configPath).parse());

        return aabResGuardConfig;
    }
}
