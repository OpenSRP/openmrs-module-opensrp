package org.ei.drishti.connector.openmrs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.InvalidAttributeValueException;

import org.ei.drishti.common.util.HttpAgent;
import org.ei.drishti.common.util.HttpResponse;
import org.ei.drishti.connector.Emailer;
import org.ei.drishti.connector.constants.OpenmrsConstants;
import org.ei.drishti.connector.constants.OpenmrsConstants.FormField;
import org.ei.drishti.connector.constants.OpenmrsConstants.Location;
import org.ei.drishti.connector.constants.OpenmrsConstants.PersonField;
import org.ei.drishti.domain.OLocation;
import org.ei.drishti.form.domain.FormSubmission;
import org.ei.drishti.form.service.FormSubmissionService;
import org.ei.drishti.service.LocationService;
import org.ihs.emailer.EmailEngine;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ibm.icu.text.SimpleDateFormat;
import com.mysql.jdbc.StringUtils;

@Service
public class OpenmrsService {
    private static Logger logger = LoggerFactory.getLogger("OMRS_SERVICE");

    private static String openmrsOpenmrsUrl;
    private static String openmrsUsername;
    private static String openmrsPassword;
    private static String openmrsOpenSrpConnectorUrl;
    private static String openmrsOpenSrpConnectorContentParam;
    private static String openmrsLocationListUrl;
    private static final String OPENMRS_SYNC_ATTR_NAME = "openmrsSynced";
    private static final String OPENMRS_CASEID_ATTR_NAME = "openmrsCaseId";

    private HttpAgent httpAgent;
    private FormSubmissionService formSubmissionService;
    private LocationService locationService;

    @Autowired
    public OpenmrsService(FormSubmissionService formSubmissionService, 
    		LocationService locationService,
    		@Value("#{drishti['openmrs.url']}") String openmrsOpenmrsUrl,
    		@Value("#{drishti['openmrs.username']}") String openmrsUsername,
    		@Value("#{drishti['openmrs.password']}") String openmrsPassword,
    		@Value("#{drishti['openmrs.opensrp-connector.url']}") String openmrsOpenSrpConnectorUrl,
    		@Value("#{drishti['openmrs.opensrp-connector.content-param']}") String openmrsOpenSrpConnectorContentParam, 
    		@Value("#{drishti['openmrs.location-list.url']}") String openmrsLocationListUrl,
    		HttpAgent httpAgent) {
        OpenmrsService.openmrsOpenmrsUrl = openmrsOpenmrsUrl;
        OpenmrsService.openmrsUsername = openmrsUsername;
        OpenmrsService.openmrsPassword = openmrsPassword;
        OpenmrsService.openmrsOpenSrpConnectorUrl = openmrsOpenSrpConnectorUrl;
        OpenmrsService.openmrsOpenSrpConnectorContentParam = openmrsOpenSrpConnectorContentParam;
        OpenmrsService.openmrsLocationListUrl = openmrsLocationListUrl;
        
        this.httpAgent = httpAgent;
        this.formSubmissionService = formSubmissionService;
        this.locationService = locationService;
    }

