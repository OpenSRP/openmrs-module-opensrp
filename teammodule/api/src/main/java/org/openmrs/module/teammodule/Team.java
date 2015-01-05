package org.openmrs.module.teammodule;

/*import javax.persistence.Access;
 import javax.persistence.AccessType;*/


import org.openmrs.BaseOpenmrsData;
import org.openmrs.Location;

/**
 * @author Muhammad Safwan
 * 
 */

 //@Access(AccessType.PROPERTY)
public class Team extends BaseOpenmrsData {

	private Integer teamId;

	private String teamIdentifier;

	private String teamName;
	
	private Location location;
	
	private Integer teamLeadId;
	
	private Integer memberCount;
	
	//private Boolean isRetired;

	public Team() {

	}

	//@Access(AccessType.PROPERTY)
	public Integer getTeamId() {
		return this.teamId;
	}

	public void setTeamId(Integer teamId) {
		this.teamId = teamId;
	}

	public String getTeamIdentifier() {
		return teamIdentifier;
	}

	public void setTeamIdentifier(String teamIdentifier) {
		this.teamIdentifier = teamIdentifier;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	/**
	 * @param teamId
	 */
	/*
	 * public Team(Integer teamId){ this.teamId = teamId; }
	 */

	public Integer getId() {
		return teamId;
	}

	public void setId(Integer id) {
		this.teamId = id;

	}

	public Integer getTeamLeadId() {
		return teamLeadId;
	}

	public void setTeamLeadId(Integer teamLeadId) {
		this.teamLeadId = teamLeadId;
	}

	public Integer getMemberCount() {
		return memberCount;
	}

	public void setMemberCount(Integer memberCount) {
		this.memberCount = memberCount;
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

/*	public Boolean getIsRetired() {
		return isRetired;
	}

	public void setIsRetired(Boolean isRetired) {
		this.isRetired = isRetired;
	}*/

}
