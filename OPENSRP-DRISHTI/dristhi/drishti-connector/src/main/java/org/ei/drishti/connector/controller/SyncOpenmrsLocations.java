package org.ei.drishti.connector.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.IOException;

import org.ei.drishti.connector.openmrs.OpenmrsService;
import org.ei.drishti.service.LocationService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("sync-openmrs-locations")
public class SyncOpenmrsLocations {

    private OpenmrsService openmrsService;
    private LocationService locationService;

	@Autowired
	public SyncOpenmrsLocations(OpenmrsService openmrsService, LocationService locationService) {
		this.openmrsService = openmrsService;
		this.locationService = locationService;
	}
	 
	@RequestMapping(method = GET)
	@ResponseBody
	public String runTimer() throws IOException, JSONException{
		return openmrsService.syncOpenmrsLocations();
    }
	
}