    public String pushDataToOpenmrs(String formToPush, long serverVersion) throws JSONException, FileNotFoundException, IOException {
    	if(!Emailer.EMAILER_INSTANTIATED)// TODO hack to instantiate emailengine if not done
		{
			Emailer.intantiateEmailer();
		}
    	List<FormSubmission> fsl = formSubmissionService.getSubmissionByOpenmrsNotSynced(formToPush);
    	
		for (FormSubmission formSubmission : fsl) {
			try{
			JSONObject json = makeRequestContent(formSubmission);
			
			String appUrl = removeEndingSlash(openmrsOpenmrsUrl);
			String serviceUrl =  removeTrailingSlash(removeEndingSlash(openmrsOpenSrpConnectorUrl));
			
			logger.info("URL:"+appUrl+"/"+serviceUrl+"\nJSON::"+json.toString());

			HttpResponse response = httpAgent.post(appUrl+"/"+serviceUrl, "username="+openmrsUsername+"&password="+openmrsPassword, json.toString(), openmrsOpenSrpConnectorContentParam);
			
			logger.info("RESPONSE:"+response.body());
			
			JSONObject resp = new JSONObject(response.body());
			
			String openmrsCaseId = (String) formSubmission.getFormMetaData().get(OPENMRS_CASEID_ATTR_NAME);
			
			if(!response.isSuccess() || resp.has("ERROR")){
				String subject = "ERROR IN SUBMITTED "+formToPush +" FORM FOR OMRS-ID "+openmrsCaseId;
				String message = "FORM: "+formToPush+"\nOMRS-ID: "+openmrsCaseId+"\nFS-ID: "+formSubmission.entityId()+"\nOpenMRS RESPONSE:\n"+resp.getString("ERROR_MESSAGE");
				EmailEngine.getInstance().emailReportToAdminInSeparateThread(subject, message);
				// if error mark as synced and put error message to avoid spamming with errors
				formSubmission.getFormMetaData().put(OPENMRS_SYNC_ATTR_NAME, true);
				formSubmission.getFormMetaData().put("ERROR", resp.getString("ERROR_MESSAGE"));
				formSubmissionService.update(formSubmission);
			}
			else if(resp.has(openmrsCaseId) && resp.getString(openmrsCaseId).equalsIgnoreCase("success")){
				formSubmission.getFormMetaData().put(OPENMRS_SYNC_ATTR_NAME, true);
				formSubmissionService.update(formSubmission);
			}
			
			}
			catch(Exception e){
				e.printStackTrace();
				
				String subject = "ERROR POSTING "+formToPush +" FORM FOR FS-ID "+formSubmission.entityId();
				String message = "FORM: "+formToPush+"\nFS-ID: "+formSubmission.entityId()+"\nEXCEPTION:\n"+ExceptionUtil.getStackTrace(e);

				EmailEngine.getInstance().emailReportToAdminInSeparateThread(subject, message);
				logger.error(subject+ "\nEXCEPTION : " + ExceptionUtil.getStackTrace(e));
			}
		}
		return "DONE";
	}
    
