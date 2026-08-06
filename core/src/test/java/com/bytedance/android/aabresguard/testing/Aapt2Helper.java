/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.bytedance.android.aabresguard.testing;

import com.android.tools.build.bundletool.androidtools.Aapt2Command;
import com.android.tools.build.bundletool.androidtools.CommandExecutor;
import com.android.tools.build.bundletool.androidtools.DefaultCommandExecutor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ObjectArrays;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

/** Helper for tests using aapt2. */
public final class Aapt2Helper {

  public static final String AAPT2_PATH = findAapt2Path();

  public static Aapt2Command getAapt2Command() {
    return Aapt2Command.createFromExecutablePath(Paths.get(AAPT2_PATH));
  }

  public static void convertBinaryApkToProtoApk(Path binaryApk, Path protoApk) {
    runAapt2(
        "convert", "--output-format", "proto", "-o", protoApk.toString(), binaryApk.toString());
  }

  private static void runAapt2(String... command) {
    new DefaultCommandExecutor()
        .execute(
            ImmutableList.copyOf(ObjectArrays.concat(AAPT2_PATH, command)),
            CommandExecutor.CommandOptions.builder().setTimeout(Duration.ofMinutes(1)).build());
  }

  private static String findAapt2Path() {
    String path = System.getenv("AAPT2_PATH");
    if (path != null) {
      return path;
    }
    // fall back to the newest aapt2 shipped with the Android SDK build-tools
    String aapt2Name = isWindows() ? "aapt2.exe" : "aapt2";
    File sdkDir = findAndroidSdkDir();
    if (sdkDir != null) {
      File[] buildTools = new File(sdkDir, "build-tools").listFiles();
      if (buildTools != null) {
        Arrays.sort(buildTools, Comparator.comparing(File::getName).reversed());
        for (File version : buildTools) {
          File aapt2 = new File(version, aapt2Name);
          if (aapt2.isFile()) {
            return aapt2.getAbsolutePath();
          }
        }
      }
    }
    return null;
  }

  private static File findAndroidSdkDir() {
    for (String envName : new String[] {"ANDROID_HOME", "ANDROID_SDK_ROOT"}) {
      String sdkDir = System.getenv(envName);
      if (sdkDir != null && new File(sdkDir).isDirectory()) {
        return new File(sdkDir);
      }
    }
    // local.properties may live in the module dir or the repo root
    for (String candidate : new String[] {"local.properties", "../local.properties"}) {
      File localProperties = new File(candidate);
      if (localProperties.isFile()) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(localProperties)) {
          properties.load(input);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        String sdkDir = properties.getProperty("sdk.dir");
        if (sdkDir != null && new File(sdkDir).isDirectory()) {
          return new File(sdkDir);
        }
      }
    }
    return null;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }

  private Aapt2Helper() {}
}
