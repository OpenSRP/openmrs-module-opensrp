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

import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;

/**
 * A LocationTag allows categorization of {@link OLocation}s
 */
@TypeDiscriminator("doc.type === 'LocationTag'")
public class LocationTag extends BaseMetadata {
	@JsonProperty
	private String tagId;
	
	public LocationTag() {
	}
	
	public LocationTag(String tagName) {
		setName(tagName);
	}
	
	public LocationTag(String tagId, String name, String description) {
		setTagId(tagId);
		setName(name);
		setDescription(description);
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}
}
