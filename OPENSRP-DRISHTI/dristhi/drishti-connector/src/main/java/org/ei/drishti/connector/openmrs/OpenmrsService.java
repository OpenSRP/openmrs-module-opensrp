package org.ei.drishti.connector.openmrs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ei.drishti.common.util.HttpAgent;
import org.ei.drishti.common.util.HttpResponse;
import org.ei.drishti.connector.constants.OpenmrsConstants.FormField;
import org.ei.drishti.connector.constants.OpenmrsConstants.Location;
import org.ei.drishti.connector.constants.OpenmrsConstants.PersonField;
import org.ei.drishti.domain.OLocation;
import org.ei.drishti.form.domain.FormSubmission;
import org.ei.drishti.form.service.FormSubmissionService;
import org.ei.drishti.service.LocationService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.motechproject.scheduler.MotechSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mysql.jdbc.StringUtils;

@Service
public class OpenmrsService {
    private static final String SUBJECT = "OPENMRS_PUSHER_SCHEDULE";
	private static Logger logger = LoggerFactory.getLogger(OpenmrsService.class.toString());
    private static String openmrsOpenmrsUrl;
    private static String openmrsUsername;
    private static String openmrsPassword;
    private static String openmrsOpenSrpConnectorUrl;
    private static String openmrsOpenSrpConnectorContentParam;
    private static String openmrsLocationListUrl;

    private HttpAgent httpAgent;
    private FormSubmissionService formSubmissionService;
    private MotechSchedulerService schedulerService;
    private LocationService locationService;

    @Autowired
    public OpenmrsService(MotechSchedulerService schedulerService, FormSubmissionService formSubmissionService, 
    		LocationService locationService,
    		@Value("#{drishti['openmrs.url']}") String openmrsOpenmrsUrl,
    		@Value("#{drishti['openmrs.username']}") String openmrsUsername,
    		@Value("#{drishti['openmrs.password']}") String openmrsPassword,
    		@Value("#{drishti['openmrs.opensrp-connector.url']}") String openmrsOpenSrpConnectorUrl,
    		@Value("#{drishti['openmrs.opensrp-connector.content-param']}") String openmrsOpenSrpConnectorContentParam, 
    		@Value("#{drishti['openmrs.location-list.url']}") String openmrsLocationListUrl,
    		HttpAgent httpAgent) {
        this.openmrsOpenmrsUrl = openmrsOpenmrsUrl;
        this.openmrsUsername = openmrsUsername;
        this.openmrsPassword = openmrsPassword;
        this.openmrsOpenSrpConnectorUrl = openmrsOpenSrpConnectorUrl;
        this.openmrsOpenSrpConnectorContentParam = openmrsOpenSrpConnectorContentParam;
        this.openmrsLocationListUrl = openmrsLocationListUrl;
        this.httpAgent = httpAgent;
        this.formSubmissionService = formSubmissionService;
        this.schedulerService = schedulerService;
        this.locationService = locationService;
    }

