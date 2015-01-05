package org.ei.drishti.connector.schedule;

import org.joda.time.DateTime;
import org.motechproject.scheduler.MotechSchedulerService;
import org.motechproject.scheduler.domain.MotechEvent;
import org.motechproject.scheduler.domain.RepeatingSchedulableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;

import static org.joda.time.DateTimeConstants.MILLIS_PER_HOUR;
import static org.joda.time.DateTimeConstants.MILLIS_PER_MINUTE;

@Component
public class OpenmrsSyncerScheduler {
    public static final String SUBJECT = "OPENRMS-SYNCER-SCHEDULE";
    private static final int START_DELAY = 5;
    private final MotechSchedulerService schedulerService;
    private static Logger logger = LoggerFactory.getLogger(OpenmrsSyncerScheduler.class.toString());
    private long pollIntervalInHours;
    private long pollIntervalInMinutes;

    @Autowired
    public OpenmrsSyncerScheduler(MotechSchedulerService schedulerService,
                               @Value("#{drishti['omrssyncer.poll.time.interval.in.hours']}") Long pollIntervalInHours,
                               @Value("#{drishti['omrssyncer.poll.time.interval.in.minutes']}") Long pollIntervalInMinutes) {
        this.schedulerService = schedulerService;
        this.pollIntervalInHours = pollIntervalInHours;
        this.pollIntervalInMinutes = pollIntervalInMinutes;
    }

    public void startTimedScheduler() {
        logger.info("Scheduling SMS to fetch ...");

        Date startTime = DateTime.now().plusMinutes(START_DELAY).toDate();
        MotechEvent event = new MotechEvent(SUBJECT, new HashMap<String, Object>());
        long repeatIntervalInMilliSeconds = (pollIntervalInHours * MILLIS_PER_HOUR) + (pollIntervalInMinutes * MILLIS_PER_MINUTE);
        RepeatingSchedulableJob job = new RepeatingSchedulableJob(event, startTime, null, repeatIntervalInMilliSeconds);

        schedulerService.safeScheduleRepeatingJob(job);
    }
}
