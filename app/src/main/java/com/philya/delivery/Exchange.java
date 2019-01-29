package com.philya.delivery;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class Exchange {
    public final static int EXCHANGEJOBID = 157;

    public final static long REPEATTIMEOUT = TimeUnit.MINUTES.toMillis(15);

    private static boolean running = false;

    public static synchronized void setRunning(boolean running) {
        Exchange.running = running;
    }

    public static synchronized boolean canStart() {
        if(running) { return false; }

        running = true;
        return true;
    }

    public static synchronized boolean isRunning() {
        return running;
    }

    public static void startExchangeJob(Context context, long timeout) {
        ComponentName name = new ComponentName(context, ExchangeJobService.class);

        JobInfo exchangeJob = new JobInfo.Builder(EXCHANGEJOBID, name)
                .setMinimumLatency(timeout)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = jobScheduler.schedule(exchangeJob);

        Log.d("delivery.exchange", "Результат запуска обмена с офисом: " +
                ((result == JobScheduler.RESULT_SUCCESS) ? "успешно" : "ошибка") + ". Таймаут: " + timeout / 1000 + " секунд.");
    }
}
