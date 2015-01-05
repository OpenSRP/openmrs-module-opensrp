package org.ei.drishti.service;

import java.util.List;

import org.ei.drishti.domain.OLocation;
import org.ei.drishti.repository.AllLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private static 	Logger logger = LoggerFactory.getLogger(LocationService.class.toString());
    private final AllLocations allLocations;

    @Autowired
    public LocationService(AllLocations allLocations) {
        this.allLocations = allLocations;
    }
 
    public OLocation getLocations(String locationId) {
    	return allLocations.findByLocationId(locationId);
	}
    
    public OLocation getLocationByName(String name) {
		List<OLocation> obj = allLocations.findByName(name);
		return obj.size()>0?obj.get(0):null;
	}
    public void saveLocation(OLocation location) {
        allLocations.add(location);;
    }
    
    public void updateLocation(OLocation location) {
        allLocations.update(location);;
    }
}
