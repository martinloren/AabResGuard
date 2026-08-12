package com.bytedance.android.aabresguard.parser;

import com.bytedance.android.aabresguard.BaseTest;
import com.bytedance.android.aabresguard.model.xml.ClassObfuscationConfig;

import org.dom4j.DocumentException;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link ClassObfuscationXmlParser}.
 */
public class ClassObfuscationXmlParserTest extends BaseTest {

    @Test
    public void test_parseActive() throws DocumentException {
        ClassObfuscationXmlParser parser = new ClassObfuscationXmlParser(
                loadResourceFile("demo/config-class-obfuscation.xml").toPath());
        ClassObfuscationConfig config = parser.parse();
        assertThat(config.isActive()).isTrue();
        assertThat(config.getRules()).containsExactly("com.ss.android.ugc.model.*");
        assertThat(config.getWhitelist()).containsExactly("*.R*");
    }

    @Test
    public void test_parseInactive() throws DocumentException {
        ClassObfuscationXmlParser parser = new ClassObfuscationXmlParser(
                loadResourceFile("demo/config.xml").toPath());
        ClassObfuscationConfig config = parser.parse();
        assertThat(config.isActive()).isFalse();
        assertThat(config.getRules()).isEmpty();
        assertThat(config.getWhitelist()).isEmpty();
    }
}
