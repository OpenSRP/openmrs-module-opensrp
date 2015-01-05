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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.IllegalStateException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonProperty;
import org.ektorp.support.TypeDiscriminator;

@TypeDiscriminator("doc.type === 'OLocation'")
public class OLocation extends BaseMetadata{

	@JsonProperty
	private String locationId;
	
	@JsonProperty
	private String address1;
	
	@JsonProperty
	private String address2;
	
	@JsonProperty
	private String countyDistrict;

	@JsonProperty
	private String cityVillage;
	
	@JsonProperty
	private String stateProvince;
	
	@JsonProperty
	private String country;
	
	@JsonProperty
	private String postalCode;
	
	@JsonProperty
	private String latitude;
	
	@JsonProperty
	private String longitude;
	
    @JsonProperty
	private OLocation parentLocation;
	
    @JsonProperty
    private Set<OLocation> childLocations;
	
    @JsonProperty
    private Map<String, Object> locationAttributes;
    
    @JsonProperty
    private Set<String> tags;
	
    public OLocation() {
    }
    
    public String getLocationId() {
		return locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public Map<String, Object> getLocationAttributes() {
		return locationAttributes;
	}

	public void setLocationAttributes(Map<String, Object> locationAttributes) {
		this.locationAttributes = locationAttributes;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getCountyDistrict() {
		return countyDistrict;
	}

	public void setCountyDistrict(String countyDistrict) {
		this.countyDistrict = countyDistrict;
	}

	public String getCityVillage() {
		return cityVillage;
	}

	public void setCityVillage(String cityVillage) {
		this.cityVillage = cityVillage;
	}

	public String getStateProvince() {
		return stateProvince;
	}

	public void setStateProvince(String stateProvince) {
		this.stateProvince = stateProvince;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public OLocation getParentLocation() {
		return parentLocation;
	}

	public void setParentLocation(OLocation parentLocation) {
		this.parentLocation = parentLocation;
	}

	public Set<OLocation> getChildLocations() {
		return childLocations;
	}

	public void setChildLocations(Set<OLocation> childLocations) {
		this.childLocations = childLocations;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	@Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, new String[]{"location_id"});
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, new String[]{"location_id"});
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }


	/**
	 * Returns all childLocations where child.locationId = this.locationId.
	 * 
	 * @param includeRetired specifies whether or not to include voided childLocations
	 * @return Returns a Set<Location> of all the childLocations.
	 * @since 1.5
	 * @should return a set of locations
	 */
	public Set<OLocation> getChildLocations(boolean includeRetired) {
		Set<OLocation> ret = new HashSet<OLocation>();
		if (includeRetired)
			ret = getChildLocations();
		else if (getChildLocations() != null) {
			for (OLocation l : getChildLocations()) {
				if (!l.getRetired())
					ret.add(l);
			}
		}
		return ret;
	}
	
	/**
	 * @param child The child location to add.
	 * @throws IllegalStateException 
	 * @since 1.5
	 * @should return null given null parameter
	 */
	public void addChildLocation(OLocation child) throws IllegalStateException {
		if (child == null)
			return;
		
		if (getChildLocations() == null)
			childLocations = new HashSet<OLocation>();
		
		if (child.equals(this))
			throw new IllegalStateException("A location cannot be its own child!");
		
		// Traverse all the way up (down?) to the root, then check whether the child is already
		// anywhere in the tree
		OLocation root = this;
		while (root.getParentLocation() != null)
			root = root.getParentLocation();
		
		if (isInHierarchy(child, root))
			throw new IllegalStateException("Location hierarchy loop detected! You cannot add: '" + child + "' to the parent: '"
			        + this
			        + "' because it is in the parent hierarchy somewhere already and a location cannot be its own parent.");
		
		child.setParentLocation(this);
		childLocations.add(child);
	}
	
	/**
	 * Checks whether 'location' is a member of the tree starting at 'root'.
	 * 
	 * @param location The location to be tested.
	 * @param root Location node from which to start the testing (down in the hierarchy).
	 * @since 1.5
	 * @should return false given any null parameter
	 * @should return true given same object in both parameters
	 * @should return true given location that is already somewhere in hierarchy
	 * @should return false given location that is not in hierarchy
	 * @should should find location in hierarchy
	 */
	public static Boolean isInHierarchy(OLocation location, OLocation root) {
		if (root == null)
			return false;
		while (true) {
			if (location == null)
				return false;
			else if (root.equals(location))
				return true;
			location = location.getParentLocation();
		}
	}
	
	/**
	 * @param child The child location to remove.
	 * @since 1.5
	 */
	public void removeChildLocation(OLocation child) {
		if (getChildLocations() != null)
			childLocations.remove(child);
	}
	
	
	/**
	 * Attaches a tag to the Location.
	 * 
	 * @param tag The tag to add.
	 * @since 1.5
	 */
	public void addTag(String tag) {
		if (getTags() == null)
			tags = new HashSet<String>();
		if (tag != null && !tags.contains(tag))
			tags.add(tag);
	}
	
	/**
	 * Remove the tag from the Location.
	 * 
	 * @param tag The tag to remove.
	 * @since 1.5
	 */
	public void removeTag(String tag) {
		if (getTags() != null)
			tags.remove(tag);
	}
	
	/**
	 * Checks whether the Location has a particular tag.
	 * 
	 * @param tagToFind the string of the tag for which to check
	 * @return true if the tags include the specified tag, false otherwise
	 * @since 1.5
	 * @should not fail given null parameter
	 * @should return false given empty string parameter
	 */
	public Boolean hasTag(String tagToFind) {
		if (tagToFind != null && getTags() != null) {
			for (String locTag : getTags()) {
				if (locTag.equalsIgnoreCase(tagToFind)) {
					return true;
				}
			}
		}
		
		return false;
	}


    
}

