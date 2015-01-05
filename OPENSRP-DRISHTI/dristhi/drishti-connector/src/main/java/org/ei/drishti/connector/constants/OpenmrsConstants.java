package org.ei.drishti.connector.constants;

import org.ei.drishti.form.domain.FormSubmission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenmrsConstants {

	public static enum PersonField{
		IDENTIFIER("identifier", "entityId"),
		FIRST_NAME("person_name", "given_name"),
		LAST_NAME("family_name", "family_name"),
		GENDER("gender", "gender"),
		AGE("age", "age"),
		BIRTHDATE("birth_date", "birth_date"),
		BIRTHDATE_IS_APPROX("birthdate_estimated", null),
		DEATHDATE("death_date", "death_date"),
		DE_USER("creator", null),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		//DEATHDATE("death_date", "death_date"),
		;
		
		private String openmrsfieldName;
		private String drishtifieldName;
		public String OMR_FIELD(){
			return openmrsfieldName;
		}
		public String SRP_FIELD(){
			return drishtifieldName;
		}
		private PersonField(String openmrsfieldName, String drishtifieldName) {
			this.openmrsfieldName = openmrsfieldName;
			this.drishtifieldName = drishtifieldName;
		}
		
	}
	
	public static enum FormField {
		BIRTH_FORM_TYPE("encounter_type", null, "BIRTH NOTIFICATION"),
		BIRTH_FORM_ID("form_id", null, "4"),
		
		DEATH_FORM_TYPE("encounter_type", null, "DEATH NOTIFICATION"),
		DEATH_FORM_ID("form_id", null, "6"),
		
		PREGNANCY_NOTIFICATION_FORM_TYPE("encounter_type", null, "PREGNANCY NOTIFICATION"),
		PREGNANCY_NOTIFICATION_FORM_ID("form_id", null, "35"),

		VERBAL_AUTOPSY_FORM_TYPE("encounter_type", null, "VERBAL AUTOPSY"),
		VERBAL_AUTOPSY_FORM_ID("form_id", null, "3005"),//TODO FORMID
		
		LOCATION("location_id", "encounter_location", null),
		REGISTRATION_DATE("encounter_datetime", "today", null),

		ADDITIONAL_NOTE("161011","additional_note",null),
		
		START("","start",null),
		END("","end",null),
		TODAY("","today",null),
		DEVICEID("","deviceid",null),
		SUBSCRIBERID("","subscriberid",null),

		PERSON_FULL_NAME("163061", null, null),
		PERSON_IDENTIFIER("163121", "entityId", null),
		GENDER("163122", "gender", null),
		BIRTHDATE("163123", "birth_date", null),
		DEATHDATE("1543", "death_date", null),
		PERSON_AGE("age", "age", null),
		MARITAL_STATUS("162265", "marital_status", null),
		MARRIAGE_DATE("162275","marriage_date",null),
		CITIZENSHIP("162296", "citizenship", null),
		ETHNICITY("162294", "ethnicity", null),
		EDUCATION("1712", "education", null),
		OCCUPATION("162807", "occupation", null),
		ABILITY_READ_WRITE("162787","ability_read_write",null),
		ECONOMIC_ACTIVITY_STATUS("162851","economic_activity_status",null),

		ADDRESS_BIRTHPLACE("163098", "address_birthplace", null),
		ADDRESS_BIRTHPLACE_STREET("162318", "address_birthplace_street", null),
		ADDRESS_BIRTHPLACE_PROVINCE("162307", "address_birthplace_province", null),
		ADDRESS_BIRTHPLACE_DISTRICT("162319", "address_birthplace_district", null),
		ADDRESS_BIRTHPLACE_TOWN("162308", "address_birthplace_town", null),
		ADDRESS_BIRTHPLACE_UC("163036", "address_birthplace_uc", null),
		
		ADDRESS_DEATHPLACE("163099", "address_deathplace", null),
		ADDRESS_DEATHPLACE_STREET("162318", "address_deathplace_street", null),
		ADDRESS_DEATHPLACE_PROVINCE("162307", "address_deathplace_province", null),
		ADDRESS_DEATHPLACE_DISTRICT("162319", "address_deathplace_district", null),
		ADDRESS_DEATHPLACE_TOWN("162308", "address_deathplace_town", null),
		ADDRESS_DEATHPLACE_UC("163036", "address_deathplace_uc", null),
		
		ADDRESS_USUAL_RESIDENCE("163096", "address_usual_residence", null),
		ADDRESS_USUAL_RESIDENCE_STREET("162318", "address_usual_residence_street", null),
		ADDRESS_USUAL_RESIDENCE_PROVINCE("162307", "address_usual_residence_province", null),
		ADDRESS_USUAL_RESIDENCE_DISTRICT("162319", "address_usual_residence_district", null),
		ADDRESS_USUAL_RESIDENCE_TOWN("162308", "address_usual_residence_town", null),
		ADDRESS_USUAL_RESIDENCE_UC("163036", "address_usual_residence_uc", null),
		
		ADDRESS_PREVIOUS_RESIDENCE("163097", "address_previous_residence", null),
		ADDRESS_PREVIOUS_RESIDENCE_STREET("162318", "address_previous_residence_street", null),
		ADDRESS_PREVIOUS_RESIDENCE_PROVINCE("162307", "address_previous_residence_province", null),
		ADDRESS_PREVIOUS_RESIDENCE_DISTRICT("162319", "address_previous_residence_district", null),
		ADDRESS_PREVIOUS_RESIDENCE_TOWN("162308", "address_previous_residence_town", null),
		ADDRESS_PREVIOUS_RESIDENCE_UC("163036", "address_previous_residence_uc", null),

		ADDRESS_DEATH_REGISTRATION("162848","address_death_registration",null),
		ADDRESS_DEATH_REGISTRATION_STREET("162318","address_death_registration_street",null),
		ADDRESS_DEATH_REGISTRATION_PROVINCE("162307","address_death_registration_province",null),
		ADDRESS_DEATH_REGISTRATION_DISTRICT("162319","address_death_registration_district",null),
		ADDRESS_DEATH_REGISTRATION_TOWN("162308","address_death_registration_town",null),
		ADDRESS_DEATH_REGISTRATION_UC("163036","address_death_registration_uc",null),
		
		DEATH_REGISTRATION_NUMBER("162771","death_registration_number",null),
		DEATH_REGISTRATION_DATE("162766","death_registration_date",null),
		
		NIC("162849", "nic", null),

		AGE("160617", "woman_age", null),
		MOTHER("970", null, null),
		FATHER("971", null, null),
		MOTHER_NAME("163050", "mother_name", null),
		MOTHER_AGE("162270", "mother_age", null),
		MOTHER_MARITAL_STATUS("162265", "mother_marital_status", null),
		MOTHER_EDUCATION("1712", "mother_education", null),
		MOTHER_OCCUPATION("162807", "mother_occupation", null),
		MOTHER_CITIZENSHIP("162296", "mother_citizenship", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE("162312", "mother_address_usual_residence", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE_STREET("162318", "mother_address_usual_residence_street", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE_PROVINCE("162307", "mother_address_usual_residence_province", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE_DISTRICT("162319", "mother_address_usual_residence_district", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE_TOWN("162308", "mother_address_usual_residence_town", null),
		MOTHER_ADDRESS_USUAL_RESIDENCE_UC("163036", "mother_address_usual_residence_uc", null),
		
		FATHER_NAME("163051", "father_name", null),
		FATHER_AGE("162270", "father_age", null),
		FATHER_MARITAL_STATUS("162265", "father_marital_status", null),
		FATHER_EDUCATION("1712", "father_education", null),
		FATHER_OCCUPATION("162807", "father_occupation", null),
		FATHER_CITIZENSHIP("162296", "father_citizenship", null),
		FATHER_NIC("162849", "father_nic", null),
		FATHER_ADDRESS_USUAL_RESIDENCE("162314", "father_address_usual_residence", null),
		FATHER_ADDRESS_USUAL_RESIDENCE_STREET("162318", "father_address_usual_residence_street", null),
		FATHER_ADDRESS_USUAL_RESIDENCE_PROVINCE("162307", "father_address_usual_residence_province", null),
		FATHER_ADDRESS_USUAL_RESIDENCE_DISTRICT("162319", "father_address_usual_residence_district", null),
		FATHER_ADDRESS_USUAL_RESIDENCE_TOWN("162308", "father_address_usual_residence_town", null),
		FATHER_ADDRESS_USUAL_RESIDENCE_UC("163036", "father_address_usual_residence_uc", null),

		BIRTH_PLACE("1572", "birth_place", null),
		BIRTH_TIME("162279", "birth_time", null),
		BIRTH_WEIGHT("162206", "birth_weight", null),
		GRAVIDA("5624", "gravida", null),
		PARITY("1053", "parity", null),
		GESTATIONAL_AGE("1409", "gestational_age", null),
		BIRTH_TYPE("162210", "birth_type", null),
		DELIVERY_TYPE("5630", "delivery_type", null),
		DELIVERY_ASSISTANT("162214", "delivery_assistant", null),
		
		CAUSE_OF_DEATH("160218", "cause_of_death", null),
		DEATH_PLACE("1541", "death_place", null),

		ANC_REGISTRATION_DATE("163106", "anc_reg_date", null),
		ANC_REGISTRATION_LOCATION("163107", "health_facility_name", null),
		NEXT_OF_KIN("163109", "contact_person", null),
		ANC_NUMBER("1425", "anc_number", null),
		LMP("1427", "lmp_date", null),
		EDD("5596", "edd_formatted", null),
		PLANNED_FACILITY_OF_DELIVERY("163110", "planned_facility_delivery", null),
		PLANNED_TRANSPORT_FOR_DELIVERY("163111", "planned_transportation_delivery", null),
		ENROLLED_IN_HIV_CARE("159811", "enrolled_hiv_care", null),
		ENROLLED_DATE_HIV_CARE("160555", "hiv_enrolled_date", null),
		HIV_CARE_ART_NUMBER("163112", "hiv_art_number", null),
		CHRONIC_MEDICAL_CONDITIONS("163113", "medical_histories", null),
		BLOOD_GROUP("300", "blood_group", null),
		HEIGHT("5090", "height", null),
		BASELINE_WEIGHT("160696", "weight_start_pregnancy", null),

		///VERBAL AUTOPSY
		MATERNAL_DEATH("162834","maternal_death",null),
		
		RESPONDENT_NAME("162809","respondent_name",null),
		RESPONDENT_RELATIONSHIP("1530","respondent_relationship",null),
		RESPONDENT_LIVED_WITH_DECEASED("162847","respondent_lived_with_deceased",null),
		INTERVIEWER_NAME("162832","interviewer_name",null),
		INTERVIEW_TIME("1521","interview_time",null),
		INTERVIEW_DATE("162838","interview_date",null),
		
		TUBERCULOSIS("112141","tuberculosis",null),
		AIDS("1169","aids",null),
		MALARIA_POSITIVE("162749","malaria_positive",null),
		MALARIA_NEGATIVE("162764","malaria_negative",null),
		MEASLES("134561","measles",null),
		HYPERTENSION("162777","hypertension",null),
		HEART_DISEASE("139071","heart_disease",null),
		DIABETES("119481","diabetes",null),
		ASTHMA("121375","asthma",null),
		EPILEPSY("126814","epilepsy",null),
		CANCER("116030","cancer",null),
		COPD("162784","copd",null),
		DEMENTIA("119566","dementia",null),
		DEPRESSION("162763","depression",null),
		STROKE("162745","stroke",null),
		SICKLE_CELL("117703","sickle_cell",null),
		RENAL_DISEASE("6033","renal_disease",null),
		HEPATIC_DISEASE("6032","hepatic_disease",null),
		WET_SEASON_DEATH("162831","wet_season_death",null),
		DRY_SEASON_DEATH("162823","dry_season_death",null),
		FINAL_ILLNESS_DURATION("1735","final_illness_duration",null),
		SUDDEN_DEATH("125576","sudden_death",null),
		
		FEVER("140238","fever",null),
		FEVER_DURATION("162830","fever_duration",null),
		NIGHT_SWEATS("133027","night_sweats",null),
		COUGH("143264","cough",null),
		COUGH_DURATION("5959","cough_duration",null),
		COUGH_PRODUCTIVE("5957","cough_productive",null),
		HEMOPTYSIS("138905","hemoptysis",null),
		COUGH_WHOOPING("114190","cough_whooping",null),
		BREATH_PROBLEMS("122496","breath_problems",null),
		BREATH_FAST("125061","breath_fast",null),
		BREATH_FAST_DURATION("162760","breath_fast_duration",null),
		BREATHLESSNESS("136201","breathlessness",null),
		BREATHLESSNESS_DURATION("162794","breathlessness_duration",null),
		BREATHLESSNESS_PROBLEM_DAILY_ROUTINES("162788","breathlessness_problem_daily_routines",null),
		BREATHLESSNESS_LYING_FLAT("5961","breathlessness_lying_flat",null),
		BREATH_PULL_RIBS("162806","breath_pull_ribs",null),
		BREATH_NOISY("113316","breath_noisy",null),
		CHESTPAIN("154948","chestpain",null),
		DIARRHOEA("142412","diarrhoea",null),
		DIARRHOEA_DURATION("162776","diarrhoea_duration",null),
		HEMATOCHEZIA ("117671","hematochezia ",null),
		VOMIT("122983","vomit",null),
		VOMIT_COFFEE_GROUNDS("154068","vomit_coffee_grounds",null),
		ABDOMINAL_PROBLEM("142135","abdominal_problem",null),
		ABDOMINAL_PAIN("150917","abdominal_pain",null),
		ABDOMINAL_PAIN_DURATION("162842","abdominal_pain_duration",null),
		ABDOMEN_PROTRUDING("162796","abdomen_protruding",null),
		ABDOMEN_PROTRUDING_DURATION("162813","abdomen_protruding_duration",null),
		ABDOMINAL_MASS("5103","abdominal_mass",null),
		ABDOMINAL_MASS_DURATION("162753","abdominal_mass_duration",null),
		HEADACHE("139081","headache",null),
		NUCHAL_RIGIDITY("5170","nuchal_rigidity",null),
		NUCHAL_RIGIDITY_DURATION("162769","nuchal_rigidity_duration",null),
		CONFUSIONAL_STATE("144386","confusional_state",null),
		CONFUSIONAL_STATE_DURATION("162843","confusional_state_duration",null),
		UNCONSCIOUSNESS("162779","unconsciousness",null),
		UNCONSCIOUSNESS_SUDDEN("133528","unconsciousness_sudden",null),
		SEIZURE("206","seizure",null),
		SEIZURE_DURATION("159510","seizure_duration",null),
		SEIZURE_LEAD_UNCONSCIOUSNESS("141126","seizure_lead_unconsciousness",null),
		URINE("162837","urine",null),
		URINE_SUPPRESSED("125272","urine_suppressed",null),
		URINE_EXCESS("137593","urine_excess",null),
		HEMATURIA("840","hematuria",null),
		SKIN_PROBLEMS("119021","skin_problems",null),
		ULCER_BODY("162754","ulcer_body",null),
		ULCER_FEET("136505","ulcer_feet",null),
		SKIN_RASH("512","skin_rash",null),
		SKIN_RASH_DURATION("162748","skin_rash_duration",null),
		SKIN_RASH_MEASLES("162772","skin_rash_measles",null),
		HERPES_ZOSTER("117543","herpes_zoster",null),
		BLEED_MOUTH_NOSE_ANUS("162815","bleed_mouth_nose_anus",null),
		WEIGHT_LOSS("832","weight_loss",null),
		EMACIATED("141390","emaciated",null),
		MOUTHSORE("5334","mouthsore",null),
		BODY_STIFFNESS("162836","body_stiffness",null),
		FACIAL_SWELLING("140814","facial_swelling",null),
		FEET_SWELLING("125198","feet_swelling",null),
		LUMPS("125216","lumps",null),
		LUMPS_MOUTH("157667","lumps_mouth",null),
		LUMPS_NECK("152229","lumps_neck",null),
		LUMPS_ARMPIT("155221","lumps_armpit",null),
		LUMPS_GROIN("139247","lumps_groin",null),
		LUMPS_BREAST("146931","lumps_breast",null),
		HEMIPLEGIA ("117655","hemiplegia ",null),
		DYSPHAGIA("155939","dysphagia",null),
		DISCOLORATION_EYES("162762","discoloration_eyes",null),
		DISCOLORATION_HAIR("162814","discoloration_hair",null),
		PALLOR("5245","pallor",null),
		SUNKEN_EYES("118613","sunken_eyes",null),
		EXCESSIVE_WATER_INPUT("162827","excessive_water_input",null),
		MENSTRUAL_HEAVY_BLEED("136756","menstrual_heavy_bleed",null),
		MENOPAUSE("134346","menopause",null),
		POST_MENOPAUSE_BLEED("129371","post_menopause_bleed",null),
		//MATERNAL AND CHILD BOTH
		MULTIPLE_PREGNANCY("162833","multiple_pregnancy",null),
		PIH("47","pih",null),
		LOCHIA_OFFENSIVE("159846","lochia_offensive",null),
		MATERNAL_SEIZURE("162822","maternal_seizure",null),
		MATERNAL_BLURRED_VISION("129251","maternal_blurred_vision",null),
		MATERNAL_VAGINAL_BLEED_LAST_3M("162786","maternal_vaginal_bleed_last_3m",null),
		DELIVERY_HEALTH_FACILITY("1502","delivery_health_facility",null),
		DELIVERY_HOME("1501","delivery_home",null),
		DELIVERY_OTHER("162232","delivery_other",null),
		DELIVERY_ASSISTED_PROFESSIONALLY("160083","delivery_assisted_professionally",null),
		DELIVERY_NORMAL("1170","delivery_normal",null),
		DELIVERY_INSTRUMENTAL("118159","delivery_instrumental",null),
		DELIVERY_CAESAREAN("155884","delivery_caesarean",null),
		DELIVERY_BREECH("1172","delivery_breech",null),

		NOT_PREGNANT_DELIVERED_WITHIN_6W("162825","not_pregnant_delivered_within_6w",null),
		PREGNANT("1523","pregnant",null),
		DELIVERED_WITHIN_6W("162824","delivered_within_6w",null),
		PREGNANCY_TREMINATED_WITHIN_6W("162799","pregnancy_treminated_within_6w",null),
		GROUP_PREGNANCY_YES("","group_pregnancy_yes",null),
		DEATH_WITHIN_24HR_DELIVERY("162761","death_within_24hr_delivery",null),
		DEATH_DURING_LABOUR_UNDELIVERED("162804","death_during_labour_undelivered",null),
		BREASTFEEDING("162808","breastfeeding",null),
		HISTORY_CAESAREAN("156634","history_caesarean",null),
		DELIVERED_HEALTHY_BABY("162839","delivered_healthy_baby",null),
		MATERNAL_VAGINAL_BLEED_HEAVY("110548","maternal_vaginal_bleed_heavy",null),
		MATERNAL_VAGINAL_BLEED_FIRST_6M("162778","maternal_vaginal_bleed_first_6m",null),
		MATERNAL_VAGINAL_BLEED_HEAVY_LABOUR("162770","maternal_vaginal_bleed_heavy_labour",null),
		MATERNAL_VAGINAL_BLEED_HEAVY_POST_DELIVERY("162797","maternal_vaginal_bleed_heavy_post_delivery",null),
		RETAINED_PLACENTA("127592","retained_placenta",null),
		LONG_LABOUR("148473","long_labour",null),
		ABORTION_SELF_INDUCED("126802","abortion_self_induced",null),
		ABORTION("50","abortion",null),
		HYSTERECTOMY_BEFORE_DEATH("159837","hysterectomy_before_death",null),
		DELIVERY_PREMATURE("113841","delivery_premature",null),
		
		DELIVERY_COMPLICATED("120216","delivery_complicated",null),
		DELIVERY_LATE_24HRS_WATERBROKE("162791","delivery_late_24hrs_waterbroke",null),
		FETAL_MOVEMENT_CEASED_PRELABOUR("162835","fetal_movement_ceased_prelabour",null),
		BABY_ABNORMAL_SIZE("162795","baby_abnormal_size",null),
		BABY_UNDER_WEIGHT("1431","baby_under_weight",null),
		BABY_OVER_WEIGHT("162844","baby_over_weight",null),
		NUCHAL_CORD("162218","nuchal_cord",null),
		BABY_MALFORMATION("143849","baby_malformation",null),
		BABY_BACK_DEFECTED("143767","baby_back_defected",null),
		MACROCEPHALY ("135427","macrocephaly ",null),
		MICROCEPHALUS("134213","microcephalus",null),
		BABY_ABNORMAL_GROWTH("162219","baby_abnormal_growth",null),
		BLUEBABY("147112","bluebaby",null),
		CRIED_AFTER_BIRTH("1894","cried_after_birth",null),
		BREATHED_AFTER_BIRTH("1893","breathed_after_birth",null),
		BREATH_ASSISTED("162131","breath_assisted",null),
		STILLBIRTH("151124","stillbirth",null),
		STILLBIRTH_MACERATED("135436","stillbirth_macerated",null),
		BABY_ABLE_TO_SUCKLE_WITHIN_24HR("162220","baby_able_to_suckle_within_24hr",null),
		BABY_STOP_SUCKLE_AFTER_3D("162221","baby_stop_suckle_after_3d",null),
		BABY_SEIZURE_WITHIN_1D("162222","baby_seizure_within_1d",null),
		BABY_SEIZURE_AFTER_2D("162223","baby_seizure_after_2d",null),
		BABY_STIFF_BODY_ARCHED_BACK("143654","baby_stiff_body_arched_back",null),
		BULGING_FONTANELLE("1836","bulging_fontanelle",null),
		SUNKEN_FONTANELLE("162224","sunken_fontanelle",null),
		BABY_UNCONSCIOUS_WITHIN_24HR("162225","baby_unconscious_within_24hr",null),
		BABY_UNCONSCIOUS_AFTER_1D("162226","baby_unconscious_after_1d",null),
		COLD_BEFORE_DEATH("162227","cold_before_death",null),
		UMBILICAL_DISCHARGE("123843","umbilical_discharge",null),
		YELLOW_PALM_SOLE("115368","yellow_palm_sole",null),
		MOTHER_NOT_RECEIVED_TT("162228","mother_not_received_tt",null),
		
		ACCIDENTAL_DEATH("1603","accidental_death",null),
		ACCIDENT_ROAD_TRAFFIC("119964","accident_road_traffic",null),
		ACCIDENT_AS_PEDESTRIAN("162235","accident_as_pedestrian",null),
		ACCIDENT_AS_CAR_OCCUPANT("161574","accident_as_car_occupant",null),
		ACCIDENT_AS_HEAVY_VEHICLE_OCCUPANT("161575","accident_as_heavy_vehicle_occupant",null),
		ACCIDENT_AS_BIKE_OCCUPANT("157822","accident_as_bike_occupant",null),
		ACCIDENT_AS_CYCLIST("162236","accident_as_cyclist",null),
		ACCIDENT_COUNTERPART_KNOWN("162237","accident_counterpart_known",null),
		ACCIDENT_BY_PEDESTRIAN("162238","accident_by_pedestrian",null),
		ACCIDENT_BY_STATIONARY_OBJECT("162239","accident_by_stationary_object",null),
		ACCIDENT_BY_CAR("162240 ","accident_by_car",null),
		ACCIDENT_BY_HEAVY_VEHICLE("162241 ","accident_by_heavy_vehicle",null),
		ACCIDENT_BY_BIKE("162845","accident_by_bike",null),
		ACCIDENT_BY_CYCLE("162242","accident_by_cycle",null),
		ACCIDENT_BY_OTHER("162846","accident_by_other",null),
		ACCIDENT_NON_ROAD_TRAFFIC("162243","accident_non_road_traffic",null),
		INJURY_FALL("118350","injury_fall",null),
		INJURY_DROWNING("141768","injury_drowning",null),
		INJURY_BURNS("116543","injury_burns",null),
		INJURY_BITE("162878","injury_bite",null),
		INJURY_BITE_DOG("166","injury_bite_dog",null),
		INJURY_BITE_SNAKE("126323","injury_bite_snake",null),
		INJURY_BITE_INSECT("116758","injury_bite_insect",null),
		INJURY_NATURAL_DISASTER("162244","injury_natural_disaster",null),
		POISONING("114088","poisoning",null),
		ASSAULT("159286","assault",null),
		ASSAULT_INTENTIONAL("162245","assault_intentional",null),
		ASSAULT_INTENTIONAL_FIREARM("162246","assault_intentional_firearm",null),
		ASSAULT_INTENTIONAL_STAB_CUT("137099","assault_intentional_stab_cut",null),
		ASSAULT_INTENTIONAL_MACHINERY("162247","assault_intentional_machinery",null),
		ASSAULT_INTENTIONAL_ANIMAL("116918","assault_intentional_animal",null),
		SUICIDE("111685","suicide",null),
		
		ALCOHOL_DRINKER("143098","alcohol_drinker",null),
		SMOKER("152722","smoker",null),
		
		VACCINATED_ADEQUATELY("162248","vaccinated_adequately",null),
		TREATMENT_FINAL_ILLNESS_RECEIVED("1774","treatment_final_illness_received",null),
		TREATMENT_REHYDRATION_SALTS("351","treatment_rehydration_salts",null),
		TREATMENT_IV_FLUID("1765","treatment_iv_fluid",null),
		TREATMENT_BLOOD_TRANSFUSION("1063","treatment_blood_transfusion",null),
		TREATMENT_NASOGASTRIC_TUBE("1766","treatment_nasogastric_tube",null),
		TREATMENT_INJECTABLE_ANTIBIOTIC("162249","treatment_injectable_antibiotic",null),
		TREATMENT_OPERATION("1806","treatment_operation",null),
		TREATMENT_OPERATION_WITHIN_1M("162250","treatment_operation_within_1m",null),
		DISCHARGED_SERIOUSLY_ILL("162251","discharged_seriously_ill",null),
		
		FINALILLNESS_HEALTHFACILITY_TRAVELLED("162252","finalillness_healthfacility_travelled",null),
		FINALILLNESS_HEALTHFACILITY_TRAVELLED_MOTOR_VEHICLE("162253","finalillness_healthfacility_travelled_motor_vehicle",null),
		FINALILLNESS_HEALTHFACILITY_ADMISSION_PROBLEM("162254","finalillness_healthfacility_admission_problem",null),
		FINALILLNESS_HEALTHFACILITY_TREATMENT_PROBLEM("162255","finalillness_healthfacility_treatment_problem",null),
		FINALILLNESS_HEALTHFACILITY_MEDICATION_TEST_PROBLEM("162256","finalillness_healthfacility_medication_test_problem",null),
		FINALILLNESS_HEALTHFACILITY_TRAVEL_GT2HR("162257","finalillness_healthfacility_travel_gt2hr",null),
		FINALILLNESS_TREATMENT_NEED_DOUBT("162258","finalillness_treatment_need_doubt",null),
		FINALILLNESS_TREATMENT_TRADITIONAL_MEDICINE("162259","finalillness_treatment_traditional_medicine",null),
		FINALILLNESS_TREATMENT_HELP_CALLED_VIA_PHONE("162260","finalillness_treatment_help_called_via_phone",null),
		FINALILLNESS_TREATMENT_EXCEED_OFFORDABILITY("162261","finalillness_treatment_exceed_offordability",null),
		;
		
		private String openmrsfieldName;
		private String drishtifieldName;
		private Object defaultValue;
		public String OMR_FIELD(){
			return openmrsfieldName;
		}
		public String SRP_FIELD(){
			return drishtifieldName;
		}
		public Object DEFAULT_VALUE(){
			return defaultValue;
		}
		private FormField(String openmrsfieldName, String drishtifieldName, Object defaultValue) {
			this.openmrsfieldName = openmrsfieldName;
			this.drishtifieldName = drishtifieldName;
			this.defaultValue = defaultValue;
		}
		
		public void createConceptObs(JSONArray obsArray, Object value, int obsId, JSONObject parent) throws JSONException {
			if(value != null){
				JSONObject obs = new JSONObject();
				obs.put("obs_data_type", "concept");
				obs.put("concept_id", this.OMR_FIELD());
				obs.put("obs_id", obsId);
				if(parent != null) obs.put("obs_group_id", parent.get("obs_id"));
				
				obs.put("value", value);
				obsArray.put(obs);
			}
		}
		
		public void createConceptObs(JSONArray obsArray, FormSubmission fs, int obsId, JSONObject parent) throws JSONException {
			if(fs.getField(this.SRP_FIELD()) != null){
				JSONObject obs = new JSONObject();
				obs.put("obs_data_type", "concept");
				obs.put("concept_id", this.OMR_FIELD());
				obs.put("obs_id", obsId);
				if(parent != null) obs.put("obs_group_id", parent.get("obs_id"));
				
				obs.put("value", fs.getField(this.SRP_FIELD()));
				obsArray.put(obs);
			}
		}
		
		public void createConceptObsWithValue(JSONArray obsArray, Object value, int obsId, JSONObject parent) throws JSONException {
			if(value != null){
				JSONObject obs = new JSONObject();
				obs.put("obs_data_type", "concept");
				obs.put("concept_id", this.OMR_FIELD());
				obs.put("obs_id", obsId);
				if(parent != null) obs.put("obs_group_id", parent.get("obs_id"));
				
				obs.put("value", value);
				obsArray.put(obs);
			}
		}
		
		public JSONObject createParentObs(int obsId) throws JSONException {
			JSONObject obs = new JSONObject();
			obs.put("obs_data_type", "concept");
			obs.put("concept_id", this.OMR_FIELD());
			obs.put("obs_id", obsId);
			return obs;

		}
		
	}
	
	public static enum Location{
		LOCATION_LIST("results", null, null),
		PAGER_LIST("links", null, null),
		UUID("uuid", "uuid", null),
		NAME("display", "name", null),
		DESCRIPTION("description", "description", null),
		ADDRESS1("address1", "address1", null),
		ADDRESS2("address2", "address2", null),
		CITY_VILLAGE("cityVillage", "cityVillage", null),
		STATE_PROVINCE("stateProvince", "stateProvince", null),
		COUNTRY("country", "country", null),
		POSTAL_CODE("postalCode", "postalCode", null),
		LATITUDE("latitude", "latitude", null),
		LONGITUDE("longitude", "longitude", null),
		COUNTY_DISTRICT("countyDistrict", "countyDistrict", null),
		ADDRESS3("address3", "address3", null),
		ADDRESS4("address4", "address4", null),
		ADDRESS5("address5", "address5", null),
		ADDRESS6("address6", "address6", null),
		TAGS("tags", "tags", null),
		PARENT("parentLocation", "parentLocation", null),
		CHILDREN("childLocations", "childLocations", null),
		RETIRED("retired", "retired", null),
		ATTRIBUTES("attributes", "locationAttributes", null),
		;
		private String openmrsfieldName;
		private String drishtifieldName;
		private Object defaultValue;
		public String OMR_FIELD(){
			return openmrsfieldName;
		}
		public String SRP_FIELD(){
			return drishtifieldName;
		}
		public Object DEFAULT_VALUE(){
			return defaultValue;
		}
		private Location(String openmrsfieldName, String drishtifieldName, Object defaultValue) {
			this.openmrsfieldName = openmrsfieldName;
			this.drishtifieldName = drishtifieldName;
			this.defaultValue = defaultValue;
		}
		
		public static String getNextPageLink(JSONObject jso) throws JSONException{
			JSONArray linksarr = jso.getJSONArray(PAGER_LIST.OMR_FIELD());
			System.out.println(linksarr);
			for (int i = 0; i < linksarr.length(); i++) {
				if(linksarr.getJSONObject(i).has("rel") && linksarr.getJSONObject(i).getString("rel").equalsIgnoreCase("next")){
					return linksarr.getJSONObject(i).getString("uri");
				}
			}
			return null;
		}
		
		public static void getPrevPageLink(){
			
		}
	}
}
