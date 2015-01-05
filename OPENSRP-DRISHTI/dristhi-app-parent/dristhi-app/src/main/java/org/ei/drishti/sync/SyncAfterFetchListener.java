package org.ei.drishti.sync;

import static org.ei.drishti.event.Event.ON_DATA_FETCHED;

import org.ei.drishti.domain.FetchStatus;

public class SyncAfterFetchListener implements AfterFetchListener {
    public void afterFetch(FetchStatus status) {
        ON_DATA_FETCHED.notifyListeners(status);
    }
}
