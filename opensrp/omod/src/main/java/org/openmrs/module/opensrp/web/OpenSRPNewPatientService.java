/**
 * This class incorporates Open MRS API services. The content type used is JSON for Requests and Responses
 * 
 */

package org.openmrs.module.opensrp.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.LocationAttributeType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.mobile.shared.ErrorType;
import org.openmrs.module.mobile.util.DateTimeUtil;
import org.openmrs.module.mobile.util.JsonUtil;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * @author owais.hussain@irdresearch.org
 * 
 */

public class OpenSRPNewPatientService extends HttpServlet {

	public static void main(String[] args) {
		String message = "Possible duplicate ${1} person IDs ${0}";
		String literal = "\\$\\{\\d{1,2}\\}";
		System.out.println(message.matches(".*("+literal+")+.*"));
		List<List<Object>> resultSet = new ArrayList<List<Object>>();
		ArrayList inlist = new ArrayList();
		inlist.add("[0]");
		inlist.add("[1]");
		inlist.add("[2]");
		inlist.add("[3]");
		inlist.add("[4]");
		resultSet.add(inlist);
		if(!resultSet.isEmpty()){// empty resultset means no one matched the criteria
			Matcher m = Pattern.compile(literal).matcher(message);
			while (!m.hitEnd() && m.find()) {// replace each instance until end
				String replaceString = m.group();
				try{
					//get index between the brackets ${indexNumber}
					int index = Integer.parseInt(replaceString.replace("${", "").replace("}", ""));
					if(index < resultSet.get(0).size()){// do nothing if index is invalid
						message = message.replace(replaceString, resultSet.get(0).get(index).toString());
					}
				}
				catch(Exception e){
					//do nothing. text would remain unreplaced
				}
				System.out.println(m.group());
				System.out.println(message);
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		String response = null;
		try {
			response = handleEvent(req, resp);
		} catch (Exception e1) {
			e1.printStackTrace();
			
			JSONObject json = new JSONObject();
			json.put("ERROR", ErrorType.UNKNOWN_ERROR.name());
			json.put("ERROR_MESSAGE", ExceptionUtil.getStackTrace(e1));
			
			response = json.toString();
		}
		resp.getWriter().write(response);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

	public String handleEvent(HttpServletRequest request, HttpServletResponse resp) throws Exception {
		System.out.println("Posting to Server..");
		
		// Check if the login credentials are provided in request as plain
		// text
		String username = null;
		String password = null;
		try {
			username = request.getParameter("username");
			password = request.getParameter("password");
			if (username == null || password == null) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException e) {
			// Read the credentials from encrypted Authorization header
			String header = request.getHeader("Authorization");
			byte[] authBytes = Base64.decode(header);
			String authData = new String(authBytes, "UTF-8");
			// Username and password MUST be separated using colon
			String[] credentials = authData.split(":");
			if (credentials.length == 2) {
				username = credentials[0];
				password = credentials[1];
			} else {
				throw new ContextAuthenticationException("Valid credentials must be provided");
			}
		}
		// Authenticate using Username and Password in the parameters
		Context.authenticate(username, password);

		String stringJson = "";
		// FileItem file = null;
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		// boolean isJsonRequest = true;

		if (isMultipart) {
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory();

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Parse the request
			List /* FileItem */items = upload.parseRequest(request);

			Iterator iter = items.iterator();
			while (iter.hasNext()) {
				FileItem item = (FileItem) iter.next();

				if (item.isFormField()) {
					if (item.getFieldName().equalsIgnoreCase("jsoncontent")) {
						stringJson = item.getString();
					}
				} else {
					// isJsonRequest = true;
					stringJson = item.getFieldName();
					// file = item;
				}
			}
		} else {
			byte[] b = new byte[request.getContentLength()];
			request.getInputStream().read(b);
			stringJson = new String(b);
		}

		// Read JSON from the Request
		if (stringJson == null)
			throw new JSONException("JSON Content not found in the request.");

		JSONObject jsonObject = JsonUtil.getJSONObject(stringJson);
		/*String appVer = jsonObject.getString("app_ver");
		if (!appVer.equals(App.appVersion)) {
			return JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.VERSION_MISMATCH_ERROR)).toString();
		}*/
					
		return addNewPatientEncounter(jsonObject);
	}

	@SuppressWarnings("deprecation")
	public String addNewPatientEncounter(JSONObject values) throws Exception {
		JSONObject json = new JSONObject();

		JSONArray MainObj = (JSONArray) values.get("MainObj");
		if (MainObj.length() != 0) {
			for (int i = 0; i < MainObj.length(); i++) {
				JSONObject data = (JSONObject) MainObj.get(i);
				
				String idStr = null;

				if (data.has("patientData")) {
					JSONObject patientData = (JSONObject) data.get("patientData");

					JSONObject pObj = (JSONObject) patientData.get("patient");

					// the encounter provider
					User deo = Context.getUserService().getUserByUsername(pObj.getString("creator").toString());
					// the daemon user i.e. everything is created automatically by a daemon thread
					User daemon = Context.getUserService().getUserByUsername("daemon");

					//for now only ; update it
					JSONArray encounter = (JSONArray) patientData.get("encounter");
					JSONObject eObj = (JSONObject) encounter.get(0);
					
					String locId = eObj.get("location_id").toString();
					Map<LocationAttributeType, Object> attr = new HashMap<LocationAttributeType, Object>();
					attr.put(getLocationAttributeType("Code"), locId);
					
					List<Location> locl = Context.getLocationService().getLocations(null, null, attr , true, 0, 2);
					if(locl.size() > 1){
						throw new IllegalStateException("Multiple locations with same CODE ID : " +locl);
					}

					Location loc = locl.get(0);
					
					// Create Person object
					Patient patientObj = null;
					idStr = pObj.getString("identifier").toString();

					if(pObj.getBoolean("is_new_person")){
						Person person = new Person();

						String givenName = pObj.getString("given_name").toString();
						String familyName = ".";
						if (pObj.has("family_name")) {
							familyName = pObj.getString("family_name").toString();
						}
						
						person.setGender(pObj.getString("gender").toString());
						Date birthdate = DateTimeUtil.getDateFromString(pObj.getString("birth_date").toString(), DateTimeUtil.SQL_DATE);
						person.setBirthdate(birthdate);
						if (pObj.has("birthdate_estimated")) {
							boolean birthdateEstimated = pObj.getBoolean("birthdate_estimated");
							person.setBirthdateEstimated(birthdateEstimated);
						}
						if (pObj.has("dead")) {
							person.setDead(pObj.getBoolean("dead"));
						}
						person.setCreator(daemon);
						person.setDateCreated(new Date());
	
						if (pObj.has("death_date")) {
							Date deathDate = DateTimeUtil.getDateFromString(pObj.getString("death_date").toString(),DateTimeUtil.SQL_DATE);
							person.setDeathDate(deathDate);
						}
						if (pObj.has("changed_by")) {
							String pChangedBy = pObj.get("changed_by").toString();
							User changerObj = Context.getUserService().getUserByUsername(pChangedBy);
							person.setChangedBy(changerObj);
						}
						if (pObj.has("date_changed")) {
							Date pDateChanged = DateTimeUtil.getDateFromString(pObj.getString("date_changed").toString(), DateTimeUtil.SQL_DATE);
							person.setDateChanged(pDateChanged);
						}
	
						// Create names set
	
						SortedSet<PersonName> names = new TreeSet<PersonName>();
						PersonName name = new PersonName();
						name.setGivenName(givenName);
						name.setMiddleName(null);
						name.setFamilyName(familyName);
						name.setCreator(daemon);
						name.setDateCreated(new Date());
						name.setPreferred(true);
						names.add(name);
						person.setNames(names);
	
						// Create Patient object
						patientObj = new Patient(person);
						person = Context.getPersonService().getPerson(patientObj.getPersonId());
						
						// Create Patient identifier
						SortedSet<PatientIdentifier> identifiers = new TreeSet<PatientIdentifier>();
						PatientIdentifier identifier = new PatientIdentifier();
						identifier.setIdentifier(idStr);
						identifier.setIdentifierType(getOrCreatePatientIdentifierType(pObj.getString("identifier_type")));
						identifier.setLocation(loc);
						identifier.setCreator(daemon);
						identifier.setDateCreated(new Date());
						identifier.setPreferred(true);
						identifiers.add(identifier);
						patientObj.setIdentifiers(identifiers);
						
						patientObj.setCreator(daemon);
						patientObj.setDateCreated(new Date());
	
						if (patientData.has("person_address")) {
	
							JSONArray person_address = (JSONArray) patientData.get("person_address");
							for (int j = 0; j < person_address.length(); j++) {
								JSONObject paObj = (JSONObject) person_address.get(j);
								boolean isPreferred = paObj.getBoolean("preferred");
								PersonAddress personAddress = new PersonAddress();
	
								if (paObj.has("address1")) {
									String address1 = paObj.getString("address1").toString();
									personAddress.setAddress1(address1);
								}
								if (paObj.has("address2")) {
									String address2 = paObj.getString("address2").toString();
									personAddress.setAddress2(address2);
								}
								if (paObj.has("address3")) {
									String address3 = paObj.getString("address3").toString();
									personAddress.setAddress3(address3);
								}
								if (paObj.has("address4")) {
									String address4 = paObj.getString("address4").toString();
									personAddress.setAddress4(address4);
								}
								if (paObj.has("address5")) {
									String address5 = paObj.getString("address5").toString();
									personAddress.setAddress5(address5);
								}
								if (paObj.has("address6")) {
									String address6 = paObj.getString("address6").toString();
									personAddress.setAddress6(address6);
								}
								if (paObj.has("city_village")) {
									String cityVillage = paObj.getString("city_village").toString();
									personAddress.setCityVillage(cityVillage);
								}
								if (paObj.has("state_province")) {
									String stateProvince = paObj.getString("state_province").toString();
									personAddress.setStateProvince(stateProvince);
								}
								if (paObj.has("postal_code")) {
									String postalCode = paObj.getString("postal_code").toString();
									personAddress.setPostalCode(postalCode);
								}
								if (paObj.has("country")) {
									String country = paObj.getString("country").toString();
									personAddress.setCountry(country);
								}
								if (paObj.has("latitude")) {
									String latitude = paObj.getString("latitude").toString();
									personAddress.setLatitude(latitude);
								}
								if (paObj.has("longitude")) {
									String longitude = paObj.getString("longitude").toString();
									personAddress.setLongitude(longitude);
								}
								if (paObj.has("country_district")) {
									String countyDistrict = paObj.getString("county_district");
								}
								if (paObj.has("date_changed")) {
									Date paDateChanged = DateTimeUtil.getDateFromString(
													paObj.getString("date_changed").toString(), DateTimeUtil.SQL_DATE);
									personAddress.setDateChanged(paDateChanged);
								}
								if (paObj.has("changed_by")) {
									int paChangedBy = Integer.parseInt(paObj.get("changed_by").toString());
									User changerObj = Context.getUserService().getUser(paChangedBy);
									personAddress.setChangedBy(changerObj);
								}
								personAddress.setCreator(daemon);
								
								patientObj.addAddress(personAddress);
							}
						} // personaddress object check
						else {
							System.out.println("Person Address was missing");
						}
	
						patientObj = Context.getPatientService().savePatient(patientObj); // error
					}//person was existing i.e. NOT new person
					else if(!pObj.getBoolean("is_new_person")){
						List<Patient> pats = Context.getPatientService().getPatients(idStr);
						if(pats.size() == 0){
							throw new Exception("NO PATIENT FOUND FOR GIVEN ID");
						}
						else if(pats.size() > 1){
							throw new Exception("MULTIPLE Patients return in given ID "+idStr);
						}
						
						// This must return a unique patient
						patientObj = Context.getPatientService().getPatients(idStr).get(0);
					}
					
					Form formObj = null;
					if(eObj.has("form_id")){
						formObj = Context.getFormService().getForm(Integer.parseInt(eObj.get("form_id").toString()));
					}
					else if(eObj.has("form_name")){
						formObj = Context.getFormService().getForm(eObj.getString("form_name"));
					}

					Date encounterDateTime = DateTimeUtil.getDateFromString(eObj.getString("encounter_datetime").toString(), DateTimeUtil.SQL_DATETIME);

					EncounterType encounterTypeObj = Context.getEncounterService().getEncounterType(eObj.getString("encounter_type"));

					Encounter encounterObj = new Encounter();
					encounterObj.setEncounterType(encounterTypeObj);
					encounterObj.setPatient(patientObj);
					encounterObj.setLocation(loc);
					encounterObj.setForm(formObj);
					encounterObj.setEncounterDatetime(encounterDateTime);
					encounterObj.setCreator(daemon);
					encounterObj.setDateCreated(new Date());
					if (eObj.has("changed_by")) {
						int eChangedBy = Integer.parseInt(eObj.get("changed_by").toString());
						User changerObj = Context.getUserService().getUser(eChangedBy);
						encounterObj.setChangedBy(changerObj);
					}
					if (eObj.has("date_changed")) {
						Date eDateChanged = DateTimeUtil.getDateFromString(
										eObj.getString("date_changed").toString(), DateTimeUtil.SQL_DATE);
						encounterObj.setDateChanged(eDateChanged);
					}

					encounterObj.setProvider(deo);
					Context.getEncounterService().saveEncounter(encounterObj);

					// Create Observations set
					HashMap<Integer, Obs> ObsSecMaintainer = new HashMap<Integer, Obs>();

					JSONArray obs = (JSONArray) eObj.get("obs");

					for (int j = 0; j < obs.length(); j++) {
						Obs ob = new Obs();
						Person personObj = Context.getPersonService().getPerson(patientObj.getPatientId());
						ob.setPerson(personObj);

						// Create question/answer Concept object
						JSONObject oObj = (JSONObject) obs.get(j);
						if (oObj.get("obs_data_type").toString().equalsIgnoreCase("concept")) {

							if (oObj.has("value")) {

								Concept concept = Context.getConceptService().getConcept(oObj.getInt("concept_id"));
								ob.setConcept(concept);
								String hl7Abbreviation = concept.getDatatype().getHl7Abbreviation();

								if (hl7Abbreviation.equals("NM")) {
									ob.setValueNumeric(Double.parseDouble(oObj.getString("value")));
								} 
								else if (hl7Abbreviation.equals("CWE")) {
									Concept valueObj = Context.getConceptService().getConcept(oObj.getString("value"));
									ob.setValueCoded(valueObj);
								} 
								else if (hl7Abbreviation.equals("ST")) {
									ob.setValueText(oObj.getString("value"));
								} 
								else if (hl7Abbreviation.equals("DT")) {
									ob.setValueDatetime(DateTimeUtil.getDateFromString(oObj.getString("value"), DateTimeUtil.SQL_DATE));
								}
								else if (hl7Abbreviation.equals("TS")) {
									ob.setValueDatetime(DateTimeUtil.getDateFromString(oObj.getString("value"), DateTimeUtil.SQL_DATETIME));
								}
								
								ob.setEncounter(encounterObj);
								ob.setObsDatetime(encounterDateTime);
								ob.setLocation(loc);
								ob.setCreator(daemon);
								ob.setDateCreated(new Date());
								ob.setEncounter(encounterObj);
								if (oObj.has("obs_group_id")) {
									Obs obsGrpObj = ObsSecMaintainer.get(oObj.getInt("obs_group_id"));
									ob.setObsGroup(obsGrpObj);
								}

								Context.getObsService().saveObs(ob, null);
							}
							else {
								Concept concept = Context.getConceptService().getConcept(oObj.getInt("concept_id"));
								ob.setConcept(concept);
								ob.setEncounter(encounterObj);
								ob.setObsDatetime(encounterDateTime);
								ob.setLocation(loc);
								ob.setCreator(daemon);
								ob.setDateCreated(new Date());

								encounterObj.addObs(ob);

								Context.getEncounterService().saveEncounter(encounterObj);

								ObsSecMaintainer.put(oObj.getInt("obs_id"),ob);

							}
						} else if (oObj.get("obs_data_type").toString().equalsIgnoreCase("person_attribute")) {
							PersonAttributeType personAttributeType;
							personAttributeType = Context.getPersonService().getPersonAttributeTypeByName(oObj.getString("tbl_fld_name"));
							PersonAttribute attribute = new PersonAttribute();
							attribute.setAttributeType(personAttributeType);
							attribute.setValue(oObj.getString("value"));
							attribute.setCreator(daemon);
							attribute.setDateCreated(new Date());
							attribute.setPerson(personObj);
							personObj.addAttribute(attribute);
							Context.getPersonService().savePerson(personObj);
						}
					} // for ends

					json.put(idStr, "SUCCESS");

				}// if patientdata exists condition
				else {
					throw new IllegalArgumentException("A patientData object must be specified in mainObj");
				}
				
			}// for loop Main Obj
		} // if MainObj Condition
		else {
			json.put("result", "Empty JSON");
		}
		
		return json.toString();
	}
	
	/*public static void main(String[] args) throws ModuleMustStartException, DatabaseUpdateException, InputRequiredException {
		Properties props = OpenmrsUtil.getRuntimeProperties("openmrs");
		
		boolean usetest = false;
		
		if (usetest) {
			props.put("connection.username", "root");
			props.put("connection.password", "codbr");
			Context.startup("jdbc:mysql://125.209.94.150:2103/openmrs?autoReconnect=true", "root", "codbr", props);
		} else {
			props.put("connection.username", "root");
			props.put("connection.password", "VA1913wm");
			Context.startup("jdbc:mysql://localhost:3306/openmrs?autoReconnect=true", "root", "VA1913wm", props);
		}
		
		try {
			Context.openSession();
			Context.authenticate("admin", "Admin321");
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Context.closeSession();
		}
	}*/

	
	private PatientIdentifierType getOrCreatePatientIdentifierType(String identifierType){
		PatientIdentifierType idtype = Context.getPatientService().getPatientIdentifierTypeByName(identifierType);
		if(idtype == null){
			idtype = new PatientIdentifierType();
			idtype.setName(identifierType);
			idtype.setDateCreated(new Date());
			idtype.setDescription("CREATED BY SYSTEM BY OPENSRP MODULE");
			idtype.setCreator(Context.getUserService().getUserByUsername("daemon"));
			idtype = Context.getPatientService().savePatientIdentifierType(idtype);
		}
		return idtype;
	}
	private LocationAttributeType getLocationAttributeType(String attributeName){
    	for (LocationAttributeType att : Context.getLocationService().getAllLocationAttributeTypes()) {
			if(att.getName().equalsIgnoreCase(attributeName)){
				return att;
			}
		}

    	return null;
    }
}
