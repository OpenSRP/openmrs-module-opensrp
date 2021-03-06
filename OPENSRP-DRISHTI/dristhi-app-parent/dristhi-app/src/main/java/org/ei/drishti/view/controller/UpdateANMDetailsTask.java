package org.ei.drishti.view.controller;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static org.ei.drishti.util.Log.logWarn;

import java.util.concurrent.locks.ReentrantLock;

import android.os.AsyncTask;

public class UpdateANMDetailsTask {
    private final ANMController anmController;
    private static final ReentrantLock lock = new ReentrantLock();

    public UpdateANMDetailsTask(ANMController anmController) {
        this.anmController = anmController;
    }

    public void fetch(final AfterANMDetailsFetchListener afterFetchListener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                if (!lock.tryLock()) {
                    logWarn("Update ANM details is in progress, so going away.");
                    cancel(true);
                    return null;
                }
                try {
                    return anmController.get();
                } finally {
                    lock.unlock();
                }
            }

            @Override
            protected void onPostExecute(String anm) {
                afterFetchListener.afterFetch(anm);
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }
}
