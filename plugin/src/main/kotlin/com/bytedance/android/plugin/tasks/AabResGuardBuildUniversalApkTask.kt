package com.bytedance.android.plugin.tasks

import com.bytedance.android.aabresguard.commands.BuildUniversalApkCommand
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Builds a universal APK from an obfuscated AAB using bundletool.
 */
abstract class AabResGuardBuildUniversalApkTask : DefaultTask() {

    /** The obfuscated AAB produced by AabResGuardTask. */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val bundleFile: RegularFileProperty

    @get:OutputFile
    abstract val universalApkFile: RegularFileProperty

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

    @get:Input
    @get:Optional
    abstract val aapt2Path: Property<String>

    init {
        description = "Build a universal APK from the obfuscated AAB"
        group = "assemble"
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun execute() {
        val bundle = bundleFile.get().asFile.toPath()
        val output = universalApkFile.get().asFile.toPath()
        println("Building universal APK from $bundle -> $output")
        val command = BuildUniversalApkCommand.builder()
                .setBundlePath(bundle)
                .setOutputApkPath(output)
        if (storeFile.isPresent) {
            command.setStoreFile(storeFile.get().asFile.toPath())
        }
        if (storePassword.isPresent) {
            command.setStorePassword(storePassword.get())
        }
        if (keyAlias.isPresent) {
            command.setKeyAlias(keyAlias.get())
        }
        if (keyPassword.isPresent) {
            command.setKeyPassword(keyPassword.get())
        }
        if (aapt2Path.isPresent) {
            command.setAapt2Path(aapt2Path.get())
        }
        command.build().execute()
    }
}
