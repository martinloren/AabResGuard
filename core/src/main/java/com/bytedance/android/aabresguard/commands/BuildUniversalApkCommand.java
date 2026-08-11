package com.bytedance.android.aabresguard.commands;

import com.android.tools.build.bundletool.commands.BuildApksCommand;
import com.android.tools.build.bundletool.device.AdbServer;
import com.android.tools.build.bundletool.device.Device;
import com.android.tools.build.bundletool.flags.FlagParser;
import com.google.common.collect.ImmutableList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Builds a universal APK from an Android App Bundle using bundletool.
 * <p>
 * The generated .apks archive contains a single universal.apk entry; this command
 * extracts it and writes it to the requested output path.
 */
public class BuildUniversalApkCommand {

    private final Path bundlePath;
    private final Path outputApkPath;
    private final Path storeFile;
    private final String storePassword;
    private final String keyAlias;
    private final String keyPassword;

    private BuildUniversalApkCommand(Path bundlePath, Path outputApkPath, Path storeFile,
                                       String storePassword, String keyAlias, String keyPassword) {
        this.bundlePath = bundlePath;
        this.outputApkPath = outputApkPath;
        this.storeFile = storeFile;
        this.storePassword = storePassword;
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void execute() throws Exception {
        Path tempApks = Files.createTempFile("universal", ".apks");
        // bundletool creates the output file itself; delete the empty placeholder.
        Files.deleteIfExists(tempApks);
        try {
            System.out.println("Building universal APK set from: " + bundlePath);

            List<String> flagList = new ArrayList<>();
            flagList.add("--bundle=" + bundlePath.toFile().getAbsolutePath());
            flagList.add("--output=" + tempApks.toFile().getAbsolutePath());
            flagList.add("--mode=universal");
            if (storeFile != null && Files.exists(storeFile)) {
                flagList.add("--ks=" + storeFile.toFile().getAbsolutePath());
                if (storePassword != null) {
                    flagList.add("--ks-pass=pass:" + storePassword);
                }
                if (keyAlias != null) {
                    flagList.add("--ks-key-alias=" + keyAlias);
                }
                if (keyPassword != null) {
                    flagList.add("--key-pass=pass:" + keyPassword);
                }
            }

            BuildApksCommand.fromFlags(
                    new FlagParser().parse(flagList.toArray(new String[0])),
                    new NoOpAdbServer()
            ).execute();

            System.out.println("Extracting universal.apk from APK set: " + tempApks);
            try (ZipFile zipFile = new ZipFile(tempApks.toFile())) {
                ZipEntry entry = zipFile.getEntry("universal.apk");
                if (entry == null) {
                    throw new RuntimeException("No universal.apk entry found in generated APK set: " + tempApks);
                }
                Files.createDirectories(outputApkPath.getParent());
                Files.copy(zipFile.getInputStream(entry), outputApkPath, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Universal APK written to: " + outputApkPath);
        } finally {
            Files.deleteIfExists(tempApks);
        }
    }

    public static class Builder {
        private Path bundlePath;
        private Path outputApkPath;
        private Path storeFile;
        private String storePassword;
        private String keyAlias;
        private String keyPassword;

        public Builder setBundlePath(Path bundlePath) {
            this.bundlePath = bundlePath;
            return this;
        }

        public Builder setOutputApkPath(Path outputApkPath) {
            this.outputApkPath = outputApkPath;
            return this;
        }

        public Builder setStoreFile(Path storeFile) {
            this.storeFile = storeFile;
            return this;
        }

        public Builder setStorePassword(String storePassword) {
            this.storePassword = storePassword;
            return this;
        }

        public Builder setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        public Builder setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        public BuildUniversalApkCommand build() {
            if (bundlePath == null) {
                throw new IllegalArgumentException("bundlePath is required");
            }
            if (outputApkPath == null) {
                throw new IllegalArgumentException("outputApkPath is required");
            }
            return new BuildUniversalApkCommand(bundlePath, outputApkPath, storeFile,
                    storePassword, keyAlias, keyPassword);
        }
    }

    /**
     * No-op AdbServer implementation. BuildApksCommand.fromFlags requires an AdbServer
     * even for universal mode, which does not communicate with a device.
     */
    private static class NoOpAdbServer extends AdbServer {

        @Override
        public void init(Path adbPath) {
        }

        @Override
        protected ImmutableList<Device> getDevicesInternal() {
            return ImmutableList.of();
        }

        @Override
        public boolean hasInitialDeviceList() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