    public JSONObject makeRequestContent(FormSubmission fs) throws JSONException, InvalidAttributeValueException{
    	JSONObject json = new JSONObject();
		
		JSONArray mainObj = new JSONArray();
		
		JSONObject pd = new JSONObject();

		//PATIENT/PERSON INFO
		JSONObject patient = new JSONObject();
		patient.put(PersonField.IDENTIFIER.OMR_FIELD(), fs.entityId());
		patient.put(PersonField.IDENTIFIER_TYPE.OMR_FIELD(), "CRVS_IDENTIFIER");
		String firstname = PersonField.FIRST_NAME.SRP_VALUE(fs);
		String lastname = PersonField.LAST_NAME.SRP_VALUE(fs);
		patient.put(PersonField.FIRST_NAME.OMR_FIELD(), StringUtils.isEmptyOrWhitespaceOnly(firstname)?".":firstname);
		patient.put(PersonField.LAST_NAME.OMR_FIELD(), StringUtils.isEmptyOrWhitespaceOnly(lastname)?".":lastname);
		patient.put(PersonField.GENDER.OMR_FIELD(), PersonField.GENDER.SRP_VALUE(fs));
		
		if(PersonField.BIRTHDATE.SRP_VALUE(fs) != null){
			patient.put(PersonField.BIRTHDATE.OMR_FIELD(), PersonField.BIRTHDATE.SRP_VALUE(fs));
			patient.put(PersonField.BIRTHDATE_IS_APPROX.OMR_FIELD(), false);
		}
		else if(PersonField.AGE.SRP_VALUE(fs) != null){
			Calendar bdc = Calendar.getInstance();
			bdc.add(Calendar.YEAR, -Integer.parseInt(PersonField.AGE.SRP_VALUE(fs)));
			patient.put(PersonField.BIRTHDATE.OMR_FIELD(), OpenmrsConstants.OPENMRS_DATE.format(bdc.getTime()));
		}
		
		patient.put(PersonField.DEATHDATE.OMR_FIELD(), PersonField.DEATHDATE.SRP_VALUE(fs));
		patient.put(FormField.FORM_CREATOR.OMR_FIELD(), fs.anmId());
		
		//ENCOUNTER/EVENT/FORM INFO
		JSONArray en = new JSONArray();
		
		if(fs.formName().toLowerCase().contains("pregnancy")){
			patient.put(PersonField.GENDER.OMR_FIELD(), "FEMALE");

			patient.put(PersonField.IS_NEW_PERSON.OMR_FIELD(), true);
			fs.getFormMetaData().put(OPENMRS_CASEID_ATTR_NAME, fs.entityId());
			en.put(createPregnancyNotificationEncounterAndObs(fs));
		}
		else if(fs.formName().toLowerCase().contains("birth")){
			patient.put(PersonField.IS_NEW_PERSON.OMR_FIELD(), true);
			fs.getFormMetaData().put(OPENMRS_CASEID_ATTR_NAME, fs.entityId());
			en.put(createBirthEncounterAndObs(fs));
		}
		else if(fs.formName().toLowerCase().contains("death")){
			patient.put(PersonField.IS_NEW_PERSON.OMR_FIELD(), true);
			fs.getFormMetaData().put(OPENMRS_CASEID_ATTR_NAME, fs.entityId());
			en.put(createDeathEncounterAndObs(fs));
		}
		else if(fs.formName().toLowerCase().contains("autopsy")){
			if(!Emailer.EMAILER_INSTANTIATED)// TODO hack to instantiate emailengine if not done
			{
				Emailer.intantiateEmailer();
			}
			// override ID to be same as of person occupying same NIC..... i.e. linking Death and VA form
			List<FormSubmission> deathfs = formSubmissionService.getSubmissionByFormAndNICFieldValue("crvs_death_notification", FormField.NIC.OSRP_FIELD(), FormField.NIC.SRP_VALUE(fs));
			if(deathfs.size() == 0){
				String message = "NO Death Notification Form was found for Verbal Autopsy filled for NIC : "+FormField.NIC.SRP_VALUE(fs) 
						+ "\n VA - EntityID : "+fs.entityId()
						+ "\n FormInstance ID : "+FormField.FORM_BACKLOG_ID.SRP_VALUE(fs);
				EmailEngine.getInstance().emailReportToAdminInSeparateThread("NO DEATH NOTIFICATION FOUND", message);
				fs.getFormMetaData().put(OPENMRS_SYNC_ATTR_NAME, true);
				fs.getFormMetaData().put("ERROR", "NO DEATH NOTIFICATION FOUND");
				formSubmissionService.update(fs);
				throw new InvalidAttributeValueException(message);
			}
			else if(deathfs.size() > 1){
				String message = "MULTIPLE Death Notification Forms found for Verbal Autopsy filled for NIC : "+FormField.NIC.SRP_VALUE(fs) 
						+ "\n VA - EntityID : "+fs.entityId()
						+ "\n FormInstance ID : "+FormField.FORM_BACKLOG_ID.SRP_VALUE(fs)
						+ ""
						+ "\n\n Assigned VA-form to first entity with CRVS_ID : "+deathfs.get(0).entityId();
				EmailEngine.getInstance().emailReportToAdminInSeparateThread("MULTIPLE DEATH NOTIFICATIONS FOUND", message);
			}
			
			if(deathfs.get(0).getFormMetaData().get(OPENMRS_SYNC_ATTR_NAME) == null
					|| !deathfs.get(0).getFormMetaData().get(OPENMRS_SYNC_ATTR_NAME).toString().equalsIgnoreCase("true")){
				throw new InvalidAttributeValueException("DEATH Notification Form still not PUSHED to openmrs. Can not continue without before Death Form is pushed");
			}
			
			patient.put(PersonField.IDENTIFIER.OMR_FIELD(), deathfs.get(0).entityId());
			patient.put(PersonField.IDENTIFIER_TYPE.OMR_FIELD(), "CRVS_IDENTIFIER");
			patient.put(PersonField.IS_NEW_PERSON.OMR_FIELD(), false);
			fs.getFormMetaData().put(OPENMRS_CASEID_ATTR_NAME, deathfs.get(0).entityId());
			en.put(createVerbalAutopsyEncounterAndObs(fs));
		}

		pd.put("patient", patient);

		pd.put("encounter", en);
		
		JSONObject j = new JSONObject();
		j.put("patientData", pd);
		
		mainObj.put(j);
					
		json.put("MainObj",mainObj);
		
		return json;
    }
    
