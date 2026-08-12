package com.bytedance.android.plugin.extensions

/**
 * Configuration for the optional class-name obfuscation of custom views.
 *
 * When active, every fully-qualified class name that matches one of the [rules]
 * and does not match any entry in [whitelist] is renamed to a short generated
 * name both in the compiled DEX files and in binary XML layouts.
 */
open class ClassObfuscationExtension {
    var isActive: Boolean = false
    var rules: Set<String>? = HashSet()
    var whitelist: Set<String>? = HashSet()
    var mappingFile: java.nio.file.Path? = null
}
