/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.ei.drishti.domain;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;
import org.motechproject.model.MotechBaseDataObject;

public abstract class BaseMetadata extends MotechBaseDataObject{
	
	private static final long serialVersionUID = 586370954841624962L;

	@JsonProperty
	protected String name;
	
	@JsonProperty
	protected String description;
	
	@JsonProperty
	protected String creator;
	
	@JsonProperty
	protected Date dateCreated;
	
	@JsonProperty
	protected String changedBy;
	
	@JsonProperty
	protected Date dateChanged;
	
	@JsonProperty
	protected Boolean retired = Boolean.FALSE;
	
	@JsonProperty
	protected Date dateRetired;
	
	@JsonProperty
	protected String retiredBy;
	
	@JsonProperty
	protected String retireReason;
	
	public BaseMetadata() {
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getCreator() {
		return creator;
	}
	
	public void setCreator(String creator) {
		this.creator = creator;
	}
	
	public Date getDateCreated() {
		return dateCreated;
	}
	
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	
	public String getChangedBy() {
		return changedBy;
	}
	
	public void setChangedBy(String changedBy) {
		this.changedBy = changedBy;
	}
	
	public Date getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(Date dateChanged) {
		this.dateChanged = dateChanged;
	}

	public Boolean getRetired() {
		return retired;
	}
	
	public void setRetired(Boolean retired) {
		this.retired = retired;
	}
	
	public Date getDateRetired() {
		return dateRetired;
	}
	
	public void setDateRetired(Date dateRetired) {
		this.dateRetired = dateRetired;
	}
	
	public String getRetiredBy() {
		return retiredBy;
	}
	
	public void setRetiredBy(String retiredBy) {
		this.retiredBy = retiredBy;
	}
	
	public String getRetireReason() {
		return retireReason;
	}
	
	public void setRetireReason(String retireReason) {
		this.retireReason = retireReason;
	}
}
