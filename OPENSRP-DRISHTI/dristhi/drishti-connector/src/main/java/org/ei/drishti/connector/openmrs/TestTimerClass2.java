package org.ei.drishti.connector.openmrs;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.ei.drishti.domain.OLocation;
import org.ei.drishti.service.LocationService;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("pinggg-openmrs")
public class TestTimerClass2 {

    private OpenmrsService openmrsService;
    private LocationService locationService;
    private String formids;
    private static Date lastRuntime = null;

	@Autowired
	public TestTimerClass2(OpenmrsService openmrsService, LocationService locationService) {
		this.openmrsService = openmrsService;
		this.locationService = locationService;
	}
	 

	/*@RequestMapping(method = GET)
	@ResponseBody
	public List<String> runTimer(HttpServletRequest req, 
			@Value("#{drishti['openmrs.opensrp-connector.accepted-form-ids']}") String formsToPush) throws IOException, JSONException, ParseException {
		String[] formsToPushArr = formids.split(",");
		System.out.println("ARRRR:"+formsToPushArr);
		List<String> resp = new ArrayList<>();
		for (String fid : formsToPushArr) {
			try {
				long version = 0L;
				resp.add(openmrsService.pushDataToOpenmrs(fid, version));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println(resp);
		return resp;
	}*/

	@RequestMapping(method = GET)
	@ResponseBody
	public String runTimer() throws IOException, JSONException{
		return openmrsService.syncOpenmrsLocations();
		/*Resource r = new ClassPathResource("classpath:drishti.properties");
		System.out.println(r.getURI());
		Properties p = new Properties();
		p.load(r.getInputStream());

//    		/Properties props = AppConfig.env.getDefaultProfiles();
    		System.out.println(p);
    		//load a properties file from class path, inside static method
 
                //get the property value and print it out
                //System.out.println(props.getProperty("database"));
    	       // System.out.println(props.getProperty("dbuser"));
    	        //System.out.println(props.getProperty("dbpassword"));
 
    			return p.stringPropertyNames();*/

 
    }
	
}