    public String pushDataToOpenmrs(String formToPush, long serverVersion) throws JSONException {
			List<FormSubmission> fsl = formSubmissionService.getSubmissionByFormName(formToPush, serverVersion);
			//System.out.println(formToPush+":FORM:"+fsl);
			JSONObject json = new JSONObject();
			json.put("app_ver", "1.4.1");
			json.put("username", "admin");
			
			JSONArray mainObj = new JSONArray();
			
			for (FormSubmission fs : fsl) {
				JSONObject pd = new JSONObject();

				JSONObject patient = new JSONObject();
				patient.put(PersonField.IDENTIFIER.OMR_FIELD(), fs.entityId());//TODO change id type
				String firstname = fs.getField(PersonField.FIRST_NAME.SRP_FIELD());
				String lastname = fs.getField(PersonField.LAST_NAME.SRP_FIELD());
				patient.put(PersonField.FIRST_NAME.OMR_FIELD(), StringUtils.isEmptyOrWhitespaceOnly(firstname)?"NONAME":firstname);
				patient.put(PersonField.LAST_NAME.OMR_FIELD(), StringUtils.isEmptyOrWhitespaceOnly(lastname)?"NONAME":lastname);
				patient.put(PersonField.GENDER.OMR_FIELD(), fs.getField(PersonField.GENDER.SRP_FIELD()));
				patient.put(PersonField.BIRTHDATE.OMR_FIELD(), fs.getField(PersonField.BIRTHDATE.SRP_FIELD()));
				patient.put(PersonField.BIRTHDATE_IS_APPROX.OMR_FIELD(), false);
				patient.put(PersonField.DEATHDATE.OMR_FIELD(), fs.getField(PersonField.DEATHDATE.SRP_FIELD()));
				patient.put(PersonField.DE_USER.OMR_FIELD(), 1);
				
				JSONArray en = new JSONArray();
				
				if(fs.formName().toLowerCase().contains("birth")){
					en.put(createBirthEncounterAndObs(fs));
				}
				else if(fs.formName().toLowerCase().contains("death")){
					patient.put(PersonField.LAST_NAME.OMR_FIELD(), fs.getField(PersonField.LAST_NAME.SRP_FIELD()));

					en.put(createDeathEncounterAndObs(fs));
				}
				else if(fs.formName().toLowerCase().contains("pregnancy")){
					patient.put(PersonField.GENDER.OMR_FIELD(), "FEMALE");
					Calendar bdc = Calendar.getInstance();
					bdc.add(Calendar.YEAR, -Integer.parseInt(fs.getField(PersonField.AGE.SRP_FIELD())));
					patient.put(PersonField.BIRTHDATE.OMR_FIELD(), new SimpleDateFormat("yyyy-MM-dd").format(bdc.getTime()));

					en.put(createPregnancyNotificationEncounterAndObs(fs));
				}

				pd.put("patient", patient);

				pd.put("encounter", en);
				
				JSONObject j = new JSONObject();
				j.put("patientData", pd);
				
				mainObj.put(j);
			}
						
			json.put("MainObj",mainObj);
			
			String appUrl = removeEndingSlash(openmrsOpenmrsUrl);
			String serviceUrl =  removeEndingSlash(openmrsOpenSrpConnectorUrl);
			HttpResponse response = httpAgent.post(appUrl+"/"+serviceUrl, "username="+openmrsUsername+"&password="+openmrsPassword, json.toString(), openmrsOpenSrpConnectorContentParam);
			System.out.println("BODY:::"+response.body());
			return response.body();
	}
    
