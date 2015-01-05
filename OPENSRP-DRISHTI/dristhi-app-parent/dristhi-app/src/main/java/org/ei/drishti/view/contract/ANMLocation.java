package org.ei.drishti.view.contract;

import java.util.List;

import com.google.gson.Gson;

public class ANMLocation {
    private String district;
    private String phcName;
    private String phcIdentifier;
    private String subCenter;
    private List<String> villages;

    public String asJSONString() {
        return new Gson().toJson(new ANMLocationJSONString(district, phcIdentifier, subCenter));
    }

    private class ANMLocationJSONString {
        private String district;
        private String phc;
        private String subCenter;

        private ANMLocationJSONString(String district, String phc, String subCenter) {
            this.district = district;
            this.phc = phc;
            this.subCenter = subCenter;
        }
    }

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getPhcName() {
		return phcName;
	}

	public void setPhcName(String phcName) {
		this.phcName = phcName;
	}

	public String getPhcIdentifier() {
		return phcIdentifier;
	}

	public void setPhcIdentifier(String phcIdentifier) {
		this.phcIdentifier = phcIdentifier;
	}

	public String getSubCenter() {
		return subCenter;
	}

	public void setSubCenter(String subCenter) {
		this.subCenter = subCenter;
	}

	public List<String> getVillages() {
		return villages;
	}

	public void setVillages(List<String> villages) {
		this.villages = villages;
	}
}
