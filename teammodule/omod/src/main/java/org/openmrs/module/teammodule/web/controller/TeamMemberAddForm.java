/**
 * 
 */
package org.openmrs.module.teammodule.web.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.module.teammodule.Team;
import org.openmrs.module.teammodule.TeamLead;
import org.openmrs.module.teammodule.TeamMember;
import org.openmrs.module.teammodule.api.TeamLeadService;
import org.openmrs.module.teammodule.api.TeamMemberService;
import org.openmrs.module.teammodule.api.TeamService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Muhammad Safwan
 * 
 */
@Controller
@RequestMapping(value = "/module/teammodule/teamMemberAddForm")
public class TeamMemberAddForm {
	protected final Log log = LogFactory.getLog(getClass());
	/** Success form view name */
	private final String SUCCESS_FORM_VIEW = "/module/teammodule/teamMemberAddForm";

	// private final String SUCCESS_REDIRECT_LINK =
	// "redirect:/module/teammodule/teamMemberForm/teamMemberForm.form";

	@ModelAttribute("memberData")
	public TeamMember populateTeamMember() {
		TeamMember memberData = new TeamMember();
		Person person = new Person();
		person.setPersonDateCreated(new Date());
		memberData.setPerson(person);
		memberData.getPerson().addName(new PersonName());
		
		return memberData;
	}

	/**
	 * Initially called after the formBackingObject method to get the landing
	 * form name
	 * 
	 * @return String form view name
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String showForm(// @ModelAttribute("existingMember") TeamMember
							// teamMember,
			Model model, HttpServletRequest request) {

		String error = request.getParameter("error");
		model.addAttribute("error", error);
		String saved = request.getParameter("saved");
		model.addAttribute("saved", saved);
		String teamId = request.getParameter("teamId");
		model.addAttribute("teamId", teamId);
		return SUCCESS_FORM_VIEW;

	}

	/*
	 * private Boolean isNewUser(User user) { return user == null ? true :
	 * user.getUserId() == null; }
	 */

	/**
	 * All the parameters are optional based on the necessity
	 * 
	 * @param httpSession
	 * @param anyRequestObject
	 * @param errors
	 * @return
	 */

	@RequestMapping(method = RequestMethod.POST)
	public String onSubmit(HttpSession httpSession, @ModelAttribute("anyRequestObject") Object anyRequestObject, BindingResult errors, HttpServletRequest request,
	/* @RequestParam(value = "teamParam", required = false) String teamId, */@ModelAttribute("memberData") TeamMember teamMember, @RequestParam(required = false, value = "userId") Integer userId,
			@RequestParam(required = false, value = "existingPerson") String existingPerson, Model model) {
		if (errors.hasErrors()) {
			// return error view
		}

		/*
		 * model.addAttribute("isNewUser", isNewUser(user)); if (isNewUser(user)
		 * || Context.hasPrivilege(PrivilegeConstants.EDIT_USER_PASSWORDS)) {
		 * model.addAttribute("modifyPasswords", true); }
		 */

		/*
		 * if (createNewPerson != null) { model.addAttribute("createNewPerson",
		 * createNewPerson); }
		 */

		/*
		 * if (!isNewUser(user)) { model.addAttribute("changePassword", new
		 * UserProperties
		 * (user.getUserProperties()).isSupposedToChangePassword()); }
		 */
		String tId = request.getParameter("teamId");
		String pId = request.getParameter("person_id");
		String error = "";

		TeamLead teamLead = new TeamLead();

		if(pId == "" || pId == null){
			Person person = Context.getPersonService().savePerson(teamMember.getPerson());
			teamMember.setPerson(person);
		}else{
			Person person = Context.getPersonService().getPerson(Integer.parseInt(pId));
			teamMember.setPerson(person);
		}

		/*if (existingPerson != null) {
			Person person = Context.getPersonService().getPerson(Integer.parseInt(pId));
			teamMember.setPerson(person); 
		}
			else {
				Person person = Context.getPersonService().savePerson(teamMember.getPerson());
				teamMember.setPerson(person);
			}*/
	
		
		/*if (teamMember.getPerson().getGivenName().isEmpty() || teamMember.getPerson().getFamilyName().isEmpty()) {
			error = "Name and Family Name can't be empty";
			model.addAttribute("error", error);
		}else{
			Person person = Context.getPersonService().savePerson(teamMember.getPerson());
			teamMember.setPerson(person);
		}*/
		
		
		Team team = Context.getService(TeamService.class).getTeam(Integer.parseInt(tId));
		teamMember.setTeam(team);

		if (teamMember.getJoinDate() == null) {
			teamMember.setJoinDate(new Date());
		}else{
			
		}
		if (teamMember.getPerson().getDateCreated() == null) {
			teamMember.getPerson().setDateCreated(new Date());
		}
		if (error.isEmpty()) {

			if (teamMember.getIsTeamLead().booleanValue()) {
				TeamLead tl = Context.getService(TeamLeadService.class).getTeamLead(team);
				if (tl == null) {
					Context.getService(TeamMemberService.class).save(teamMember);
					teamLead.setTeam(team);
					teamLead.setTeamMember(teamMember);
					if (teamMember.getJoinDate() == null) {
						teamLead.setJoinDate(new Date());
					}else{
						teamLead.setJoinDate(teamMember.getJoinDate());
					}
					Context.getService(TeamLeadService.class).save(teamLead);
				} else {
					error = "Team Lead for this team already exists. ";
					model.addAttribute("error", error);
				}
			}
		}
		model.addAttribute("teamId", tId);
		if (error.isEmpty()) {
			Context.getService(TeamMemberService.class).save(teamMember);
			String saved = "Member saved successfully";
			model.addAttribute("saved", saved);
		}

		return "redirect:/module/teammodule/teamMemberAddForm.form?teamId=" + tId;
	}

	// Made command Object (memberData)

}
