package org.ei.drishti.view.controller;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static org.ei.drishti.util.Log.logWarn;

import java.util.concurrent.locks.ReentrantLock;

import org.ei.drishti.view.contract.HomeContext;

import android.os.AsyncTask;

public class NativeUpdateANMDetailsTask {
    private final ANMController anmController;
    private static final ReentrantLock lock = new ReentrantLock();

    public NativeUpdateANMDetailsTask(ANMController anmController) {
        this.anmController = anmController;
    }

    public void fetch(final NativeAfterANMDetailsFetchListener afterFetchListener) {
        new AsyncTask<Void, Void, HomeContext>() {
            @Override
            protected HomeContext doInBackground(Void... params) {
                if (!lock.tryLock()) {
                    logWarn("Update ANM details is in progress, so going away.");
                    cancel(true);
                    return null;
                }
                try {
                    return anmController.getHomeContext();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            protected void onPostExecute(HomeContext anm) {
                afterFetchListener.afterFetch(anm);
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }
}
