package org.openmrs.module.opensrp.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.hibernate.NonUniqueObjectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
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
import org.openmrs.module.IConstants;
import org.openmrs.module.mobile.shared.App;
import org.openmrs.module.mobile.shared.CustomMessage;
import org.openmrs.module.mobile.shared.ErrorType;
import org.openmrs.module.mobile.util.DateTimeUtil;
import org.openmrs.module.mobile.util.JsonUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;


@SuppressWarnings("serial")
public class OpenSRPNewPatientService extends HttpServlet implements IConstants {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		System.out.println("In OpenSRP");
		PrintWriter out = resp.getWriter();
		String result = handleEvent(req, resp);
		System.out.println(result);
		out.println(result);
		out.flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		doGet(req, resp);
	}

	public String handleEvent(HttpServletRequest request, HttpServletResponse resp) {
		System.out.println("Posting to Server..");
		String response = "Response";
		try {
			// Check if the login credentials are provided in request as plain
			// text
			String username = null;
			String password = null;
			try {
				/*
				 * username = request.getParameter("username"); password =
				 * request.getParameter("password");
				 */

				username = "admin";
				password = "Admin123";
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
				/*
				 * if (credentials.length == 2) { username = credentials[0];
				 * password = credentials[1]; } else { throw new
				 * ContextAuthenticationException(); }
				 */
			}
			// Open OpenMRS Context session
			// Context.openSession();
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
			// String json = request.getParameter ("content");
			if (stringJson == null)
				throw new JSONException("JSON Content not found in the request.");

			JSONObject jsonObject = JsonUtil.getJSONObject(stringJson);
			String appVer = jsonObject.getString("app_ver");
			if (!appVer.equals(App.appVersion)) {
				return JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.VERSION_MISMATCH_ERROR)).toString();
			}
			response = addNewPatientEncounter(jsonObject);

		} catch (NullPointerException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.DATA_MISMATCH_ERROR) + "\n" + e.getMessage()).toString();
		} catch (ContextAuthenticationException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.AUTHENTICATION_ERROR) + "\n" + e.getMessage()).toString();
		} catch (JSONException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.INVALID_DATA_ERROR) + "\n" + e.getMessage()).toString();
		} catch (Exception e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(CustomMessage.getErrorMessage(ErrorType.UNKNOWN_ERROR) + "\n" + e.getMessage()).toString();
		} finally {
			if (Context.isSessionOpen()) {
				Context.closeSession();
			}
		}
		return response;
	}

	@SuppressWarnings({ "deprecation", "null" })
	public String addNewPatientEncounter(JSONObject values) {
		JSONObject json = new JSONObject();
		String error = "";
		try {
			String username = values.getString("username").toString();

			JSONArray MainObj = (JSONArray) values.get("MainObj");
			if (MainObj.length() != 0) {
				for (int i = 0; i < MainObj.length(); i++) {
					JSONObject data = (JSONObject) MainObj.get(i);

					if (data.has("patientData")) {
						try {
							JSONObject patientData = (JSONObject) data.get("patientData");
							if (patientData.has("patient")) {
								JSONObject pObj = (JSONObject) patientData.get("patient");

								// Create Person object
								Person person = new Person();

								/*
								 * if(pObj.has("patient_id")) { int pPatientId =
								 * Integer
								 * .parseInt(pObj.get("patient_id").toString());
								 * }
								 */

								String codbrIdentifier = "";
								if (pObj.has(IDENTIFIER)) {
									codbrIdentifier = pObj.getString(IDENTIFIER).toString();
								}
								// String codbrIdentifier= "236";
								String givenName = "";
								if (pObj.has(PERSON_NAME)) {
									givenName = pObj.getString(PERSON_NAME).toString();
								}
								String familyName = "Familyname";
								if (pObj.has(FAMILY_NAME)) {
									familyName = pObj.getString(FAMILY_NAME).toString();
								}
								if (pObj.has(GENDER)) {
									String gender = pObj.getString(GENDER).toString();
									person.setGender(gender);
								}
								if (pObj.has(BIRTH_DATE)) {
									Date birthdate = DateTimeUtil.getDateFromString(pObj.getString(BIRTH_DATE).toString(), DateTimeUtil.SQL_DATE);
									person.setBirthdate(birthdate);
								}
								if (pObj.has(BIRTH_DATE_ESTIMATED)) {
									boolean birthdateEstimated = pObj.getBoolean(BIRTH_DATE_ESTIMATED);
									person.setBirthdateEstimated(birthdateEstimated);
								}
								if (pObj.has(DEAD)) {
									boolean dead = pObj.getBoolean(DEAD);
									person.setDead(dead);
								}
								// if (pObj.has("creator")) {
								int pCreator = Integer.parseInt(pObj.get(IConstants.CREATOR).toString());
								User creatorObj = Context.getUserService().getUser(pCreator);
								person.setCreator(creatorObj);
								person.setDateCreated(new Date());
								// }
								// if(pObj.has("date_created")) {
								// Date pDateCreated =
								// DateTimeUtil.getDateFromString
								// (pObj.getString("date_created").toString(),
								// DateTimeUtil.SQL_DATETIME);
								// }

								if (pObj.has(DEATH_DATE)) {
									Date deathDate = DateTimeUtil.getDateFromString(pObj.getString(DEATH_DATE).toString(), DateTimeUtil.SQL_DATE);
									person.setDeathDate(deathDate);
								}
								if (pObj.has(CHANGED_BY)) {
									int pChangedBy = Integer.parseInt(pObj.get(CHANGED_BY).toString());
									User changerObj = Context.getUserService().getUser(pChangedBy);
									person.setChangedBy(changerObj);
								}
								if (pObj.has(DATE_CHANGED)) {
									Date pDateChanged = DateTimeUtil.getDateFromString(pObj.getString(DATE_CHANGED).toString(), DateTimeUtil.SQL_DATE);
									person.setDateChanged(pDateChanged);
								}

								// Create names set

								SortedSet<PersonName> names = new TreeSet<PersonName>();
								// PersonName name = new PersonName (givenName,
								// null, familyName);
								PersonName name = new PersonName();
								name.setGivenName(givenName);
								name.setMiddleName(null);
								name.setFamilyName(familyName);
								name.setCreator(creatorObj);
								name.setDateCreated(new Date());
								// name.setPreferred (isPreferred);
								name.setPreferred(true);
								names.add(name);
								person.setNames(names);

								if (patientData.has(ENCOUNTER)) {

									JSONArray encounter = (JSONArray) patientData.get(ENCOUNTER);
									JSONObject eObj = (JSONObject) encounter.get(0);
									/*
									 * if(eObj.has("encounter_id")) { int
									 * encounterId =
									 * Integer.parseInt(eObj.get("encounter_id"
									 * ).toString()); }
									 */
									// String encounterType =
									// eObj.getString("encounter_type").toString();
									String encounterType = null;
									if (eObj.has(ENCOUNTER_TYPE)) { // commented
																	// this now
										encounterType = eObj.get(ENCOUNTER_TYPE).toString();
										// encounterType = "VERBAL AUTOPSY";
									}
									if (eObj.has(PERSON_ID)) {
										int ePatientId = Integer.parseInt(eObj.get(PERSON_ID).toString()); // PERSON_ID
																											// is
																											// PATIENT_ID
									}
									// if(eObj.has("location_id")) {
									int locationId = Integer.parseInt(eObj.get(LOCATION_ID).toString());
									Location locationObj = Context.getLocationService().getLocation(locationId);
									// }
									// if(eObj.has("form_id")) {
									int formId = Integer.parseInt(eObj.get(FORM_ID).toString());
									// }
									// if(eObj.has("encounter_datetime")) {
									Date encounterDateTime = DateTimeUtil.getDateFromString(eObj.getString(ENCOUNTER_DATE_TIME).toString(), DateTimeUtil.SQL_DATETIME);
									// }
									/*
									 * if(eObj.has("creator")) { int eCreator =
									 * Integer
									 * .parseInt(eObj.get("creator").toString
									 * ()); }
									 */
									// if(eObj.has("date_created")) {
									// Date eDateCreated =
									// DateTimeUtil.getDateFromString
									// (eObj.getString("date_created").toString(),
									// DateTimeUtil.SQL_DATE);
									// }
									// Create Patient object
									Patient patientObj = new Patient(person);
									person = Context.getPersonService().getPerson(patientObj.getPersonId());
									// Create Patient identifier
									{
										SortedSet<PatientIdentifier> identifiers = new TreeSet<PatientIdentifier>();
										PatientIdentifier identifier = new PatientIdentifier();
										identifier.setIdentifier(codbrIdentifier);
										identifier.setIdentifierType(new PatientIdentifierType(3));
										identifier.setLocation(locationObj);
										identifier.setCreator(creatorObj);
										identifier.setDateCreated(new Date());
										// identifier.setPreferred
										// (isPreferred);
										identifier.setPreferred(true);
										identifiers.add(identifier);
										patientObj.setIdentifiers(identifiers);
									}
									patientObj.setCreator(creatorObj);
									patientObj.setDateCreated(new Date());

		

									EncounterType encounterTypeObj = null;

									encounterTypeObj = Context.getEncounterService().getEncounterType(encounterType);


									 
									if (patientData.has(PERSON_ADDRESS)) {

										JSONArray person_address = (JSONArray) patientData.get("person_address");
										for (int j = 0; j < person_address.length(); j++) {
											JSONObject paObj = (JSONObject) person_address.get(j);
											// int personAddressId =
											// Integer.parseInt(paObj.get("person_address_id").toString());
											// int personId =
											// Integer.parseInt(paObj.get("person_id").toString());
											boolean isPreferred = paObj.getBoolean(PREFERRED);
											PersonAddress personAddress = new PersonAddress();

											if (paObj.has(ADDRESS1)) {
												String address1 = paObj.getString(ADDRESS1).toString();
												personAddress.setAddress1(address1);
											}
											if (paObj.has(ADDRESS2)) {
												String address2 = paObj.getString(ADDRESS2).toString();
												personAddress.setAddress2(address2);
											}
											if (paObj.has(ADDRESS3)) {
												String address3 = paObj.getString(ADDRESS3).toString();
												personAddress.setAddress3(address3);
											}
											if (paObj.has(ADDRESS4)) {
												String address4 = paObj.getString(ADDRESS4).toString();
												personAddress.setAddress4(address4);
											}
											if (paObj.has(ADDRESS5)) {
												String address5 = paObj.getString(ADDRESS5).toString();
												personAddress.setAddress5(address5);
											}
											if (paObj.has(ADDRESS6)) {
												String address6 = paObj.getString(ADDRESS6).toString();
												personAddress.setAddress6(address6);
											}
											if (paObj.has(CITY_VILLAGE)) {
												String cityVillage = paObj.getString(CITY_VILLAGE).toString();
												personAddress.setCityVillage(cityVillage);
											}
											if (paObj.has(STATE_PROVINCE)) {
												String stateProvince = paObj.getString(STATE_PROVINCE).toString();
												personAddress.setStateProvince(stateProvince);
											}
											if (paObj.has(POSTAL_CODE)) {
												String postalCode = paObj.getString(POSTAL_CODE).toString();
												personAddress.setPostalCode(postalCode);
											}
											if (paObj.has(COUNTRY)) {
												String country = paObj.getString(COUNTRY).toString();
												personAddress.setCountry(country);
											}
											if (paObj.has(LATITUDE)) {
												String latitude = paObj.getString(LATITUDE).toString();
												personAddress.setLatitude(latitude);
											}
											if (paObj.has(LONGITUDE)) {
												String longitude = paObj.getString(LONGITUDE).toString();
												personAddress.setLongitude(longitude);
											}
											if (paObj.has(COUNTY_DISTRICT)) {
												String countyDistrict = paObj.getString(COUNTY_DISTRICT);
											}
											// int paCreator =
											// Integer.parseInt(paObj.get("creator").toString());
											// Date paDateCreated =
											// DateTimeUtil.getDateFromString
											// (paObj.getString("date_created").toString(),
											// DateTimeUtil.SQL_DATETIME);
											if (paObj.has(DATE_CHANGED)) {
												Date paDateChanged = DateTimeUtil.getDateFromString(paObj.getString(DATE_CHANGED).toString(), DateTimeUtil.SQL_DATE);
												personAddress.setDateChanged(paDateChanged);
											}
											if (paObj.has(CHANGED_BY)) {
												int paChangedBy = Integer.parseInt(paObj.get(CHANGED_BY).toString());
												User changerObj = Context.getUserService().getUser(paChangedBy);
												personAddress.setChangedBy(changerObj);
											}
											patientObj.addAddress(personAddress);
										}
									} // personaddress object check

									else {
										System.out.println("Person Address was missing");
									}

									patientObj = Context.getPatientService().savePatient(patientObj); // error
									error = "Patient was created with Error. ";
									Encounter encounterObj = new Encounter();
									encounterObj.setEncounterType(encounterTypeObj);
									encounterObj.setPatient(patientObj);

									// In case of Encounter location different
									// than
									// login location
									// Assumption, the location remains same
									// entirely during 1 entry
									/*
									 * if (!encounterLocation.equalsIgnoreCase
									 * (location)) { locationObj =
									 * Context.getLocationService ().getLocation
									 * (encounterLocation); }
									 */
									encounterObj.setLocation(locationObj);
									Form formObj = Context.getFormService().getForm(formId);
									encounterObj.setForm(formObj);
									encounterObj.setEncounterDatetime(encounterDateTime);
									encounterObj.setCreator(creatorObj);
									encounterObj.setDateCreated(new Date());
									if (eObj.has(CHANGED_BY)) {
										int eChangedBy = Integer.parseInt(eObj.get(CHANGED_BY).toString());
										User changerObj = Context.getUserService().getUser(eChangedBy);
										encounterObj.setChangedBy(changerObj);
									}
									if (eObj.has(DATE_CHANGED)) {
										Date eDateChanged = DateTimeUtil.getDateFromString(eObj.getString(DATE_CHANGED).toString(), DateTimeUtil.SQL_DATE);
										encounterObj.setDateChanged(eDateChanged);
									}

									// if (creatorObj.getUsername ().equals
									// (provider))
									encounterObj.setProvider(creatorObj);
									Context.getEncounterService().saveEncounter(encounterObj);

									// Create Observations set
									if (eObj.has(OBS)) {
										// Integer secKey = null;

										HashMap<Integer, Obs> ObsSecMaintainer = new HashMap<Integer, Obs>();

										JSONArray obs = (JSONArray) eObj.get(OBS);

										for (int j = 0; j < obs.length(); j++) {
											Obs ob = new Obs();
											// Create Person object

											Person personObj = Context.getPersonService().getPerson(patientObj.getPatientId());
											ob.setPerson(personObj);

											// Create question/answer Concept
											// object
											{
												JSONObject oObj = (JSONObject) obs.get(j);
												if (oObj.get(OBS_DATA_TYPE).toString().equalsIgnoreCase(CONCEPT)) {

													if (oObj.has(VALUE)) {

														Concept concept = Context.getConceptService().getConcept(oObj.getInt(CONCEPT_ID));
														ob.setConcept(concept);
														String hl7Abbreviation = concept.getDatatype().getHl7Abbreviation();

														if (hl7Abbreviation.equals("NM")) {
															ob.setValueNumeric(Double.parseDouble(oObj.getString(VALUE)));
														} else if (hl7Abbreviation.equals("CWE")) {
															Concept valueObj = Context.getConceptService().getConcept(oObj.getString(VALUE));
															ob.setValueCoded(valueObj);
														} else if (hl7Abbreviation.equals("ST")) {
															ob.setValueText(oObj.getString(VALUE));
														} else if (hl7Abbreviation.equals("DT")) {
															ob.setValueDatetime(DateTimeUtil.getDateFromString(oObj.getString(VALUE), DateTimeUtil.SQL_DATE));
														}
														// ob.setPerson(person);

														ob.setEncounter(encounterObj);
														ob.setObsDatetime(encounterDateTime);
														ob.setLocation(locationObj);
														ob.setCreator(creatorObj);
														ob.setDateCreated(new Date());
														ob.setEncounter(encounterObj);
														if (oObj.has(OBS_GROUP_ID)) {
															Obs obsGrpObj = ObsSecMaintainer.get(oObj.getInt(OBS_GROUP_ID));
															ob.setObsGroup(obsGrpObj);
														}
														// encounterObj.addObs
														// (ob);

														Context.getObsService().saveObs(ob, null);

													}

													else {

														Concept concept = Context.getConceptService().getConcept(oObj.getInt(CONCEPT_ID));
														ob.setConcept(concept);
														

														ob.setEncounter(encounterObj);
														ob.setObsDatetime(encounterDateTime);
														ob.setLocation(locationObj);
														ob.setCreator(creatorObj);
														ob.setDateCreated(new Date());

														encounterObj.addObs(ob);

														Context.getEncounterService().saveEncounter(encounterObj);

														ObsSecMaintainer.put(oObj.getInt(OBS_ID), ob);

													}
												} else if (oObj.get(OBS_DATA_TYPE).toString().equalsIgnoreCase("person_attribute")) {
													PersonAttributeType personAttributeType;
													personAttributeType = Context.getPersonService().getPersonAttributeTypeByName(oObj.getString("tbl_fld_name"));
													PersonAttribute attribute = new PersonAttribute();
													attribute.setAttributeType(personAttributeType);
													attribute.setValue(oObj.getString(VALUE));
													attribute.setCreator(creatorObj);
													attribute.setDateCreated(new Date());
													attribute.setPerson(personObj);
													personObj.addAttribute(attribute);
													Context.getPersonService().savePerson(personObj);

													//

												}
											}
										} // for ends
									} // if obs ends

									/*
									 * patientObj = Context.getPatientService
									 * ().savePatient (patientObj); //error
									 * error =
									 * "Patient was created with Error. ";
									 */

									// encounterObj.setEncounterType
									// (encounterTypeObj);
									// encounterObj.setPatient (patientObj);

									json.put("result", "SUCCESS");

								}// if encounter obj exists
								else {
									System.out.println("encounter was missing");
								}
							} // if patient obj exists
							else {
								System.out.println("patient obj was missing");
							}
						} catch (Exception e) {
							e.printStackTrace();
							error += ErrorType.UNKNOWN_ERROR + ";" +e.getMessage();
						}
					} // if patientdata exists condition
				}// for loop Main Obj
			} // if MainObj Condition
			else {
				json.put("result", "Empty JSON");
			}
		}

		catch (NonUniqueObjectException e) {
			e.printStackTrace();
			error = CustomMessage.getErrorMessage(ErrorType.DUPLICATION_ERROR);
		} catch (NullPointerException e) {
			e.printStackTrace();
			error += CustomMessage.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			error += CustomMessage.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} catch (JSONException e) {
			e.printStackTrace();
			error += CustomMessage.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} finally {
			try {
				if (!json.has("result")) {
					json.put("result", "FAIL. " + error);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return json.toString();
	}

}