    /**
     * Variables Covered in obs filler
     * <ul><li>PERSON_IDENTIFIER,
     * <li>PERSON_FULL_NAME,
     * <li>BIRTHDATE,
     * <li>DEATHDATE,
     * <li>GENDER,
     * <li>AGE,
     * <li>MARITAL_STATUS,
     * <li>MARRIAGE_DATE,
     * <li>ABILITY_READ_WRITE,
     * <li>CITIZENSHIP,
     * <li>ECONOMIC_ACTIVITY_STATUS,
     * <li>EDUCATION,
     * <li>ETHNICITY,
     * <li>NIC,
     * <li>RELIGION,
     * <li>OCCUPATION</ul>
     * @param fs - formSubmission to fill data From
     * @param obarr - jsonArray object to make openmrs acceptable obs list
     * @param indexObsStart - index / obs number to start from and to return so that calling method can maintain proper obs ids/numbering 
     * @return obs_id/index to assign to next obs for the encounter object`s obs list
     * @throws JSONException
     */
    private int fillPersonDetailsObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
    	//Person details in OBS
		FormField.PERSON_IDENTIFIER.createConceptObs(obarr, fs, indexObsStart++, null);
		
		String firstName = PersonField.FIRST_NAME.SRP_VALUE(fs);
		String lastName = PersonField.LAST_NAME.SRP_VALUE(fs);
		if(!StringUtils.isEmptyOrWhitespaceOnly(firstName)){ 
			if(!StringUtils.isEmptyOrWhitespaceOnly(lastName)){
				FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, firstName + " " + lastName, indexObsStart++, null);
			}
			else {
				FormField.PERSON_FULL_NAME.createConceptObsWithValue(obarr, firstName, indexObsStart++, null);
			}
		}
		
		FormField.BIRTHDATE.createConceptObs(obarr, fs, indexObsStart++, null);
		FormField.DEATHDATE.createConceptObs(obarr, fs, indexObsStart++, null);
		FormField.GENDER.createConceptObs(obarr, fs, indexObsStart++, null);
		
		String age = fs.getField(FormField.AGE.OSRP_FIELD());
		// To make sure that floating point doenot go with age since it throws error.precision exception
		age = StringUtils.isEmptyOrWhitespaceOnly(age)?null:""+((int)Double.parseDouble(age));
		FormField.AGE.createConceptObsWithValue(obarr, age, indexObsStart++, null);
		
		FormField.MARITAL_STATUS.createConceptObs(obarr, fs, indexObsStart++, null);
		FormField.MARRIAGE_DATE.createConceptObs(obarr, fs, indexObsStart++, null);
		FormField.ABILITY_READ_WRITE.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.CITIZENSHIP.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.ECONOMIC_ACTIVITY_STATUS.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.EDUCATION.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.ETHNICITY.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.NIC.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.RELIGION.createConceptObs(obarr, fs, indexObsStart, null);
		FormField.OCCUPATION.createConceptObs(obarr, fs, indexObsStart, null);
		
		return indexObsStart;
    }
    
    public static void main(String[] args) throws ParseException {
		logger.info("LOGGEDDDDDDDDDDDDDDDDDDDDDDD");

    	SimpleDateFormat sd = new SimpleDateFormat("HH:mm");
		System.out.println(sd.parse("23:11"));
    	//String age = "777";
		//System.out.println((int)Double.parseDouble(age));
		
//		System.out.println(OpenmrsConstants.OPENMRS_DATETIME.format(new Date(org.joda.time.DateTime.parse("2015-01-09T11:43:29.000+05:00").getMillis())));
	}
    private int fillEncounterAndFormDetailsObs(FormSubmission fs, JSONObject encounter, JSONArray obarr, int indexObsStart) throws JSONException{
    	//Form details in OBS
    	encounter.put(FormField.ENCOUNTER_LOCATION.OMR_FIELD(), FormField.ENCOUNTER_LOCATION.SRP_VALUE(fs));
		encounter.put(FormField.ENCOUNTER_DATE.OMR_FIELD(), FormField.ENCOUNTER_DATE.SRP_VALUE(fs)+" 00:00:00");
		encounter.put(FormField.FORM_CREATOR.OMR_FIELD(), fs.anmId());

		JSONObject address = FormField.ADDRESS_ENCOUNTER.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_ENCOUNTER_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_ENCOUNTER_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_ENCOUNTER_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_ENCOUNTER_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_ENCOUNTER_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		FormField.ENCOUNTER_CENTER_TYPE.createConceptObs(obarr, fs, indexObsStart++, null);
		FormField.ENCOUNTER_CENTER.createConceptObs(obarr, fs, indexObsStart++, null);
		
		FormField.FORM_BACKLOG_ID.createConceptObsWithValue(obarr, fs.getId(), indexObsStart++, null);
		FormField.FORM_DATE.createConceptObs(obarr, fs, indexObsStart++, null);
		
		String vfs = OpenmrsConstants.OPENMRS_DATETIME.format(new Date(DateTime.parse(FormField.FORM_START_DATETIME.SRP_VALUE(fs)).getMillis()));
		FormField.FORM_START_DATETIME.createConceptObsWithValue(obarr, vfs, indexObsStart++, null);
		
		String vfe = OpenmrsConstants.OPENMRS_DATETIME.format(new Date(DateTime.parse(FormField.FORM_END_DATETIME.SRP_VALUE(fs)).getMillis()));
		FormField.FORM_END_DATETIME.createConceptObsWithValue(obarr, vfe, indexObsStart++, null);
		
		return indexObsStart;
    }
    
    private int fillAddressUsualResidenceObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
		JSONObject address = FormField.ADDRESS_USUAL_RESIDENCE.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_USUAL_RESIDENCE_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_USUAL_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_USUAL_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_USUAL_RESIDENCE_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_USUAL_RESIDENCE_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		return indexObsStart;
    }
    
    private int fillAddressPreviousResidenceObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
    	JSONObject address = FormField.ADDRESS_PREVIOUS_RESIDENCE.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_PREVIOUS_RESIDENCE_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_PREVIOUS_RESIDENCE_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		return indexObsStart;
    }
    
    private int fillAddressBirthplaceObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
    	JSONObject address = FormField.ADDRESS_BIRTHPLACE.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_BIRTHPLACE_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_BIRTHPLACE_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_BIRTHPLACE_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_BIRTHPLACE_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_BIRTHPLACE_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		return indexObsStart;
    }
    
    private int fillAddressDeathplaceObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
    	JSONObject address = FormField.ADDRESS_DEATHPLACE.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_DEATHPLACE_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATHPLACE_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATHPLACE_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATHPLACE_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATHPLACE_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		return indexObsStart;
    }
    
    private int fillAddressDeathRegistrationObs(FormSubmission fs, JSONArray obarr, int indexObsStart) throws JSONException{
    	JSONObject address = FormField.ADDRESS_DEATH_REGISTRATION.createParentObs(indexObsStart++);
		obarr.put(address);
		
		FormField.ADDRESS_DEATH_REGISTRATION_STREET.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATH_REGISTRATION_PROVINCE.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATH_REGISTRATION_DISTRICT.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATH_REGISTRATION_TOWN.createConceptObs(obarr, fs, indexObsStart++, address);
		FormField.ADDRESS_DEATH_REGISTRATION_UC.createConceptObs(obarr, fs, indexObsStart++, address);
		
		return indexObsStart;
    }
    
    private JSONObject createPregnancyNotificationEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.PREGNANCY_NOTIFICATION_FORM_TYPE.OMR_FIELD(), FormField.PREGNANCY_NOTIFICATION_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.PREGNANCY_NOTIFICATION_FORM_NAME.OMR_FIELD(), FormField.PREGNANCY_NOTIFICATION_FORM_NAME.DEFAULT_VALUE());

		//Fill other encounter and Form details in the end so that observations`s data comes in the end....
		int i = 1;
		JSONArray obarr = new JSONArray();

		// must get i so tht obs ids donot get repeated
		i = fillPersonDetailsObs(fs, obarr, i);
		
		// must get i so tht obs ids donot get repeated
		i = fillAddressUsualResidenceObs(fs, obarr, i);
		
		FormField.NEXT_OF_KIN.createConceptObs(obarr, fs, i++, null);
		
		FormField.ANC_REGISTRATION_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.ANC_REGISTRATION_LOCATION.createConceptObs(obarr, fs, i++, null);
		FormField.ANC_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.LMP.createConceptObs(obarr, fs, i++, null);
		FormField.EDD.createConceptObs(obarr, fs, i++, null);
		FormField.PLANNED_FACILITY_OF_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.PLANNED_TRANSPORT_FOR_DELIVERY.createConceptObs(obarr, fs, i++, null);
		FormField.ENROLLED_IN_HIV_CARE.createConceptObs(obarr, fs, i++, null);
		//FormField.ENROLLED_DATE_HIV_CARE.createConceptObs(obarr, fs, i++, null);
		//FormField.HIV_CARE_ART_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.GRAVIDA.createConceptObs(obarr, fs, i++, null);
		FormField.PARITY.createConceptObs(obarr, fs, i++, null);
		FormField.CHRONIC_MEDICAL_CONDITIONS.createConceptObs(obarr, fs, i++, null);
		FormField.BLOOD_GROUP.createConceptObs(obarr, fs, i++, null);
		FormField.HEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.BASELINE_WEIGHT.createConceptObs(obarr, fs, i++, null);
		
		// must get i so tht obs ids donot get repeated
		i = fillEncounterAndFormDetailsObs(fs, encounter, obarr, i);
		
		encounter.put("obs", obarr);
		
		return encounter;
	}
	private JSONObject createBirthEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.BIRTH_FORM_TYPE.OMR_FIELD(), FormField.BIRTH_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.BIRTH_FORM_NAME.OMR_FIELD(), FormField.BIRTH_FORM_NAME.DEFAULT_VALUE());

		int i = 1;
		JSONArray obarr = new JSONArray();
		
		// must get i so tht obs ids donot get repeated
		i = fillPersonDetailsObs(fs, obarr, i);
		
		// must get i so tht obs ids donot get repeated
		i = fillAddressUsualResidenceObs(fs, obarr, i);
				
		// must get i so tht obs ids donot get repeated
		i = fillAddressBirthplaceObs(fs, obarr, i);
		// applicant data		
		FormField.INFORMANT_FULL_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject applicant = FormField.INFORMANT_INFORMATION.createParentObs(i++);
		obarr.put(applicant);
		
		FormField.INFORMANT_NIC.createConceptObs(obarr, fs, i++, applicant);
		FormField.INFORMANT_RELATIONSHIP.createConceptObs(obarr, fs, i++, applicant);
		
		//father data
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject father = FormField.FATHER.createParentObs(i++);
		obarr.put(father);
		
		FormField.FATHER_AGE.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_MARITAL_STATUS.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_EDUCATION.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_OCCUPATION.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_CITIZENSHIP.createConceptObs(obarr, fs, i++, father);
		FormField.FATHER_NIC.createConceptObs(obarr, fs, i++, father);

		JSONObject fatherAddressUsualResidence = FormField.FATHER_ADDRESS_USUAL_RESIDENCE.createParentObs(i++);
		obarr.put(fatherAddressUsualResidence);
		
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE_STREET.createConceptObs(obarr, fs, i++, fatherAddressUsualResidence);
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, i++, fatherAddressUsualResidence);
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, i++, fatherAddressUsualResidence);
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE_TOWN.createConceptObs(obarr, fs, i++, fatherAddressUsualResidence);
		FormField.FATHER_ADDRESS_USUAL_RESIDENCE_UC.createConceptObs(obarr, fs, i++, fatherAddressUsualResidence);
		
		//mother data
		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject mother = FormField.MOTHER.createParentObs(i++);
		obarr.put(mother);

		FormField.MOTHER_AGE.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_MARITAL_STATUS.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_EDUCATION.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_OCCUPATION.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_CITIZENSHIP.createConceptObs(obarr, fs, i++, mother);
		FormField.MOTHER_NIC.createConceptObs(obarr, fs, i++, mother);
		
		JSONObject motherAddressUsualResidence = FormField.MOTHER_ADDRESS_USUAL_RESIDENCE.createParentObs(i++);
		obarr.put(motherAddressUsualResidence);
		
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE_STREET.createConceptObs(obarr, fs, i++, motherAddressUsualResidence);
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE_PROVINCE.createConceptObs(obarr, fs, i++, motherAddressUsualResidence);
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE_DISTRICT.createConceptObs(obarr, fs, i++, motherAddressUsualResidence);
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE_TOWN.createConceptObs(obarr, fs, i++, motherAddressUsualResidence);
		FormField.MOTHER_ADDRESS_USUAL_RESIDENCE_UC.createConceptObs(obarr, fs, i++, motherAddressUsualResidence);
		
		//paternal grandfather data
		FormField.GRANDFATHER_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject grandpa = FormField.GRANDFATHER.createParentObs(i++);
		obarr.put(grandpa);

		FormField.GRANDFATHER_NIC.createConceptObs(obarr, fs, i++, grandpa);
		
		// pregnancy and delivery info
		FormField.BIRTH_TIME.createConceptObs(obarr, fs, i++, null);
		
		FormField.BIRTH_PLACE.createConceptObs(obarr, fs, i++, null);
		FormField.BIRTH_WEIGHT.createConceptObs(obarr, fs, i++, null);
		FormField.GRAVIDA.createConceptObs(obarr, fs, i++, null);
		FormField.PARITY.createConceptObs(obarr, fs, i++, null);
		FormField.GESTATIONAL_AGE.createConceptObs(obarr, fs, i++, null);
		FormField.BIRTH_TYPE.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_TYPE.createConceptObs(obarr, fs, i++, null);
		FormField.DELIVERY_ASSISTANT.createConceptObs(obarr, fs, i++, null);

		// other
		FormField.VACCINATED_ADEQUATELY.createConceptObs(obarr, fs, i++, null);

		// must get i so tht obs ids donot get repeated
		i = fillEncounterAndFormDetailsObs(fs, encounter, obarr, i);

		encounter.put("obs", obarr);
		
		return encounter;
	}
	
	private JSONObject createDeathEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.DEATH_FORM_TYPE.OMR_FIELD(), FormField.DEATH_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.DEATH_FORM_NAME.OMR_FIELD(), FormField.DEATH_FORM_NAME.DEFAULT_VALUE());

		int i = 1;
		JSONArray obarr = new JSONArray();

		// must get i so tht obs ids donot get repeated
		i = fillPersonDetailsObs(fs, obarr, i);

		// must get i so tht obs ids donot get repeated
		i = fillAddressUsualResidenceObs(fs, obarr, i);
		
		// applicant data		
		FormField.INFORMANT_FULL_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject applicant = FormField.INFORMANT_INFORMATION.createParentObs(i++);
		obarr.put(applicant);
		
		FormField.INFORMANT_NIC.createConceptObs(obarr, fs, i++, applicant);
		FormField.INFORMANT_RELATIONSHIP.createConceptObs(obarr, fs, i++, applicant);
		
		// father info 
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject father = FormField.FATHER.createParentObs(i++);
		obarr.put(father);
		
		FormField.FATHER_NIC.createConceptObs(obarr, fs, i++, father);

		// mother info
		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject mother = FormField.MOTHER.createParentObs(i++);
		obarr.put(mother);

		FormField.MOTHER_BIRTHDATE.createConceptObs(obarr, fs, i++, mother);
		
		//husband info
		FormField.HUSBAND_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject husband = FormField.HUSBAND.createParentObs(i++);
		obarr.put(husband);

		FormField.HUSBAND_NIC.createConceptObs(obarr, fs, i++, husband);
		
		// must get i so tht obs ids donot get repeated
		i = fillAddressDeathplaceObs(fs, obarr, i);

		FormField.DEATH_PLACE.createConceptObs(obarr, fs, i++, null);
		FormField.HEALTH_CARETAKER_NAME.createConceptObs(obarr, fs, i, null);
		FormField.BURIAL_DATE.createConceptObs(obarr, fs, i, null);
		FormField.GRAVEYARD_NAME.createConceptObs(obarr, fs, i, null);
		FormField.FINAL_ILLNESS_DURATION.createConceptObs(obarr, fs, i, null);
		FormField.DEATH_TYPE.createConceptObs(obarr, fs, i, null);
		FormField.DEATH_NATURE.createConceptObs(obarr, fs, i, null);
		FormField.CAUSE_OF_DEATH.createConceptObs(obarr, fs, i++, null);

		// relative info
		FormField.RELATIVE_NAME.createConceptObs(obarr, fs, i++, null);

		JSONObject relative = FormField.RELATIVE.createParentObs(i++);
		obarr.put(relative);

		FormField.NIC.createConceptObs(obarr, fs, i++, relative);
		FormField.RELATIVE_RELATIONSHIP.createConceptObs(obarr, fs, i++, relative);

		FormField.ADDITIONAL_NOTE.createConceptObs(obarr, fs, i++, null);
		
		// must get i so tht obs ids donot get repeated
		i = fillEncounterAndFormDetailsObs(fs, encounter, obarr, i);
		
		encounter.put("obs", obarr);
		
		return encounter;
	}
	
	

	private JSONObject createVerbalAutopsyEncounterAndObs(FormSubmission fs) throws JSONException{
		JSONObject encounter = new JSONObject();
		encounter.put(FormField.VERBAL_AUTOPSY_FORM_TYPE.OMR_FIELD(), FormField.VERBAL_AUTOPSY_FORM_TYPE.DEFAULT_VALUE());
		encounter.put(FormField.VERBAL_AUTOPSY_FORM_NAME.OMR_FIELD(), FormField.VERBAL_AUTOPSY_FORM_NAME.DEFAULT_VALUE());
		
		int i = 1;
		JSONArray obarr = new JSONArray();

		// must get i so tht obs ids donot get repeated
		i = fillPersonDetailsObs(fs, obarr, i);
		
		
		// must get i so tht obs ids donot get repeated
		i = fillAddressBirthplaceObs(fs, obarr, i);
		
		// must get i so tht obs ids donot get repeated
		i = fillAddressUsualResidenceObs(fs, obarr, i);

		// must get i so tht obs ids donot get repeated
		i = fillAddressPreviousResidenceObs(fs, obarr, i);

		// must get i so tht obs ids donot get repeated
		i = fillAddressDeathplaceObs(fs, obarr, i);

		FormField.MARRIAGE_DATE.createConceptObs(obarr, fs, i++, null);
		FormField.FATHER_NAME.createConceptObs(obarr, fs, i++, null);
		FormField.MOTHER_NAME.createConceptObs(obarr, fs, i++, null);

		FormField.CITIZENSHIP_TYPE.createConceptObs(obarr, fs, i++, null);

		FormField.DEATH_REGISTRATION_NUMBER.createConceptObs(obarr, fs, i++, null);
		FormField.DEATH_REGISTRATION_DATE.createConceptObs(obarr, fs, i++, null);

		// must get i so tht obs ids donot get repeated
		i = fillAddressDeathRegistrationObs(fs, obarr, i);

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

		FormField.VERBAL_AUTOPSY_RECORD_ID.createConceptObsWithValue(obarr, fs.entityId(), i++, null);

		// must get i so tht obs ids donot get repeated
		i = fillEncounterAndFormDetailsObs(fs, encounter, obarr, i);
		
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
	private String removeTrailingSlash(String str){
		return str.startsWith("/")?str.substring(1):str;
	}
}
