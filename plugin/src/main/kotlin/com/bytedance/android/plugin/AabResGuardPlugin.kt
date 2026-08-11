package com.bytedance.android.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.impl.SigningConfigImpl
import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.bytedance.android.plugin.tasks.AabResGuardBuildUniversalApkTask
import com.bytedance.android.plugin.tasks.AabResGuardTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by YangJing on 2019/10/15 .
 * Email: yangjing.yeoh@bytedance.com
 */
class AabResGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        val aabResGuard = project.extensions.create("aabResGuard", AabResGuardExtension::class.java)
        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            registerAabResGuardTask(project, variant, aabResGuard)
        }
    }

    private fun registerAabResGuardTask(project: Project, variant: ApplicationVariant, aabResGuard: AabResGuardExtension) {
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        // Read eagerly so the provider closure below only captures a plain String
        // (configuration-cache friendly).
        val obfuscatedBundleFileName = aabResGuard.obfuscatedBundleFileName

        val aabResGuardTask = project.tasks.register("aabresguard$variantName", AabResGuardTask::class.java) { task ->
            // SingleArtifact.BUNDLE is the final (signed) bundle produced by the
            // package<Variant>Bundle / sign<Variant>Bundle tasks; wiring it as an input
            // also carries the dependency on those tasks.
            task.bundleFile.set(variant.artifacts.get(SingleArtifact.BUNDLE))
            // Place the obfuscated bundle in the variant's bundle output directory.
            // We intentionally do NOT derive the path from bundleFile, because reading
            // that provider to compute an output location would force it to be resolved
            // before the sign/packaging task that produces it has finished.
            task.obfuscatedBundleFile.set(
                project.layout.buildDirectory.file("outputs/bundle/${variant.name}/$obfuscatedBundleFileName")
            )

            task.enableObfuscate.set(aabResGuard.enableObfuscate)
            task.enableObfuscateAssets.set(aabResGuard.enableObfuscateAssets)
            aabResGuard.mappingFile?.let { task.mappingFile.fileValue(it.toFile()) }
            task.whiteList.set(aabResGuard.whiteList ?: emptySet())
            task.mergeDuplicatedRes.set(aabResGuard.mergeDuplicatedRes)
            task.enableFilterFiles.set(aabResGuard.enableFilterFiles)
            task.filterList.set(aabResGuard.filterList ?: emptySet())
            task.enableFilterStrings.set(aabResGuard.enableFilterStrings)
            aabResGuard.unusedStringPath?.let { task.unusedStringPath.set(it) }
            task.languageWhiteList.set(aabResGuard.languageWhiteList ?: emptySet())

            // shrinkResources unused.txt location for this variant (may not exist).
            val simpleName = variant.name.replace("Release", "")
            val unusedResName = simpleName.replaceFirstChar { it.lowercase() }
            task.shrinkUnusedResourcesFile.set(
                    project.layout.buildDirectory.file("outputs/mapping/$unusedResName/release/unused.txt"))

            // Pass the signing config as lazy task inputs.
            (variant.signingConfig as? SigningConfigImpl)?.let { signingConfig ->
                task.storeFile.fileProvider(signingConfig.storeFile)
                task.storePassword.set(signingConfig.storePassword)
                task.keyAlias.set(signingConfig.keyAlias)
                task.keyPassword.set(signingConfig.keyPassword)
            }
        }

        // The bundle<Variant> lifecycle task depends on the obfuscation task.
        project.tasks.matching { it.name == "bundle$variantName" }.configureEach {
            it.dependsOn(aabResGuardTask)
        }

        // Optionally build a universal APK from the obfuscated AAB and wire it into
        // assemble<Variant> so that APK builds also produce an obfuscated universal APK.
        if (aabResGuard.buildUniversalApk) {
            val universalApkTask = project.tasks.register("aabresguardBuildUniversalApk$variantName", AabResGuardBuildUniversalApkTask::class.java) { task ->
                task.bundleFile.set(aabResGuardTask.flatMap { it.obfuscatedBundleFile })
                task.universalApkFile.set(
                    project.layout.buildDirectory.file("outputs/apk/${variant.name}/${aabResGuard.universalApkFileName}")
                )
                // Re-use the same signing configuration so the universal APK is signed
                // with the release key, not bundletool's default debug keystore.
                (variant.signingConfig as? SigningConfigImpl)?.let { signingConfig ->
                    task.storeFile.fileProvider(signingConfig.storeFile)
                    task.storePassword.set(signingConfig.storePassword)
                    task.keyAlias.set(signingConfig.keyAlias)
                    task.keyPassword.set(signingConfig.keyPassword)
                }
            }
            project.tasks.matching { it.name == "assemble$variantName" }.configureEach {
                it.dependsOn(universalApkTask)
            }
        }
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }
}
