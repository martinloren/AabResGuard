package com.bytedance.android.aabresguard.model.xml;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for class-name obfuscation of custom views in compiled DEX and XML.
 */
public class ClassObfuscationConfig {
    private boolean active;
    private final Set<String> rules = new HashSet<>();
    private final Set<String> whitelist = new HashSet<>();

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<String> getRules() {
        return rules;
    }

    public void addRule(String rule) {
        if (rule != null && !rule.isEmpty()) {
            this.rules.add(rule);
        }
    }

    public Set<String> getWhitelist() {
        return whitelist;
    }

    public void addWhitelist(String whitelistRule) {
        if (whitelistRule != null && !whitelistRule.isEmpty()) {
            this.whitelist.add(whitelistRule);
        }
    }
}
