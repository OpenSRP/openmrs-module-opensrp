package org.ei.drishti.repository;

import java.util.List;
import java.util.Map;

import org.ei.drishti.common.AllConstants;
import org.ei.drishti.domain.OLocation;
import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.support.GenerateView;
import org.ektorp.support.View;
import org.motechproject.dao.MotechBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class AllLocations extends MotechBaseRepository<OLocation> {
    private Map<String, String> designDocMap;

    @Autowired
    protected AllLocations(@Qualifier(AllConstants.DRISHTI_DATABASE_CONNECTOR) CouchDbConnector db) {
        super(OLocation.class, db);
    }

    public boolean exists(String locationId) {
        return findByLocationId(locationId) != null;
    }

    @GenerateView
    public OLocation findByLocationId(String locationId) {
        List<OLocation> locations = queryView("by_locationId", locationId);
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        return locations.get(0);
    }

    @View(name = "location_by_name", map = "function(doc) { if (doc.type === 'OLocation') { emit([doc.name], null); } }")
    public List<OLocation> findByName(String name) {
        return db.queryView(createQuery("location_by_name").key(name).includeDocs(true), OLocation.class);
    }

    @View(name = "location_by_matching_name", map = "function(doc) { if (doc.type === 'OLocation') { emit([doc.name], null); } }")
    public List<OLocation> findByMatchingName(String name) {
        return db.queryView(createQuery("location_by_matching_name").startKey(name).endKey(name+"Z").includeDocs(true), OLocation.class);
    }

    @View(name = "location_by_parent", map = "function(doc) { if (doc.type === 'OLocation') { emit([doc.parentLocation.locationId, doc.retired], null); } }")
    public List<OLocation> fetchByParent(String parentId, boolean includeRetired){
        ComplexKey key = ComplexKey.of(parentId, false);
        if(includeRetired){
        	key = ComplexKey.of(parentId);
        }
        return db.queryView(createQuery("location_by_parent").key(key).includeDocs(true), OLocation.class);
    }
    
    /**
     * Returns all location based on parameter provided. Any of the parameter can be excluded from the filter by providing 
     * null value, but you must specify anyone. Throws exception otherwise.  
     * @param parentId - Parent location`s UUID or integer id. Nullable only if tags are provided
     * @param tags - Location tags. Nullable only if parent location is provided
     * @param attibutes - Location attribute values to be filtered. Nullable only if parentId or tags is provided.
     * @param includeRetired - whether to include retired locations in resultset
     */
   /*@View(name = "location_by_tag", map = "function(doc) { if (doc.type === 'OLocation') { emit([doc.tags, doc.retired], null); } }")
    public List<OLocation> fetchBy(String[] tags, boolean includeRetired){
        ComplexKey startKey = ComplexKey.of(tags, includeRetired);
        ComplexKey endKey = ComplexKey.of(tags, includeRetired);
        return db.queryView(createQuery("location_by_tag").key(startKey).endKey(endKey).includeDocs(true), OLocation.class);
    }*/
}