	private JSONObject createBirthEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.BIRTH_FORM_ID.OMR_FIELD(), FormField.BIRTH_FORM_ID.DEFAULT_VALUE());
		encounter.put(FormField.BIRTH_FORM_TYPE.OMR_FIELD(), FormField.BIRTH_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.LOCATON.OMR_FIELD(), FormField.LOCATON.DEFAULT_VALUE());
		encounter.put(FormField.REGISTRATION_DATE.OMR_FIELD(), fs.getField(FormField.REGISTRATION_DATE.SRP_FIELD())+" 00:00:00");

		int i = 1;
		JSONArray obarr = new JSONArray();
		
		FormField.PERSON_IDENTIFIER.createConceptObs(obarr, fs, i++, null);
		FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, fs.getField(PersonField.FIRST_NAME.SRP_FIELD()), i++, null);
		FormField.BIRTHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.DEATHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.GENDER.createConceptObs(obarr, fs, i++, null);
		
		JSONObject mother = FormField.MOTHER.createParentObs(i++);
		obarr.put(mother);

		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_AGE.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_MARITAL_STATUS.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_EDUCATION.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_OCCUPATION.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_CITIZENSHIP.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE.createConceptObs(obarr, fs, i++, mother);
		
		JSONObject father = FormField.FATHER.createParentObs(i++);
		obarr.put(father);
		
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_AGE.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_MARITAL_STATUS.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_EDUCATION.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_OCCUPATION.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_CITIZENSHIP.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_NIC.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE.createConceptObs(obarr, fs, i++, father);

		FormField.BIRTH_TIME.createConceptObs(obarr, fs, i++, null);
		FormField.BIRTH_WEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.BIRTH_PLACE.createConceptObs(obarr, fs, i++, null);
		FormField.GRAVIDA.createConceptObs(obarr, fs, i++, null);
		FormField.PARITY.createConceptObs(obarr, fs, i++, null);
		FormField.GESTATIONAL_AGE.createConceptObs(obarr, fs, i++, null);
		FormField.BIRTH_TYPE.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_TYPE.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_ASSISTANT.createConceptObs(obarr, fs, i++, null);

		encounter.put("obs", obarr);
		
		return encounter;
	}
	
	private JSONObject createDeathEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.DEATH_FORM_ID.OMR_FIELD(), FormField.DEATH_FORM_ID.DEFAULT_VALUE());
		encounter.put(FormField.DEATH_FORM_TYPE.OMR_FIELD(), FormField.DEATH_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.LOCATON.OMR_FIELD(), FormField.LOCATON.DEFAULT_VALUE());
		encounter.put(FormField.REGISTRATION_DATE.OMR_FIELD(), fs.getField(FormField.REGISTRATION_DATE.SRP_FIELD())+" 00:00:00");

		int i = 1;
		JSONArray obarr = new JSONArray();

		FormField.PERSON_IDENTIFIER.createConceptObs(obarr, fs, i++, null);
		String lname = fs.getField(PersonField.LAST_NAME.SRP_FIELD());
		FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, fs.getField(PersonField.FIRST_NAME.SRP_FIELD())+(StringUtils.isEmptyOrWhitespaceOnly(lname)?"": " "+lname), i++, null);
		FormField.BIRTHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.DEATHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.GENDER.createConceptObs(obarr, fs, i++, null);
		
		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUSAL_RESIDENCE.createConceptObs(obarr, fs, i++, null);

		FormField.CAUSE_OF_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.DEATH_PLACE.createConceptObs(obarr, fs, i++, null);

		FormField.NIC.createConceptObs(obarr, fs, i++, null);
		FormField.EDUCATION.createConceptObs(obarr, fs, i++, null);
		FormField.CITIZENSHIP.createConceptObs(obarr, fs, i++, null);
		FormField.ETHNICITY.createConceptObs(obarr, fs, i++, null);
		FormField.MARITAL_STATUS.createConceptObs(obarr, fs, i++, null);
		
		encounter.put("obs", obarr);
		
		return encounter;
	}
	
	private JSONObject createPregnancyNotificationEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.PREGNANCY_NOTIFICATION_FORM_ID.OMR_FIELD(), FormField.PREGNANCY_NOTIFICATION_FORM_ID.DEFAULT_VALUE());
		encounter.put(FormField.PREGNANCY_NOTIFICATION_FORM_TYPE.OMR_FIELD(), FormField.PREGNANCY_NOTIFICATION_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.LOCATON.OMR_FIELD(), FormField.LOCATON.DEFAULT_VALUE());
		encounter.put(FormField.REGISTRATION_DATE.OMR_FIELD(), fs.getField(FormField.REGISTRATION_DATE.SRP_FIELD())+" 00:00:00");

		int i = 1;
		JSONArray obarr = new JSONArray();

		FormField.PERSON_IDENTIFIER.createConceptObs(obarr, fs, i++, null);
		FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, fs.getField(PersonField.FIRST_NAME.SRP_FIELD()), i++, null);
		FormField.BIRTHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.DEATHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.GENDER.createConceptObs(obarr, fs, i++, null);
		
		FormField.ANC_REGISTRATION_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.ANC_REGISTRATION_LOCATION.createConceptObs(obarr, fs, i++, null);
		
		FormField.AGE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUSAL_RESIDENCE.createConceptObs(obarr, fs, i++, null);
		FormField.MARITAL_STATUS.createConceptObs(obarr, fs, i++, null);
		FormField.NEXT_OF_KIN.createConceptObs(obarr, fs, i++, null);
		FormField.ANC_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.LMP.createConceptObs(obarr, fs, i++, null);
		FormField.EDD.createConceptObs(obarr, fs, i++, null);
		FormField.PLANNED_FACILITY_OF_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.PLANNED_TRANSPORT_FOR_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.ENROLLED_IN_HIV_CARE.createConceptObs(obarr, fs, i++, null);
		FormField.ENROLLED_DATE_HIV_CARE.createConceptObs(obarr, fs, i++, null);
		FormField.HIV_CARE_ART_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.GRAVIDA.createConceptObs(obarr, fs, i++, null);
		FormField.PARITY.createConceptObs(obarr, fs, i++, null);
		FormField.CHRONIC_MEDICAL_CONDITIONS.createConceptObs(obarr, fs, i++, null);
		FormField.BLOOD_GROUP.createConceptObs(obarr, fs, i++, null);
		FormField.HEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.BASELINE_WEIGHT.createConceptObs(obarr, fs, i++, null);
		
		encounter.put("obs", obarr);
		
		return encounter;
	}
	

	private JSONObject createVerbalAutopsyEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.VERBAL_AUTOPSY_FORM_ID.OMR_FIELD(), FormField.VERBAL_AUTOPSY_FORM_ID.DEFAULT_VALUE());
		encounter.put(FormField.VERBAL_AUTOPSY_FORM_TYPE.OMR_FIELD(), FormField.VERBAL_AUTOPSY_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.LOCATION.OMR_FIELD(), FormField.LOCATION.DEFAULT_VALUE());
		encounter.put(FormField.REGISTRATION_DATE.OMR_FIELD(), fs.getField(FormField.REGISTRATION_DATE.SRP_FIELD())+" 00:00:00");

		FormField.ENCOUNTER_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.TOWN.createConceptObs(obarr, fs, i++, null);
		

		FormField.START.createConceptObs(obarr, fs, i++, null);
		FormField.END.createConceptObs(obarr, fs, i++, null);
		FormField.TODAY.createConceptObs(obarr, fs, i++, null);
		FormField.DEVICEID.createConceptObs(obarr, fs, i++, null);
		FormField.SUBSCRIBERID.createConceptObs(obarr, fs, i++, null);
		
		int i = 1;
		JSONArray obarr = new JSONArray();

		FormField.PERSON_IDENTIFIER.createConceptObs(obarr, fs, i++, null);
		FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, fs.getField(PersonField.FIRST_NAME.SRP_FIELD()), i++, null);
		FormField.BIRTHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.DEATHDATE.createConceptObs(obarr, fs, i++, null);
		FormField.GENDER.createConceptObs(obarr, fs, i++, null);
		
		FormField.NIC.createConceptObs(obarr, fs, i++, null);
		FormField.AGE.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.CITIZENSHIP.createConceptObs(obarr, fs, i++, null);
		FormField.ETHNICITY.createConceptObs(obarr, fs, i++, null);
		
		FormField.ADDRESS_BIRTHPLACE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_BIRTHPLACE_STREET.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_BIRTHPLACE_PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_BIRTHPLACE_DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_BIRTHPLACE_TOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_BIRTHPLACE_UC.createConceptObs(obarr, fs, i++, null);
		
		FormField.ADDRESS_USUAL_RESIDENCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUAL_RESIDENCE_STREET.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUAL_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUAL_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUAL_RESIDENCE_TOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_USUAL_RESIDENCE_UC.createConceptObs(obarr, fs, i++, null);

		FormField.ADDRESS_PREVIOUS_RESIDENCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_STREET.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_TOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_UC.createConceptObs(obarr, fs, i++, null);

		FormField.ADDRESS_DEATHPLACE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATHPLACE_STREET.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATHPLACE_PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATHPLACE_DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATHPLACE_TOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATHPLACE_UC.createConceptObs(obarr, fs, i++, null);

		FormField.MARITAL_STATUS.createConceptObs(obarr, fs, i++, null);
		FormField.MARRIAGE_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.EDUCATION.createConceptObs(obarr, fs, i++, null);
		FormField.ABILITY_READ_WRITE.createConceptObs(obarr, fs, i++, null);
		FormField.ECONOMIC_ACTIVITY_STATUS.createConceptObs(obarr, fs, i++, null);
		FormField.OCCUPATION.createConceptObs(obarr, fs, i++, null);

		FormField.DEATH_REGISTRATION_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.DEATH_REGISTRATION_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION_STREET.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION_PROVINCE.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION_DISTRICT.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION_TOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ADDRESS_DEATH_REGISTRATION_UC.createConceptObs(obarr, fs, i++, null);

		FormField.RESPONDENT_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.RESPONDENT_RELATIONSHIP.createConceptObs(obarr, fs, i++, null);
		FormField.RESPONDENT_LIVED_WITH_DECEASED.createConceptObs(obarr, fs, i++, null);
		FormField.INTERVIEWER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.INTERVIEW_TIME.createConceptObs(obarr, fs, i++, null);
		FormField.INTERVIEW_DATE.createConceptObs(obarr, fs, i++, null);
		
		FormField.TUBERCULOSIS.createConceptObs(obarr, fs, i++, null);
		FormField.AIDS.createConceptObs(obarr, fs, i++, null);
		FormField.MALARIA_POSITIVE.createConceptObs(obarr, fs, i++, null);
		FormField.MALARIA_NEGATIVE.createConceptObs(obarr, fs, i++, null);
		FormField.MEASLES.createConceptObs(obarr, fs, i++, null);
		FormField.HYPERTENSION.createConceptObs(obarr, fs, i++, null);
		FormField.HEART_DISEASE.createConceptObs(obarr, fs, i++, null);
		FormField.DIABETES.createConceptObs(obarr, fs, i++, null);
		FormField.ASTHMA.createConceptObs(obarr, fs, i++, null);
		FormField.EPILEPSY.createConceptObs(obarr, fs, i++, null);
		FormField.CANCER.createConceptObs(obarr, fs, i++, null);
		FormField.COPD.createConceptObs(obarr, fs, i++, null);
		FormField.DEMENTIA.createConceptObs(obarr, fs, i++, null);
		FormField.DEPRESSION.createConceptObs(obarr, fs, i++, null);
		FormField.STROKE.createConceptObs(obarr, fs, i++, null);
		FormField.SICKLE_CELL.createConceptObs(obarr, fs, i++, null);
		FormField.RENAL_DISEASE.createConceptObs(obarr, fs, i++, null);
		FormField.HEPATIC_DISEASE.createConceptObs(obarr, fs, i++, null);
		FormField.WET_SEASON_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.DRY_SEASON_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.FINAL_ILLNESS_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.SUDDEN_DEATH.createConceptObs(obarr, fs, i++, null);

		FormField.FEVER.createConceptObs(obarr, fs, i++, null);
		FormField.FEVER_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.NIGHT_SWEATS.createConceptObs(obarr, fs, i++, null);
		FormField.COUGH.createConceptObs(obarr, fs, i++, null);
		FormField.COUGH_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.COUGH_PRODUCTIVE.createConceptObs(obarr, fs, i++, null);
		FormField.HEMOPTYSIS.createConceptObs(obarr, fs, i++, null);
		FormField.COUGH_WHOOPING.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_PROBLEMS.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_FAST.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_FAST_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.BREATHLESSNESS.createConceptObs(obarr, fs, i++, null);
		FormField.BREATHLESSNESS_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.BREATHLESSNESS_PROBLEM_DAILY_ROUTINES.createConceptObs(obarr, fs, i++, null);
		FormField.BREATHLESSNESS_LYING_FLAT.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_PULL_RIBS.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_NOISY.createConceptObs(obarr, fs, i++, null);
		FormField.CHESTPAIN.createConceptObs(obarr, fs, i++, null);
		FormField.DIARRHOEA.createConceptObs(obarr, fs, i++, null);
		FormField.DIARRHOEA_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.HEMATOCHEZIA .createConceptObs(obarr, fs, i++, null);
		FormField.VOMIT.createConceptObs(obarr, fs, i++, null);
		FormField.VOMIT_COFFEE_GROUNDS.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMINAL_PROBLEM.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMINAL_PAIN.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMINAL_PAIN_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMEN_PROTRUDING.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMEN_PROTRUDING_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMINAL_MASS.createConceptObs(obarr, fs, i++, null);
		FormField.ABDOMINAL_MASS_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.HEADACHE.createConceptObs(obarr, fs, i++, null);
		FormField.NUCHAL_RIGIDITY.createConceptObs(obarr, fs, i++, null);
		FormField.NUCHAL_RIGIDITY_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.CONFUSIONAL_STATE.createConceptObs(obarr, fs, i++, null);
		FormField.CONFUSIONAL_STATE_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.UNCONSCIOUSNESS.createConceptObs(obarr, fs, i++, null);
		FormField.UNCONSCIOUSNESS_SUDDEN.createConceptObs(obarr, fs, i++, null);
		FormField.SEIZURE.createConceptObs(obarr, fs, i++, null);
		FormField.SEIZURE_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.SEIZURE_LEAD_UNCONSCIOUSNESS.createConceptObs(obarr, fs, i++, null);
		FormField.URINE.createConceptObs(obarr, fs, i++, null);
		FormField.URINE_SUPPRESSED.createConceptObs(obarr, fs, i++, null);
		FormField.URINE_EXCESS.createConceptObs(obarr, fs, i++, null);
		FormField.HEMATURIA.createConceptObs(obarr, fs, i++, null);
		FormField.SKIN_PROBLEMS.createConceptObs(obarr, fs, i++, null);
		FormField.ULCER_BODY.createConceptObs(obarr, fs, i++, null);
		FormField.ULCER_FEET.createConceptObs(obarr, fs, i++, null);
		FormField.SKIN_RASH.createConceptObs(obarr, fs, i++, null);
		FormField.SKIN_RASH_DURATION.createConceptObs(obarr, fs, i++, null);
		FormField.SKIN_RASH_MEASLES.createConceptObs(obarr, fs, i++, null);
		FormField.HERPES_ZOSTER.createConceptObs(obarr, fs, i++, null);
		FormField.BLEED_MOUTH_NOSE_ANUS.createConceptObs(obarr, fs, i++, null);
		FormField.WEIGHT_LOSS.createConceptObs(obarr, fs, i++, null);
		FormField.EMACIATED.createConceptObs(obarr, fs, i++, null);
		FormField.MOUTHSORE.createConceptObs(obarr, fs, i++, null);
		FormField.BODY_STIFFNESS.createConceptObs(obarr, fs, i++, null);
		FormField.FACIAL_SWELLING.createConceptObs(obarr, fs, i++, null);
		FormField.FEET_SWELLING.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS_MOUTH.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS_NECK.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS_ARMPIT.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS_GROIN.createConceptObs(obarr, fs, i++, null);
		FormField.LUMPS_BREAST.createConceptObs(obarr, fs, i++, null);
		FormField.HEMIPLEGIA .createConceptObs(obarr, fs, i++, null);
		FormField.DYSPHAGIA.createConceptObs(obarr, fs, i++, null);
		FormField.DISCOLORATION_EYES.createConceptObs(obarr, fs, i++, null);
		FormField.DISCOLORATION_HAIR.createConceptObs(obarr, fs, i++, null);
		FormField.PALLOR.createConceptObs(obarr, fs, i++, null);
		FormField.SUNKEN_EYES.createConceptObs(obarr, fs, i++, null);
		FormField.EXCESSIVE_WATER_INPUT.createConceptObs(obarr, fs, i++, null);
		FormField.MENSTRUAL_HEAVY_BLEED.createConceptObs(obarr, fs, i++, null);
		FormField.MENOPAUSE.createConceptObs(obarr, fs, i++, null);
		FormField.POST_MENOPAUSE_BLEED.createConceptObs(obarr, fs, i++, null);

		FormField.GRAVIDA.createConceptObs(obarr, fs, i++, null);
		FormField.MULTIPLE_PREGNANCY.createConceptObs(obarr, fs, i++, null);
		FormField.PIH.createConceptObs(obarr, fs, i++, null);
		FormField.LOCHIA_OFFENSIVE.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_SEIZURE.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_BLURRED_VISION.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_VAGINAL_BLEED_LAST_3M.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_BREECH.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_HEALTH_FACILITY.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_HOME.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_OTHER.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_ASSISTED_PROFESSIONALLY.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_NORMAL.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_INSTRUMENTAL.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_CAESAREAN.createConceptObs(obarr, fs, i++, null);
		
		FormField.NOT_PREGNANT_DELIVERED_WITHIN_6W.createConceptObs(obarr, fs, i++, null);
		FormField.PREGNANT.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERED_WITHIN_6W.createConceptObs(obarr, fs, i++, null);
		FormField.PREGNANCY_TREMINATED_WITHIN_6W.createConceptObs(obarr, fs, i++, null);
		FormField.GROUP_PREGNANCY_YES.createConceptObs(obarr, fs, i++, null);
		FormField.DEATH_WITHIN_24HR_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.DEATH_DURING_LABOUR_UNDELIVERED.createConceptObs(obarr, fs, i++, null);
		FormField.BREASTFEEDING.createConceptObs(obarr, fs, i++, null);
		FormField.HISTORY_CAESAREAN.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERED_HEALTHY_BABY.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_VAGINAL_BLEED_HEAVY.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_VAGINAL_BLEED_FIRST_6M.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_VAGINAL_BLEED_HEAVY_LABOUR.createConceptObs(obarr, fs, i++, null);
		FormField.MATERNAL_VAGINAL_BLEED_HEAVY_POST_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.RETAINED_PLACENTA.createConceptObs(obarr, fs, i++, null);
		FormField.LONG_LABOUR.createConceptObs(obarr, fs, i++, null);
		FormField.ABORTION_SELF_INDUCED.createConceptObs(obarr, fs, i++, null);
		FormField.ABORTION.createConceptObs(obarr, fs, i++, null);
		FormField.HYSTERECTOMY_BEFORE_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_PREMATURE.createConceptObs(obarr, fs, i++, null);
	
		FormField.DELIVERY_COMPLICATED.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_LATE_24HRS_WATERBROKE.createConceptObs(obarr, fs, i++, null);
		FormField.FETAL_MOVEMENT_CEASED_PRELABOUR.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_ABNORMAL_SIZE.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_UNDER_WEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_OVER_WEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.GESTATIONAL_AGE.createConceptObs(obarr, fs, i++, null);
		FormField.NUCHAL_CORD.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_MALFORMATION.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_BACK_DEFECTED.createConceptObs(obarr, fs, i++, null);
		FormField.MACROCEPHALY .createConceptObs(obarr, fs, i++, null);
		FormField.MICROCEPHALUS.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_ABNORMAL_GROWTH.createConceptObs(obarr, fs, i++, null);
		FormField.BLUEBABY.createConceptObs(obarr, fs, i++, null);
		FormField.CRIED_AFTER_BIRTH.createConceptObs(obarr, fs, i++, null);
		FormField.BREATHED_AFTER_BIRTH.createConceptObs(obarr, fs, i++, null);
		FormField.BREATH_ASSISTED.createConceptObs(obarr, fs, i++, null);
		FormField.STILLBIRTH.createConceptObs(obarr, fs, i++, null);
		FormField.STILLBIRTH_MACERATED.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_ABLE_TO_SUCKLE_WITHIN_24HR.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_STOP_SUCKLE_AFTER_3D.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_SEIZURE_WITHIN_1D.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_SEIZURE_AFTER_2D.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_STIFF_BODY_ARCHED_BACK.createConceptObs(obarr, fs, i++, null);
		FormField.BULGING_FONTANELLE.createConceptObs(obarr, fs, i++, null);
		FormField.SUNKEN_FONTANELLE.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_UNCONSCIOUS_WITHIN_24HR.createConceptObs(obarr, fs, i++, null);
		FormField.BABY_UNCONSCIOUS_AFTER_1D.createConceptObs(obarr, fs, i++, null);
		FormField.COLD_BEFORE_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.UMBILICAL_DISCHARGE.createConceptObs(obarr, fs, i++, null);
		FormField.YELLOW_PALM_SOLE.createConceptObs(obarr, fs, i++, null);
		FormField.MOTHER_NOT_RECEIVED_TT.createConceptObs(obarr, fs, i++, null);
		
		FormField.ACCIDENTAL_DEATH.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_ROAD_TRAFFIC.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_AS_PEDESTRIAN.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_AS_CAR_OCCUPANT.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_AS_HEAVY_VEHICLE_OCCUPANT.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_AS_BIKE_OCCUPANT.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_AS_CYCLIST.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_COUNTERPART_KNOWN.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_PEDESTRIAN.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_STATIONARY_OBJECT.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_CAR.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_HEAVY_VEHICLE.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_BIKE.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_CYCLE.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_BY_OTHER.createConceptObs(obarr, fs, i++, null);
		FormField.ACCIDENT_NON_ROAD_TRAFFIC.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_FALL.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_DROWNING.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_BURNS.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_BITE.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_BITE_DOG.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_BITE_SNAKE.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_BITE_INSECT.createConceptObs(obarr, fs, i++, null);
		FormField.INJURY_NATURAL_DISASTER.createConceptObs(obarr, fs, i++, null);
		FormField.POISONING.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT_INTENTIONAL.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT_INTENTIONAL_FIREARM.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT_INTENTIONAL_STAB_CUT.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT_INTENTIONAL_MACHINERY.createConceptObs(obarr, fs, i++, null);
		FormField.ASSAULT_INTENTIONAL_ANIMAL.createConceptObs(obarr, fs, i++, null);
		FormField.SUICIDE.createConceptObs(obarr, fs, i++, null);

		FormField.ALCOHOL_DRINKER.createConceptObs(obarr, fs, i++, null);
		FormField.SMOKER.createConceptObs(obarr, fs, i++, null);

		FormField.VACCINATED_ADEQUATELY.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_FINAL_ILLNESS_RECEIVED.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_REHYDRATION_SALTS.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_IV_FLUID.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_BLOOD_TRANSFUSION.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_NASOGASTRIC_TUBE.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_INJECTABLE_ANTIBIOTIC.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_OPERATION.createConceptObs(obarr, fs, i++, null);
		FormField.TREATMENT_OPERATION_WITHIN_1M.createConceptObs(obarr, fs, i++, null);
		FormField.DISCHARGED_SERIOUSLY_ILL.createConceptObs(obarr, fs, i++, null);

		FormField.FINALILLNESS_HEALTHFACILITY_TRAVELLED.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_HEALTHFACILITY_TRAVELLED_MOTOR_VEHICLE.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_HEALTHFACILITY_ADMISSION_PROBLEM.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_HEALTHFACILITY_TREATMENT_PROBLEM.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_HEALTHFACILITY_MEDICATION_TEST_PROBLEM.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_HEALTHFACILITY_TRAVEL_GT2HR.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_TREATMENT_NEED_DOUBT.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_TREATMENT_TRADITIONAL_MEDICINE.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_TREATMENT_HELP_CALLED_VIA_PHONE.createConceptObs(obarr, fs, i++, null);
		FormField.FINALILLNESS_TREATMENT_EXCEED_OFFORDABILITY.createConceptObs(obarr, fs, i++, null);

		FormField.ADDITIONAL_NOTE.createConceptObs(obarr, fs, i++, null);
		
		encounter.put("obs", obarr);
		
		return encounter;
	}
	
	public String syncOpenmrsLocations() throws JSONException{
		String appUrl = removeEndingSlash(openmrsOpenmrsUrl.trim());
		String serviceUrl =  removeEndingSlash(openmrsLocationListUrl.trim());
		String mainurl = appUrl+serviceUrl;
		
		String currenturl = mainurl;
		while(true){
			System.out.println(currenturl);
			HttpResponse response = httpAgent.get(currenturl, openmrsUsername, openmrsPassword);
			JSONObject jso = new JSONObject(response.body());
			JSONArray loclist = jso.getJSONArray(Location.LOCATION_LIST.OMR_FIELD());
			
			for (int i = 0; i < loclist.length(); i++) {
				JSONObject loc = loclist.getJSONObject(i);
				String uuid = loc.getString(Location.UUID.OMR_FIELD());
				///String name = loc.getString(Location.NAME.OMR_FIELD());
				
				JSONObject locfull = new JSONObject(httpAgent.get(mainurl+"/"+uuid, openmrsUsername, openmrsPassword).body());
				
				OLocation oloc = locationService.getLocations(uuid);
				OLocation loccr = createOrUpdateLoction(locfull, oloc);
				if(oloc == null){
					locationService.saveLocation(loccr);
				}
				else {
					locationService.updateLocation(loccr);
				}
			}
			
			if(Location.getNextPageLink(jso) != null){
				currenturl = Location.getNextPageLink(jso);
			}
			else {
				break;
			}
		}
		//JSONArray linklist = jso.getJSONArray("links");
		//System.out.println("BODY:::"+response.body());
		return "done";
	}
	
	private OLocation createOrUpdateLoction(JSONObject locfull, OLocation existinglocation) throws JSONException {
		OLocation loc = new OLocation();

		if(existinglocation != null){
			loc = existinglocation;
		}
		System.out.println("LOCFULL::::::::::::"+locfull);
		System.out.println("LOC::::::::::::"+loc);
		loc.setLocationId(locfull.getString(Location.UUID.OMR_FIELD()));

		loc.setAddress1(locfull.getString(Location.ADDRESS1.OMR_FIELD()));
		loc.setAddress2(locfull.getString(Location.ADDRESS2.OMR_FIELD()));
		JSONArray children = locfull.getJSONArray(Location.CHILDREN.OMR_FIELD());
		Set<OLocation> chlset = new HashSet<OLocation>();
		for (int i = 0; i < children.length(); i++) {
			OLocation chl = new OLocation();
			chl.setLocationId(children.getJSONObject(i).getString(Location.UUID.OMR_FIELD()));
			chl.setName(children.getJSONObject(i).getString(Location.NAME.OMR_FIELD()));
			chlset.add(chl);
		}
		loc.setChildLocations(chlset);
		loc.setCityVillage(locfull.getString(Location.CITY_VILLAGE.OMR_FIELD()));
		loc.setCountry(locfull.getString(Location.COUNTRY.OMR_FIELD()));
		loc.setCountyDistrict(locfull.getString(Location.COUNTY_DISTRICT.OMR_FIELD()));
		loc.setDateChanged(null);
		loc.setDateCreated(new Date());
		loc.setDescription(locfull.getString(Location.DESCRIPTION.OMR_FIELD()));
		loc.setLatitude(locfull.getString(Location.LATITUDE.OMR_FIELD()));
		
		JSONArray attributes = locfull.getJSONArray(Location.ATTRIBUTES.OMR_FIELD());
		Map<String, Object> attr = new HashMap<String, Object>();
		for (int i = 0; i < attributes.length(); i++) {
			String dstr = attributes.getJSONObject(i).getString("display");
			String key = dstr.substring(0, dstr.indexOf(":"));
			String val = dstr.substring(dstr.indexOf(":")+1).trim();
			attr.put(key, val);
		}
		loc.setLocationAttributes(attr);
		loc.setName(locfull.getString(Location.NAME.OMR_FIELD()));
		
		if(locfull.has(Location.PARENT.OMR_FIELD()) && locfull.optJSONObject(Location.PARENT.OMR_FIELD()) != null){
			System.out.println(locfull.opt(Location.PARENT.OMR_FIELD()));
			OLocation par = new OLocation();
			JSONObject parjo = locfull.getJSONObject(Location.PARENT.OMR_FIELD());
			par.setLocationId(parjo.getString(Location.UUID.OMR_FIELD()));
			par.setName(parjo.getString(Location.NAME.OMR_FIELD()));
			loc.setParentLocation(par);
		}
		loc.setPostalCode(locfull.getString(Location.POSTAL_CODE.OMR_FIELD()));
		loc.setRetired(locfull.getBoolean(Location.RETIRED.OMR_FIELD()));
		loc.setStateProvince(locfull.getString(Location.STATE_PROVINCE.OMR_FIELD()));
		
		JSONArray tagsjsa = locfull.getJSONArray(Location.TAGS.OMR_FIELD());
		Set<String> tags = new HashSet<String>();
		for (int i = 0; i < tagsjsa.length(); i++) {
			tags.add(tagsjsa.getJSONObject(i).getString(Location.NAME.OMR_FIELD()));
		}
		
		loc.setTags(tags);
		
		return loc;
	}

	private String removeEndingSlash(String str){
		return str.endsWith("/")?str.substring(0, str.lastIndexOf("/")):str;
	}
}
