package com.bytedance.android.plugin.extensions

import org.gradle.api.Action

import java.nio.file.Path

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
open class AabResGuardExtension {
    var enableObfuscate: Boolean = true
    var enableObfuscateAssets: Boolean = false
    var mappingFile: Path? = null
    var whiteList: Set<String>? = HashSet()
    lateinit var obfuscatedBundleFileName: String
    var mergeDuplicatedRes: Boolean = false
    var enableFilterFiles: Boolean = false
    var filterList: Set<String>? = HashSet()
    var enableFilterStrings: Boolean = false
    var unusedStringPath: String? = ""
    var languageWhiteList: Set<String>? = HashSet()
    var buildUniversalApk: Boolean = true
    var universalApkFileName: String = "universal.apk"
    val classObfuscation: ClassObfuscationExtension = ClassObfuscationExtension()

    fun classObfuscation(action: Action<ClassObfuscationExtension>) {
        action.execute(classObfuscation)
    }

    override fun toString(): String {
        return "-------------- AabResGuardExtension --------------\n" +
                "\tenableObfuscate=$enableObfuscate\n" +
                "\tenableObfuscateAssets=$enableObfuscateAssets\n" +
                "\tmappingFile=$mappingFile\n" +
                "\twhiteList=$whiteList\n" +
                "\tobfuscatedBundleFileName=${if (::obfuscatedBundleFileName.isInitialized) obfuscatedBundleFileName else null}\n" +
                "\tmergeDuplicatedRes=$mergeDuplicatedRes\n" +
                "\tenableFilterFiles=$enableFilterFiles\n" +
                "\tfilterList=$filterList\n" +
                "\tenableFilterStrings=$enableFilterStrings\n" +
                "\tunusedStringPath=$unusedStringPath\n" +
                "\tlanguageWhiteList=$languageWhiteList\n" +
                "\tclassObfuscation=$classObfuscation"
    }
}