package org.ei.drishti.connector.listener;

import static org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.ei.drishti.connector.openmrs.OpenmrsService;
import org.ei.drishti.connector.schedule.OpenmrsSyncerScheduler;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.server.event.annotations.MotechListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsSyncerEventListener {
    private static Logger logger = LoggerFactory.getLogger(OpenmrsSyncerEventListener.class.toString());
    private OpenmrsService openmrsService;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final String previousFirtimeKey = "PRV_FIRE_TIME";

    @Autowired
    public OpenmrsSyncerEventListener(OpenmrsService openmrsService) {
		this.openmrsService = openmrsService;
	}

    @MotechListener(subjects = OpenmrsSyncerScheduler.SUBJECT)
    public void fetchReports(MotechEvent event) {
        if (!lock.tryLock()) {
            logger.warn("Not syncing locations with openmrs. It is already in progress.");
            return;
        }
        try {
            logger.info("Syncing locations with openmrs");

            String resp = openmrsService.syncOpenmrsLocations();
    		
			System.out.println(event.getParameters()+"::"+event.getEndTime());

    		Map<String, Object> map = event.getParameters();
			map.put(previousFirtimeKey, new Date());
			event.copy(OpenmrsSyncerScheduler.SUBJECT, map);
			
    		System.out.println(resp);

            logger.info("Recieved responses back from openrms : "+resp);
        } catch (Exception e) {
            logger.error(MessageFormat.format("{0} occurred while syncing locations with OpenMRS. Message: {1} with stack trace {2}",
                    e.toString(), e.getMessage(), getFullStackTrace(e)));
        } finally {
            lock.unlock();
        }
    }
}
