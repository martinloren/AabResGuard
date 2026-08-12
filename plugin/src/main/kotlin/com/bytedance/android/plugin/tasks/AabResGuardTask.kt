package com.bytedance.android.plugin.tasks

import com.bytedance.android.aabresguard.commands.ObfuscateBundleCommand
import com.bytedance.android.aabresguard.model.xml.ClassObfuscationConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 * Modified 2021/08/11
 *
 * Configuration-cache compatible: all data is injected as task inputs at
 * configuration time; no Project or variant objects are touched at execution time.
 */
abstract class AabResGuardTask : DefaultTask() {

    /** The final (signed) bundle produced by AGP. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val bundleFile: RegularFileProperty

    @get:OutputFile
    abstract val obfuscatedBundleFile: RegularFileProperty

    @get:Input
    abstract val enableObfuscate: Property<Boolean>

    @get:Input
    abstract val enableObfuscateAssets: Property<Boolean>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val mappingFile: RegularFileProperty

    @get:Input
    abstract val whiteList: SetProperty<String>

    @get:Input
    abstract val mergeDuplicatedRes: Property<Boolean>

    @get:Input
    abstract val enableFilterFiles: Property<Boolean>

    @get:Input
    abstract val filterList: SetProperty<String>

    @get:Input
    abstract val enableFilterStrings: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val unusedStringPath: Property<String>

    @get:Input
    abstract val languageWhiteList: SetProperty<String>

    @get:Input
    abstract val classObfuscationActive: Property<Boolean>

    @get:Input
    abstract val classObfuscationRules: SetProperty<String>

    @get:Input
    abstract val classObfuscationWhitelist: SetProperty<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val classObfuscationMappingFile: RegularFileProperty

    /** shrinkResources unused.txt location; existence is checked at execution time. */
    @get:Internal
    abstract val shrinkUnusedResourcesFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val storeFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val storePassword: Property<String>

    @get:Input
    @get:Optional
    abstract val keyAlias: Property<String>

    @get:Input
    @get:Optional
    abstract val keyPassword: Property<String>

    init {
        description = "Assemble resource proguard for bundle file"
        group = "bundle"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun execute() {
        printExtensionConfiguration()
        printSignConfiguration()
        printOutputFileLocation()

        val effectiveUnusedStringPath = resolveUnusedStringPath()

        val command = ObfuscateBundleCommand.builder()
                .setEnableObfuscate(enableObfuscate.get())
                .setObfuscateAssets(enableObfuscateAssets.get())
                .setBundlePath(bundleFile.get().asFile.toPath())
                .setOutputPath(obfuscatedBundleFile.get().asFile.toPath())
                .setMergeDuplicatedResources(mergeDuplicatedRes.get())
                .setWhiteList(whiteList.get())
                .setFilterFile(enableFilterFiles.get())
                .setFileFilterRules(filterList.get())
                .setRemoveStr(enableFilterStrings.get())
                .setUnusedStrPath(effectiveUnusedStringPath)
                .setLanguageWhiteList(languageWhiteList.get())
                .setClassObfuscationConfig(buildClassObfuscationConfig())
        if (mappingFile.isPresent) {
            command.setMappingPath(mappingFile.get().asFile.toPath())
        }

        if (classObfuscationMappingFile.isPresent) {
            command.setClassMappingPath(classObfuscationMappingFile.get().asFile.toPath())
        }

        val signingStoreFile = if (storeFile.isPresent) storeFile.get().asFile else null
        if (signingStoreFile != null && signingStoreFile.exists()) {
            command.setStoreFile(signingStoreFile.toPath())
                    .setKeyAlias(keyAlias.orNull)
                    .setKeyPassword(keyPassword.orNull)
                    .setStorePassword(storePassword.orNull)
        }
        command.build().execute()
    }

    private fun resolveUnusedStringPath(): String? {
        val unusedFile = if (shrinkUnusedResourcesFile.isPresent) shrinkUnusedResourcesFile.get().asFile else null
        if (unusedFile != null && unusedFile.exists()) {
            println("find unused.txt : ${unusedFile.absolutePath}")
            if (enableFilterStrings.get() && unusedStringPath.orNull.isNullOrBlank()) {
                println("replace unused.txt!")
                return unusedFile.absolutePath
            }
        } else {
            println("not exists unused.txt : ${unusedFile?.absolutePath}\n" +
                    "use default path : ${unusedStringPath.orNull}")
        }
        return unusedStringPath.orNull
    }

    private fun buildClassObfuscationConfig(): ClassObfuscationConfig {
        val config = ClassObfuscationConfig()
        config.isActive = classObfuscationActive.get()
        classObfuscationRules.get().forEach { config.addRule(it) }
        classObfuscationWhitelist.get().forEach { config.addWhitelist(it) }
        return config
    }

    private fun printExtensionConfiguration() {
        println("-------------- AabResGuardExtension --------------")
        println("\tenableObfuscate=${enableObfuscate.get()}")
        println("\tenableObfuscateAssets=${enableObfuscateAssets.get()}")
        println("\tmappingFile=${if (mappingFile.isPresent) mappingFile.get().asFile.toPath() else null}")
        println("\twhiteList=${whiteList.get()}")
        println("\tobfuscatedBundleFileName=${obfuscatedBundleFile.get().asFile.name}")
        println("\tmergeDuplicatedRes=${mergeDuplicatedRes.get()}")
        println("\tenableFilterFiles=${enableFilterFiles.get()}")
        println("\tfilterList=${filterList.get()}")
        println("\tenableFilterStrings=${enableFilterStrings.get()}")
        println("\tunusedStringPath=${unusedStringPath.orNull}")
        println("\tlanguageWhiteList=${languageWhiteList.get()}")
        println("\tclassObfuscationActive=${classObfuscationActive.orNull}")
        println("\tclassObfuscationRules=${classObfuscationRules.orNull}")
        println("\tclassObfuscationWhitelist=${classObfuscationWhitelist.orNull}")
        println("\tclassObfuscationMappingFile=${if (classObfuscationMappingFile.isPresent) classObfuscationMappingFile.get().asFile.toPath() else null}")
    }

    private fun printSignConfiguration() {
        println("-------------- Sign configuration --------------")
        println("\tStoreFile:\t\t${if (storeFile.isPresent) storeFile.get().asFile else "/"}")
        println("\tKeyPassword:\t${mask(keyPassword.orNull)}")
        println("\tAlias:\t\t\t${mask(keyAlias.orNull)}")
        println("\tStorePassword:\t${mask(storePassword.orNull)}")
    }

    private fun printOutputFileLocation() {
        val obfuscatedBundle = obfuscatedBundleFile.get().asFile
        println("-------------- Output configuration --------------")
        println("\tFolder:\t\t${obfuscatedBundle.parent}")
        println("\tFile:\t\t${obfuscatedBundle.name}")
        println("--------------------------------------------------")
    }

    /** Never log any substring of a password. */
    private fun mask(value: String?): String {
        return if (value == null) "/" else "****"
    }
}
