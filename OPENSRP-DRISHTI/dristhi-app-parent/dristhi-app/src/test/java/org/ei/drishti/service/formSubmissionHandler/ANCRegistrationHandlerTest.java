package org.ei.drishti.service.formSubmissionHandler;

import static org.ei.drishti.util.FormSubmissionBuilder.create;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.ei.drishti.domain.form.FormSubmission;
import org.ei.drishti.service.MotherService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ANCRegistrationHandlerTest {
    @Mock
    private MotherService motherService;

    private ANCRegistrationHandler handler;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        handler = new ANCRegistrationHandler(motherService);
    }

    @Test
    public void shouldDelegateFormSubmissionHandlingToMotherService() throws Exception {
        FormSubmission submission = create().withFormName("anc_registration").withInstanceId("instance id 1").withVersion("122").build();

        handler.handle(submission);

        verify(motherService).registerANC(submission);
    }
}
