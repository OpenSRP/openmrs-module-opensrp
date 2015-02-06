package org.ei.drishti.view.activity;

import static java.lang.String.valueOf;
import static org.ei.drishti.event.Event.ACTION_HANDLED;
import static org.ei.drishti.event.Event.FORM_SUBMITTED;
import static org.ei.drishti.event.Event.SYNC_COMPLETED;
import static org.ei.drishti.event.Event.SYNC_STARTED;

import org.ei.drishti.Context;
import org.ei.drishti.crvs.pk.R;
import org.ei.drishti.event.Listener;
import org.ei.drishti.service.PendingFormSubmissionService;
import org.ei.drishti.sync.SyncAfterFetchListener;
import org.ei.drishti.sync.SyncProgressIndicator;
import org.ei.drishti.sync.UpdateActionsTask;
import org.ei.drishti.view.contract.ANMLocation;
import org.ei.drishti.view.contract.HomeContext;
import org.ei.drishti.view.controller.NativeAfterANMDetailsFetchListener;
import org.ei.drishti.view.controller.NativeUpdateANMDetailsTask;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.Gson;

public class CRVSHomeActivity extends SecuredActivity {
    private MenuItem updateMenuItem;
    private MenuItem remainingFormsToSyncMenuItem;
    private PendingFormSubmissionService pendingFormSubmissionService;

    private Listener<Boolean> onSyncStartListener = new Listener<Boolean>() {
        @Override
        public void onEvent(Boolean data) {
            if (updateMenuItem != null) {
                updateMenuItem.setActionView(R.layout.progress);
            }
        }
    };

    private Listener<Boolean> onSyncCompleteListener = new Listener<Boolean>() {
        @Override
        public void onEvent(Boolean data) {
            //#TODO: RemainingFormsToSyncCount cannot be updated from a back ground thread!!
            updateRemainingFormsToSyncCount();
            if (updateMenuItem != null) {
                updateMenuItem.setActionView(null);
            }
            updateRegisterCounts();
        }
    };

    private Listener<String> onFormSubmittedListener = new Listener<String>() {
        @Override
        public void onEvent(String instanceId) {
            updateRegisterCounts();
        }
    };

    private Listener<String> updateANMDetailsListener = new Listener<String>() {
        @Override
        public void onEvent(String data) {
            updateRegisterCounts();
        }
    };

    @Override
    protected void onCreation() {
        setContentView(R.layout.crvs_home);
        setupViews();
        initialize();
    }

    private void setupViews() {
        findViewById(R.id.btn_crvs_birth_notification).setOnClickListener(onRegisterStartListener);
        findViewById(R.id.btn_crvs_death_notification).setOnClickListener(onRegisterStartListener);
        findViewById(R.id.btn_crvs_pregnancy_notification).setOnClickListener(onRegisterStartListener);
        findViewById(R.id.btn_crvs_verbal_autopsy).setOnClickListener(onRegisterStartListener);

    }

    private void initialize() {
        pendingFormSubmissionService = context.pendingFormSubmissionService();
        SYNC_STARTED.addListener(onSyncStartListener);
        SYNC_COMPLETED.addListener(onSyncCompleteListener);
        FORM_SUBMITTED.addListener(onFormSubmittedListener);
        ACTION_HANDLED.addListener(updateANMDetailsListener);
    }

    @Override
    protected void onResumption() {
        updateRegisterCounts();
        updateSyncIndicator();
        updateRemainingFormsToSyncCount();
    }

    private void updateRegisterCounts() {
        NativeUpdateANMDetailsTask task = new NativeUpdateANMDetailsTask(Context.getInstance().anmController());
        task.fetch(new NativeAfterANMDetailsFetchListener() {
            @Override
            public void afterFetch(HomeContext anmDetails) {
                updateRegisterCounts(anmDetails);
            }
        });
    }

    private void updateRegisterCounts(HomeContext homeContext) {}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateMenuItem = menu.findItem(R.id.updateMenuItem);
        remainingFormsToSyncMenuItem = menu.findItem(R.id.remainingFormsToSyncMenuItem);

        updateSyncIndicator();
        updateRemainingFormsToSyncCount();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.updateMenuItem:
                updateFromServer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void updateFromServer() {
        UpdateActionsTask updateActionsTask = new UpdateActionsTask(
                this, context.actionService(), context.formSubmissionSyncService(), new SyncProgressIndicator());
        updateActionsTask.updateFromServer(new SyncAfterFetchListener());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SYNC_STARTED.removeListener(onSyncStartListener);
        SYNC_COMPLETED.removeListener(onSyncCompleteListener);
        FORM_SUBMITTED.removeListener(onFormSubmittedListener);
        ACTION_HANDLED.removeListener(updateANMDetailsListener);
    }

    private void updateSyncIndicator() {
        if (updateMenuItem != null) {
            if (context.allSharedPreferences().fetchIsSyncInProgress()) {
                updateMenuItem.setActionView(R.layout.progress);
            } else
                updateMenuItem.setActionView(null);
        }
    }

    private void updateRemainingFormsToSyncCount() {
        if (remainingFormsToSyncMenuItem == null) {
            return;
        }

        long size = pendingFormSubmissionService.pendingFormSubmissionCount();
        if (size > 0) {
            remainingFormsToSyncMenuItem.setTitle(valueOf(size) + " " + getString(R.string.unsynced_forms_count_message));
            remainingFormsToSyncMenuItem.setVisible(true);
        } else {
            remainingFormsToSyncMenuItem.setVisible(false);
        }
    }

    private View.OnClickListener onRegisterStartListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
        	String locs = context.allSettings().fetchANMLocation();
            ANMLocation anmLocation = new Gson().fromJson(locs, ANMLocation.class);
            
//            String anmloc = "{\"fieldOverrides\":\"{\\\"address_encounter_district\\\":\\\""+anmLocation.getDistrict()+"\\\",\\\"address_encounter_town\\\":\\\""+anmLocation.getSubCenter()+"\\\",\\\"address_encounter_province\\\":\\\"kpk\\\"}\"}";
            String anmloc = "{\"fieldOverrides\":\"{\\\"address_encounter_district\\\":\\\""+anmLocation.getDistrict()+"\\\",\\\"address_encounter_town\\\":\\\""+anmLocation.getSubCenter()+"\\\",\\\"address_encounter_province\\\":\\\"kpk\\\",\\\"phc_identifier\\\":\\\""+anmLocation.getPhcIdentifier()+"\\\",\\\"phc_name\\\":\\\""+anmLocation.getPhcName()+"\\\"}\"}";

            System.out.println("LOCCCCCCCCCCCCCCCCCS:::::::::"+locs);
            switch (view.getId()) {
                case R.id.btn_crvs_birth_notification:
                	//anmloc = "{\"fieldOverrides\":\"{\\\"tpo1\\\":\\\"tone\\\",\\\"tpo2\\\":\\\"ttwo\\\",\\\"tpo3\\\":\\\"tthree\\\"}\"}";
                    formController.startFormActivity("crvs_birth_notification", null, anmloc);
                    break;

                case R.id.btn_crvs_death_notification:
                	formController.startFormActivity("crvs_death_notification", null, anmloc);
                    break;

                case R.id.btn_crvs_pregnancy_notification:
                	formController.startFormActivity("crvs_pregnancy_notification", null, anmloc);
                    break;

                case R.id.btn_crvs_verbal_autopsy:
                	formController.startFormActivity("crvs_verbal_autopsy", null, anmloc);
                    break;
            }
        }
    };
}
