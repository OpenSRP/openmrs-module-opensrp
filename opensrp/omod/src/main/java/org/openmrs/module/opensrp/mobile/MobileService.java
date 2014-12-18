/**
 * This class incorporates Open MRS API services. The content type used is JSON for Requests and Responses
 * 
 */

package org.openmrs.module.opensrp.mobile;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.openmrs.module.mobile.shared.App;
import org.openmrs.module.mobile.shared.CustomMessage;
import org.openmrs.module.mobile.shared.ErrorType;
import org.openmrs.module.mobile.util.DateTimeUtil;
import org.openmrs.module.mobile.util.JsonUtil;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

//import org.irdresearch.codbrweb.shared.FormType;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author owais.hussain@irdresearch.org
 * 
 */

public class MobileService extends HttpServlet {
	/**
	 * 
	 */

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		PrintWriter out = resp.getWriter();
		String result = handleEvent(req, resp);
		System.out.println(result);
		out.println(result);
		out.flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		doGet(req, resp);
	}

	public String handleEvent(HttpServletRequest request,
			HttpServletResponse resp) {
		System.out.println("Posting to Server..");
		String response = "Response";
		try {
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
					throw new ContextAuthenticationException();
				}
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
				throw new JSONException(
						"JSON Content not found in the request.");

			JSONObject jsonObject = JsonUtil.getJSONObject(stringJson);
			String appVer = jsonObject.getString("app_ver");
			if (!appVer.equals(App.appVersion)) {
				return JsonUtil
						.getJsonError(
								CustomMessage
										.getErrorMessage(ErrorType.VERSION_MISMATCH_ERROR))
						.toString();
			}
			response = addNewPatientEncounter(jsonObject);

		} catch (NullPointerException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(
					CustomMessage
							.getErrorMessage(ErrorType.DATA_MISMATCH_ERROR)
							+ "\n" + e.getMessage()).toString();
		} catch (ContextAuthenticationException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(
					CustomMessage
							.getErrorMessage(ErrorType.AUTHENTICATION_ERROR)
							+ "\n" + e.getMessage()).toString();
		} catch (JSONException e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(
					CustomMessage.getErrorMessage(ErrorType.INVALID_DATA_ERROR)
							+ "\n" + e.getMessage()).toString();
		} catch (Exception e) {
			e.printStackTrace();
			response = JsonUtil.getJsonError(
					CustomMessage.getErrorMessage(ErrorType.UNKNOWN_ERROR)
							+ "\n" + e.getMessage()).toString();
		} finally {
			if (Context.isSessionOpen()) {
				Context.closeSession();
			}
		}
		return response;
	}

	@SuppressWarnings("deprecation")
	public String addNewPatientEncounter(JSONObject values) {
		JSONObject json = new JSONObject();
		String error = "";
		try {
			String username = values.getString("username").toString();

			JSONArray MainObj = (JSONArray) values.get("MainObj");
			if (MainObj.length() != 0) {
				for (int i = 0; i < MainObj.length(); i++) {
					JSONObject data = (JSONObject) MainObj.get(0);

					if (data.has("patientData")) {
						JSONObject patientData = (JSONObject) data
								.get("patientData");
						if (patientData.has("patient")) {
							JSONObject pObj = (JSONObject) patientData
									.get("patient");

							// Create Person object
							Person person = new Person();

							/*
							 * if(pObj.has("patient_id")) { int pPatientId =
							 * Integer
							 * .parseInt(pObj.get("patient_id").toString()); }
							 */

							String codbrIdentifier = "";
							if (pObj.has("codbr_identifier")) {
								codbrIdentifier = pObj.getString(
										"codbr_identifier").toString();
							}
							// String codbrIdentifier= "236";
							String givenName = "";
							if (pObj.has("given_name")) {
								givenName = pObj.getString("given_name")
										.toString();
							}
							String familyName = "Familyname";
							if (pObj.has("family_name")) {
								familyName = pObj.getString("family_name")
										.toString();
							}
							if (pObj.has("gender")) {
								String gender = pObj.getString("gender")
										.toString();
								person.setGender(gender);
							}
							if (pObj.has("birthdate")) {
								Date birthdate = DateTimeUtil
										.getDateFromString(
												pObj.getString("birthdate")
														.toString(),
												DateTimeUtil.SQL_DATE);
								person.setBirthdate(birthdate);
							}
							if (pObj.has("birthdate_estimated")) {
								boolean birthdateEstimated = pObj
										.getBoolean("birthdate_estimated");
								person.setBirthdateEstimated(birthdateEstimated);
							}
							if (pObj.has("dead")) {
								boolean dead = pObj.getBoolean("dead");
								person.setDead(dead);
							}
							// if (pObj.has("creator")) {
							int pCreator = Integer.parseInt(pObj.get("creator")
									.toString());
							User creatorObj = Context.getUserService().getUser(
									pCreator);
							person.setCreator(creatorObj);
							person.setDateCreated(new Date());
							// }
							// if(pObj.has("date_created")) {
							// Date pDateCreated =
							// DateTimeUtil.getDateFromString
							// (pObj.getString("date_created").toString(),
							// DateTimeUtil.SQL_DATETIME);
							// }

							if (pObj.has("death_date")) {
								Date deathDate = DateTimeUtil
										.getDateFromString(
												pObj.getString("death_date")
														.toString(),
												DateTimeUtil.SQL_DATE);
								person.setDeathDate(deathDate);
							}
							if (pObj.has("changed_by")) {
								int pChangedBy = Integer.parseInt(pObj.get(
										"changed_by").toString());
								User changerObj = Context.getUserService()
										.getUser(pChangedBy);
								person.setChangedBy(changerObj);
							}
							if (pObj.has("date_changed")) {
								Date pDateChanged = DateTimeUtil
										.getDateFromString(
												pObj.getString("date_changed")
														.toString(),
												DateTimeUtil.SQL_DATE);
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

							if (patientData.has("encounter")) {

								JSONArray encounter = (JSONArray) patientData
										.get("encounter");
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
								if (eObj.has("encounter_type")) {
									encounterType = "VERBAL AUTOSPY";
								}
								if (eObj.has("patient_id")) {
									int ePatientId = Integer.parseInt(eObj.get(
											"patient_id").toString());
								}
								// if(eObj.has("location_id")) {
								int locationId = Integer.parseInt(eObj.get(
										"location_id").toString());
								Location locationObj = Context
										.getLocationService().getLocation(
												locationId);
								// }
								// if(eObj.has("form_id")) {
								int formId = Integer.parseInt(eObj.get(
										"form_id").toString());
								// }
								// if(eObj.has("encounter_datetime")) {
								Date encounterDateTime = DateTimeUtil
										.getDateFromString(
												eObj.getString(
														"encounter_datetime")
														.toString(),
												DateTimeUtil.SQL_DATETIME);
								// }
								/*
								 * if(eObj.has("creator")) { int eCreator =
								 * Integer
								 * .parseInt(eObj.get("creator").toString()); }
								 */
								// if(eObj.has("date_created")) {
								// Date eDateCreated =
								// DateTimeUtil.getDateFromString
								// (eObj.getString("date_created").toString(),
								// DateTimeUtil.SQL_DATE);
								// }
								// Create Patient object
								Patient patientObj = new Patient(person);
								person = Context.getPersonService().getPerson(
										patientObj.getPersonId());
								// Create Patient identifier
								{
									SortedSet<PatientIdentifier> identifiers = new TreeSet<PatientIdentifier>();
									PatientIdentifier identifier = new PatientIdentifier();
									identifier.setIdentifier(codbrIdentifier);
									identifier
											.setIdentifierType(new PatientIdentifierType(
													3));
									identifier.setLocation(locationObj);
									identifier.setCreator(creatorObj);
									identifier.setDateCreated(new Date());
									// identifier.setPreferred (isPreferred);
									identifier.setPreferred(true);
									identifiers.add(identifier);
									patientObj.setIdentifiers(identifiers);
								}
								patientObj.setCreator(creatorObj);
								patientObj.setDateCreated(new Date());

								// Get Creator
								// User creatorObj = Context.getUserService
								// ().getUserByUsername (username);

								// Get Identifier type
								// List<PatientIdentifierType> allIdTypes =
								// Context.getPatientService
								// ().getAllPatientIdentifierTypes ();
								// PatientIdentifierType patientIdTypeObj =
								// allIdTypes.get (3);
								// Get Location
								// Location locationObj =
								// Context.getLocationService ().getLocation
								// (location);

								// Get Encounter type
								// EncounterType encounterTypeObj =
								// Context.getEncounterService
								// ().getEncounterType (encounterType);
								/*
								 * EncounterType encounterTypeObj = Context
								 * .getEncounterService() .getEncounterType(6);
								 */
								// Get Encounter type
								// EncounterType encounterTypeObj =
								// Context.getEncounterService
								// ().getEncounterType (encounterType);

								EncounterType encounterTypeObj = null;

								if (encounterType
										.equalsIgnoreCase("VERBAL AUTOSPY")) {
									encounterTypeObj = Context
											.getEncounterService()
											.getEncounterType(6);
								} else if (encounterType
										.equalsIgnoreCase("BIRTH_INFORMANT")) {
									encounterTypeObj = Context
											.getEncounterService()
											.getEncounterType(5);
								} else if (encounterType
										.equalsIgnoreCase("DEATH_INFORMANT_ANDROID")) {
									encounterTypeObj = Context
											.getEncounterService()
											.getEncounterType(9);
								}
								if (patientData.has("person_address")) {

									JSONArray person_address = (JSONArray) patientData
											.get("person_address");
									for (int j = 0; j < person_address.length(); j++) {
										JSONObject paObj = (JSONObject) person_address
												.get(j);
										// int personAddressId =
										// Integer.parseInt(paObj.get("person_address_id").toString());
										// int personId =
										// Integer.parseInt(paObj.get("person_id").toString());
										boolean isPreferred = paObj
												.getBoolean("preferred");
										PersonAddress personAddress = new PersonAddress();

										if (paObj.has("address1")) {
											String address1 = paObj.getString(
													"address1").toString();
											personAddress.setAddress1(address1);
										}
										if (paObj.has("address2")) {
											String address2 = paObj.getString(
													"address2").toString();
											personAddress.setAddress2(address2);
										}
										if (paObj.has("address3")) {
											String address3 = paObj.getString(
													"address3").toString();
											personAddress.setAddress3(address3);
										}
										if (paObj.has("address4")) {
											String address4 = paObj.getString(
													"address4").toString();
											personAddress.setAddress4(address4);
										}
										if (paObj.has("address5")) {
											String address5 = paObj.getString(
													"address5").toString();
											personAddress.setAddress5(address5);
										}
										if (paObj.has("address6")) {
											String address6 = paObj.getString(
													"address6").toString();
											personAddress.setAddress6(address6);
										}
										if (paObj.has("city_village")) {
											String cityVillage = paObj
													.getString("city_village")
													.toString();
											personAddress
													.setCityVillage(cityVillage);
										}
										if (paObj.has("state_province")) {
											String stateProvince = paObj
													.getString("state_province")
													.toString();
											personAddress
													.setStateProvince(stateProvince);
										}
										if (paObj.has("postal_code")) {
											String postalCode = paObj
													.getString("postal_code")
													.toString();
											personAddress
													.setPostalCode(postalCode);
										}
										if (paObj.has("country")) {
											String country = paObj.getString(
													"country").toString();
											personAddress.setCountry(country);
										}
										if (paObj.has("latitude")) {
											String latitude = paObj.getString(
													"latitude").toString();
											personAddress.setLatitude(latitude);
										}
										if (paObj.has("longitude")) {
											String longitude = paObj.getString(
													"longitude").toString();
											personAddress
													.setLongitude(longitude);
										}
										if (paObj.has("country_district")) {
											String countyDistrict = paObj
													.getString("county_district");
										}
										// int paCreator =
										// Integer.parseInt(paObj.get("creator").toString());
										// Date paDateCreated =
										// DateTimeUtil.getDateFromString
										// (paObj.getString("date_created").toString(),
										// DateTimeUtil.SQL_DATETIME);
										if (paObj.has("date_changed")) {
											Date paDateChanged = DateTimeUtil
													.getDateFromString(
															paObj.getString(
																	"date_changed")
																	.toString(),
															DateTimeUtil.SQL_DATE);
											personAddress
													.setDateChanged(paDateChanged);
										}
										if (paObj.has("changed_by")) {
											int paChangedBy = Integer
													.parseInt(paObj.get(
															"changed_by")
															.toString());
											User changerObj = Context
													.getUserService().getUser(
															paChangedBy);
											personAddress
													.setChangedBy(changerObj);
										}
										patientObj.addAddress(personAddress);
									}
								} // personaddress object check

								else {
									System.out
											.println("Person Address was missing");
								}

								patientObj = Context.getPatientService()
										.savePatient(patientObj); // error
								error = "Patient was created with Error. ";
								Encounter encounterObj = new Encounter();
								encounterObj.setEncounterType(encounterTypeObj);
								encounterObj.setPatient(patientObj);

								// In case of Encounter location different than
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
								Form formObj = Context.getFormService()
										.getForm(formId);
								encounterObj.setForm(formObj);
								encounterObj
										.setEncounterDatetime(encounterDateTime);
								encounterObj.setCreator(creatorObj);
								encounterObj.setDateCreated(new Date());
								if (eObj.has("changed_by")) {
									int eChangedBy = Integer.parseInt(eObj.get(
											"changed_by").toString());
									User changerObj = Context.getUserService()
											.getUser(eChangedBy);
									encounterObj.setChangedBy(changerObj);
								}
								if (eObj.has("date_changed")) {
									Date eDateChanged = DateTimeUtil
											.getDateFromString(
													eObj.getString(
															"date_changed")
															.toString(),
													DateTimeUtil.SQL_DATE);
									encounterObj.setDateChanged(eDateChanged);
								}

								// if (creatorObj.getUsername ().equals
								// (provider))
								encounterObj.setProvider(creatorObj);
								Context.getEncounterService().saveEncounter(
										encounterObj);

								// Create Observations set
								if (eObj.has("obs")) {
									// Integer secKey = null;

									HashMap<Integer, Obs> ObsSecMaintainer = new HashMap<Integer, Obs>();

									JSONArray obs = (JSONArray) eObj.get("obs");

									for (int j = 0; j < obs.length(); j++) {
										Obs ob = new Obs();
										// Create Person object

										Person personObj = Context
												.getPersonService()
												.getPerson(
														patientObj
																.getPatientId());
										ob.setPerson(personObj);

										// Create question/answer Concept object
										{
											JSONObject oObj = (JSONObject) obs
													.get(j);
											if (oObj.get("obs_data_type")
													.toString()
													.equalsIgnoreCase("concept")) {

												if (oObj.has("value")) {

													Concept concept = Context
															.getConceptService()
															.getConcept(
																	oObj.getInt("concept_id"));
													ob.setConcept(concept);
													String hl7Abbreviation = concept
															.getDatatype()
															.getHl7Abbreviation();

													if (hl7Abbreviation
															.equals("NM")) {
														ob.setValueNumeric(Double
																.parseDouble(oObj
																		.getString("value")));
													} else if (hl7Abbreviation
															.equals("CWE")) {
														Concept valueObj = Context
																.getConceptService()
																.getConcept(
																		oObj.getString("value"));
														ob.setValueCoded(valueObj);
													} else if (hl7Abbreviation
															.equals("ST")) {
														ob.setValueText(oObj
																.getString("value"));
													} else if (hl7Abbreviation
															.equals("DT")) {
														ob.setValueDatetime(DateTimeUtil
																.getDateFromString(
																		oObj.getString("value"),
																		DateTimeUtil.SQL_DATETIME));
													}
													// ob.setPerson(person);

													ob.setEncounter(encounterObj);
													ob.setObsDatetime(encounterDateTime);
													ob.setLocation(locationObj);
													ob.setCreator(creatorObj);
													ob.setDateCreated(new Date());
													ob.setEncounter(encounterObj);
													if (oObj.has("obs_group_id")) {
														Obs obsGrpObj = ObsSecMaintainer
																.get(oObj
																		.getInt("obs_group_id"));
														ob.setObsGroup(obsGrpObj);
													}
													// encounterObj.addObs (ob);

													Context.getObsService()
															.saveObs(ob, null);

												}

												else {

													Concept concept = Context
															.getConceptService()
															.getConcept(
																	oObj.getInt("concept_id"));
													ob.setConcept(concept);
													/*
													 * String hl7Abbreviation =
													 * concept.getDatatype
													 * ().getHl7Abbreviation ();
													 * 
													 * if
													 * (hl7Abbreviation.equals
													 * ("NM")) {
													 * ob.setValueNumeric
													 * (Double.parseDouble
													 * (oObj.getString
													 * ("value"))); } else if
													 * (hl7Abbreviation.equals
													 * ("CWE")) { Concept
													 * valueObj =
													 * Context.getConceptService
													 * ().getConcept
													 * (oObj.getString
													 * ("value"));
													 * ob.setValueCoded
													 * (valueObj); } else if
													 * (hl7Abbreviation.equals
													 * ("ST")) { ob.setValueText
													 * (oObj.getString
													 * ("value")); } else if
													 * (hl7Abbreviation.equals
													 * ("DT")) { ob.setValueDate
													 * (DateTimeUtil.
													 * getDateFromString
													 * (oObj.getString
													 * ("value"),
													 * DateTimeUtil.SQL_DATE));
													 * }
													 */

													ob.setEncounter(encounterObj);
													ob.setObsDatetime(encounterDateTime);
													ob.setLocation(locationObj);
													ob.setCreator(creatorObj);
													ob.setDateCreated(new Date());

													encounterObj.addObs(ob);

													Context.getEncounterService()
															.saveEncounter(
																	encounterObj);

													ObsSecMaintainer.put(oObj
															.getInt("obs_id"),
															ob);

												}
											} else if (oObj
													.get("obs_data_type")
													.toString()
													.equalsIgnoreCase(
															"person_attribute")) {
												PersonAttributeType personAttributeType;
												personAttributeType = Context
														.getPersonService()
														.getPersonAttributeTypeByName(
																oObj.getString("tbl_fld_name"));
												PersonAttribute attribute = new PersonAttribute();
												attribute
														.setAttributeType(personAttributeType);
												attribute.setValue(oObj
														.getString("value"));
												attribute
														.setCreator(creatorObj);
												attribute
														.setDateCreated(new Date());
												attribute.setPerson(personObj);
												personObj
														.addAttribute(attribute);
												Context.getPersonService()
														.savePerson(personObj);

												//

											}
										}
									} // for ends
								} // if obs ends

								/*
								 * patientObj = Context.getPatientService
								 * ().savePatient (patientObj); //error error =
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
			error += CustomMessage
					.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			error += CustomMessage
					.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} catch (JSONException e) {
			e.printStackTrace();
			error += CustomMessage
					.getErrorMessage(ErrorType.INVALID_DATA_ERROR);
		} catch (ParseException e) {
			e.printStackTrace();
			error += CustomMessage.getErrorMessage(ErrorType.PARSING_ERROR);
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
