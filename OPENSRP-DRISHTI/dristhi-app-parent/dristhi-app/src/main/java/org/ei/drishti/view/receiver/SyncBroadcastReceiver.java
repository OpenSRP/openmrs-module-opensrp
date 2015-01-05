package org.ei.drishti.view.receiver;

import static org.ei.drishti.util.Log.logInfo;

import org.ei.drishti.sync.SyncAfterFetchListener;
import org.ei.drishti.sync.SyncProgressIndicator;
import org.ei.drishti.sync.UpdateActionsTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SyncBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        logInfo("Sync alarm triggered. Trying to Sync.");

        UpdateActionsTask updateActionsTask = new UpdateActionsTask(
                context,
                org.ei.drishti.Context.getInstance().actionService(),
                org.ei.drishti.Context.getInstance().formSubmissionSyncService(), new SyncProgressIndicator());

        updateActionsTask.updateFromServer(new SyncAfterFetchListener());
    }
}

