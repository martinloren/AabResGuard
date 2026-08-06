package com.bytedance.android.aabresguard.bundle;

import com.android.tools.build.bundletool.model.AppBundle;
import com.bytedance.android.aabresguard.utils.TimeClock;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import static com.android.tools.build.bundletool.model.utils.files.FilePreconditions.checkFileExistsAndReadable;

/**
 * Created by YangJing on 2019/10/10 .
 * Email: yangjing.yeoh@bytedance.com
 */
public class AppBundleAnalyzer implements Closeable {

    private static final Logger logger = Logger.getLogger(AppBundleAnalyzer.class.getName());
    private final Path bundlePath;
    private ZipFile bundleZip;

    public AppBundleAnalyzer(Path bundlePath) {
        checkFileExistsAndReadable(bundlePath);
        this.bundlePath = bundlePath;
    }

    public AppBundle analyze() throws IOException {
        TimeClock timeClock = new TimeClock();
        // NOTE: entry contents are read lazily from this zip, it must stay open
        // until the returned AppBundle has been packaged, see close().
        bundleZip = new ZipFile(bundlePath.toFile());
        AppBundle appBundle = AppBundle.buildFromZip(bundleZip);
        System.out.println(String.format("analyze bundle file done, const %s", timeClock.getCost()));
        return appBundle;
    }

    @Override
    public void close() throws IOException {
        if (bundleZip != null) {
            bundleZip.close();
        }
    }
}
