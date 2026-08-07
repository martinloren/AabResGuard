package com.bytedance.android.aabresguard.executors;

import com.android.tools.build.bundletool.model.AppBundle;
import com.android.tools.build.bundletool.model.BundleModule;
import com.bytedance.android.aabresguard.BaseTest;
import com.bytedance.android.aabresguard.bundle.AppBundleAnalyzer;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by YangJing on 2019/10/14 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class ResourcesObfuscatorTest extends BaseTest {

    private static final String ASSET_ENTRY = "assets/ss.properties";

    @Test
    public void test() throws IOException {
        Set<String> whiteList = new HashSet<>(
                ImmutableSet.of(
                        "com.bytedance.android.ugc.aweme.R.raw.*",
                        "*.R.drawable.icon",
                        "*.R.anim.ab*"
                )
        );
        Path bundlePath = loadResourceFile("demo/demo.aab").toPath();
        Path outputDir = getTempDirPath();
        AppBundleAnalyzer analyzer = new AppBundleAnalyzer(bundlePath);
        AppBundle appBundle = analyzer.analyze();
        ResourcesObfuscator obfuscator = new ResourcesObfuscator(bundlePath, appBundle, whiteList, outputDir, loadResourceFile("demo/mapping.txt").toPath());
        AppBundle obfuscateAppBundle = obfuscator.obfuscate();
        assert obfuscateAppBundle != null;
        assert obfuscateAppBundle.getModules().size() == appBundle.getModules().size();
        appBundle.getModules().forEach((bundleModuleName, bundleModule) -> {
            BundleModule obfuscatedModule = obfuscateAppBundle.getModule(bundleModuleName);
            assert obfuscatedModule.getEntries().size() == bundleModule.getEntries().size();
        });
    }

    @Test
    public void test_assetsNotObfuscatedByDefault() throws IOException {
        AppBundle obfuscated = obfuscate(false, new HashSet<>());
        assertThat(assetPaths(obfuscated)).contains(ASSET_ENTRY);
    }

    @Test
    public void test_assetsObfuscated() throws IOException {
        AppBundle obfuscated = obfuscate(true, new HashSet<>());
        Set<String> assetPaths = assetPaths(obfuscated);
        assertThat(assetPaths).doesNotContain(ASSET_ENTRY);
        // the asset was renamed, not removed
        assertThat(assetPaths).isNotEmpty();
    }

    @Test
    public void test_assetsWhitelist() throws IOException {
        AppBundle obfuscated = obfuscate(true, new HashSet<>(ImmutableSet.of("*/assets/*")));
        assertThat(assetPaths(obfuscated)).contains(ASSET_ENTRY);
    }

    private AppBundle obfuscate(boolean obfuscateAssets, Set<String> whiteList) throws IOException {
        Path bundlePath = loadResourceFile("demo/demo.aab").toPath();
        AppBundleAnalyzer analyzer = new AppBundleAnalyzer(bundlePath);
        AppBundle appBundle = analyzer.analyze();
        ResourcesObfuscator obfuscator = new ResourcesObfuscator(
                bundlePath, appBundle, whiteList, getTempDirPath(), null, obfuscateAssets);
        return obfuscator.obfuscate();
    }

    private static Set<String> assetPaths(AppBundle appBundle) {
        Set<String> paths = new HashSet<>();
        appBundle.getModules().values().forEach(module ->
                module.getEntries().stream()
                        .map(entry -> entry.getPath().toString())
                        .filter(path -> path.startsWith(BundleModule.ASSETS_DIRECTORY.toString()))
                        .forEach(paths::add));
        return paths;
    }
}
