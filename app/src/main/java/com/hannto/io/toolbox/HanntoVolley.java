package com.hannto.io.toolbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.hannto.io.HanntoNetwork;
import com.hannto.io.HanntoRequestQueue;

import java.io.File;

public class HanntoVolley {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * Creates a default instance of the worker pool and calls {@link HanntoRequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack A {@link HanntoStack} to use for the network, or null for default.
     * @return A started {@link HanntoRequestQueue} instance.
     */
    public static HanntoRequestQueue newRequestQueue(Context context, HanntoStack stack) {
        HanntoBasicNetwork network;
        if (stack == null) {

            network = new HanntoBasicNetwork(new HanntoBasicStack());

//            if (Build.VERSION.SDK_INT >= 9) {
//                network = new BasicNetwork(new HurlStack());
//            } else {
//                // Prior to Gingerbread, HttpUrlConnection was unreliable.
//                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
//                // At some point in the future we'll move our minSdkVersion past Froyo and can
//                // delete this fallback (along with all Apache HTTP code).
//                String userAgent = "volley/0";
//                try {
//                    String packageName = context.getPackageName();
//                    PackageInfo info =
//                            context.getPackageManager().getPackageInfo(packageName, /* flags= */ 0);
//                    userAgent = packageName + "/" + info.versionCode;
//                } catch (NameNotFoundException e) {
//                }
//
//                network =
//                        new BasicNetwork(
//                                new HttpClientStack(AndroidHttpClient.newInstance(userAgent)));
//            }
        } else {
            network = new HanntoBasicNetwork(stack);
        }

        return newRequestQueue(context, network);
    }

//    /**
//     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
//     *
//     * @param context A {@link Context} to use for creating the cache dir.
//     * @param stack An {@link HttpStack} to use for the network, or null for default.
//     * @return A started {@link RequestQueue} instance.
//     * @deprecated Use {@link #newRequestQueue(Context, BaseHttpStack)} instead to avoid depending
//     *     on Apache HTTP. This method may be removed in a future release of Volley.
//     */
//    @Deprecated
//    @SuppressWarnings("deprecation")
//    public static HanntoRequestQueue newRequestQueue(Context context, HttpStack stack) {
//        if (stack == null) {
//            return newRequestQueue(context, (BaseHttpStack) null);
//        }
//        return newRequestQueue(context, new BasicNetwork(stack));
//    }

    private static HanntoRequestQueue newRequestQueue(Context context, HanntoNetwork network) {
//        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        HanntoRequestQueue queue = new HanntoRequestQueue(network);
        queue.start();
        return queue;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link HanntoRequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link HanntoRequestQueue} instance.
     */
    public static HanntoRequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, (HanntoStack) null);
    }
}
