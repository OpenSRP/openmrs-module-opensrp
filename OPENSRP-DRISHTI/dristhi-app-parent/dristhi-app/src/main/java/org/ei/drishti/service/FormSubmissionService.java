package org.ei.drishti.service;

import static java.text.MessageFormat.format;
import static org.ei.drishti.AllConstants.ENTITY_ID_PARAM;
import static org.ei.drishti.AllConstants.FORM_NAME_PARAM;
import static org.ei.drishti.AllConstants.INSTANCE_ID_PARAM;
import static org.ei.drishti.AllConstants.SYNC_STATUS;
import static org.ei.drishti.AllConstants.VERSION_PARAM;
import static org.ei.drishti.domain.SyncStatus.SYNCED;
import static org.ei.drishti.util.EasyMap.create;
import static org.ei.drishti.util.Log.logError;

import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ei.drishti.domain.form.FormSubmission;
import org.ei.drishti.repository.AllSettings;
import org.ei.drishti.repository.FormDataRepository;

import com.google.gson.Gson;

public class FormSubmissionService {
    private ZiggyService ziggyService;
    private FormDataRepository formDataRepository;
    private AllSettings allSettings;

    public FormSubmissionService(ZiggyService ziggyService, FormDataRepository formDataRepository, AllSettings allSettings) {
        this.ziggyService = ziggyService;
        this.formDataRepository = formDataRepository;
        this.allSettings = allSettings;
    }

    public void processSubmissions(List<FormSubmission> formSubmissions) {
        for (FormSubmission submission : formSubmissions) {
            if (!formDataRepository.submissionExists(submission.instanceId())) {
                try {
                    ziggyService.saveForm(getParams(submission), submission.instance());
                } catch (Exception e) {
                    logError(format("Form submission processing failed, with instanceId: {0}. Exception: {1}, StackTrace: {2}",
                            submission.instanceId(), e.getMessage(), ExceptionUtils.getStackTrace(e)));
                }
            }
            formDataRepository.updateServerVersion(submission.instanceId(), submission.serverVersion());
            allSettings.savePreviousFormSyncIndex(submission.serverVersion());
        }
    }

    private String getParams(FormSubmission submission) {
        return new Gson().toJson(
                create(INSTANCE_ID_PARAM, submission.instanceId())
                        .put(ENTITY_ID_PARAM, submission.entityId())
                        .put(FORM_NAME_PARAM, submission.formName())
                        .put(VERSION_PARAM, submission.version())
                        .put(SYNC_STATUS, SYNCED.value())
                        .map());
    }
}
