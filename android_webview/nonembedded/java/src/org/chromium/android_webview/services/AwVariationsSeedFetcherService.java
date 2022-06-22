package org.chromium.android_webview.services;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import org.chromium.android_webview.common.AwSwitches;
import org.chromium.android_webview.common.variations.VariationsServiceMetricsHelper;
import org.chromium.android_webview.common.variations.VariationsUtils;
import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.components.background_task_scheduler.TaskIds;
import org.chromium.components.minidump_uploader.util.NetworkPermissionUtil;
import org.chromium.components.variations.firstrun.VariationsSeedFetcher;
import org.chromium.components.version_info.VersionConstants;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class AwVariationsSeedFetcherService extends JobIntentService {
    private static final String TAG = "AwVariationsSeedFet-";
    private static final int JOB_ID = TaskIds.WEBVIEW_VARIATIONS_SEED_FETCH_JOB_ID;
    private static final long MIN_JOB_PERIOD_MILLIS = TimeUnit.DAYS.toMillis(1);

    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private static boolean isNetworkAvailableForCrashUploads(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return NetworkPermissionUtil.isNetworkUnmetered(connectivityManager);
    }

    public static void scheduleIfNeeded() {
        // Check how long it's been since FetchTask last ran.
        long lastRequestTime = VariationsUtils.getStampTime();
        if (lastRequestTime != 0) {
            long now = currentTimeMillis();
            long minJobPeriodMillis = VariationsUtils.getDurationSwitchValueInMillis(
                    AwSwitches.FINCH_SEED_MIN_DOWNLOAD_PERIOD, MIN_JOB_PERIOD_MILLIS);
            if (now < lastRequestTime + minJobPeriodMillis) {
                VariationsUtils.debugLog("Throttling seed download job");
                return;
            }
        }

        VariationsUtils.debugLog("Scheduling seed download job");
        Context context = ContextUtils.getApplicationContext();
        ComponentName thisComponent = new ComponentName(context, AwVariationsSeedFetcherService.class);
        if (isNetworkAvailableForCrashUploads(context)) {
            enqueueWork(context, thisComponent, JOB_ID, new Intent());
            VariationsServiceMetricsHelper metrics =
                    VariationsServiceMetricsHelper.fromVariationsSharedPreferences(context);
            metrics.setLastEnqueueTime(currentTimeMillis());
            if (!metrics.writeMetricsToVariationsSharedPreferences(context)) {
                Log.e(TAG, "Failed to write variations SharedPreferences to disk");
            }
        } else {
            Log.e(TAG, "Failed to schedule job");
        }
    }

    private void fetch(Runnable onFinished) {
        // Should we call jobFinished at the end of this task?
        boolean shouldFinish = true;
        long startTime = currentTimeMillis();

        try {
            VariationsUtils.updateStampTime();

            VariationsUtils.debugLog("Downloading new seed");
            VariationsSeedFetcher downloader = VariationsSeedFetcher.get();
            String milestone = String.valueOf(VersionConstants.PRODUCT_MAJOR_VERSION);
            VariationsSeedFetcher.SeedFetchInfo fetchInfo = downloader.downloadContent(
                    VariationsSeedFetcher.VariationsPlatform.ANDROID_WEBVIEW,
                    /*restrictMode=*/null, milestone, null);

            saveMetrics(fetchInfo.seedFetchResult, startTime, /*endTime=*/currentTimeMillis());

            if (fetchInfo.seedInfo != null) {
                VariationsSeedHolder.getInstance().updateSeed(
                        fetchInfo.seedInfo, onFinished);
                shouldFinish = false; // jobFinished will be deferred until updateSeed is done.
            }
        } finally {
            if (shouldFinish) onFinished.run();
        }
    }

    private void saveMetrics(int seedFetchResult, long startTime, long endTime) {
        Context context = ContextUtils.getApplicationContext();
        VariationsServiceMetricsHelper metrics =
                VariationsServiceMetricsHelper.fromVariationsSharedPreferences(context);
        metrics.setSeedFetchResult(seedFetchResult);
        metrics.setSeedFetchTime(endTime - startTime);
        if (metrics.hasLastEnqueueTime()) {
            metrics.setJobQueueTime(startTime - metrics.getLastEnqueueTime());
        }
        if (metrics.hasLastJobStartTime()) {
            metrics.setJobInterval(startTime - metrics.getLastJobStartTime());
        }
        metrics.clearLastEnqueueTime();
        metrics.setLastJobStartTime(startTime);
        if (!metrics.writeMetricsToVariationsSharedPreferences(context)) {
            Log.e(TAG, "Failed to write variations SharedPreferences to disk");
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final BlockingQueue<Object> done = new ArrayBlockingQueue<>(1);
        fetch(() -> done.offer(new Object()));
        try {
            done.take();
        } catch (InterruptedException ignore) {
        }
    }
}
