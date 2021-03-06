package gov.healthit.chpl.manager.impl;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;
import static org.quartz.impl.matchers.GroupMatcher.groupEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gov.healthit.chpl.auth.Util;
import gov.healthit.chpl.auth.permission.GrantedPermission;
import gov.healthit.chpl.domain.schedule.ChplJob;
import gov.healthit.chpl.domain.schedule.ChplTrigger;
import gov.healthit.chpl.dto.CertificationBodyDTO;
import gov.healthit.chpl.exception.ValidationException;
import gov.healthit.chpl.manager.CertificationBodyManager;
import gov.healthit.chpl.manager.SchedulerManager;
import gov.healthit.chpl.scheduler.ChplSchedulerReference;

/**
 * Implementation of Scheduler Manager.
 * @author alarned
 *
 */
@Service
public class SchedulerManagerImpl implements SchedulerManager {

    private static final String AUTHORITY_DELIMITER = ";";

    @Autowired
    private ChplSchedulerReference chplScheduler;

    @Autowired
    private CertificationBodyManager acbManager;

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ONC', 'ROLE_ACB')")
    public ChplTrigger createTrigger(final ChplTrigger trigger) throws SchedulerException, ValidationException {
        Scheduler scheduler = getScheduler();

        TriggerKey triggerId = triggerKey(createTriggerName(trigger), createTriggerGroup(trigger));
        JobKey jobId = jobKey(trigger.getJob().getName(), trigger.getJob().getGroup());
        if (doesUserHavePermissionToJob(scheduler.getJobDetail(jobId))) {
            Trigger qzTrigger = null;
            if (trigger.getJob().getJobDataMap().getBooleanValue("acbSpecific")) {
                qzTrigger = newTrigger()
                        .withIdentity(triggerId)
                        .startNow()
                        .forJob(jobId)
                        .usingJobData("email", trigger.getEmail())
                        .usingJobData("acb", trigger.getAcb())
                        .withSchedule(cronSchedule(trigger.getCronSchedule()))
                        .build();
            } else {
                qzTrigger = newTrigger()
                        .withIdentity(triggerId)
                        .startNow()
                        .forJob(jobId)
                        .usingJobData("email", trigger.getEmail())
                        .withSchedule(cronSchedule(trigger.getCronSchedule()))
                        .build();
            }

            if (doesUserHavePermissionToTrigger(qzTrigger)) {
                scheduler.scheduleJob(qzTrigger);
            } else {
                throw new AccessDeniedException("Can not create this trigger");
            }

            ChplTrigger newTrigger = new ChplTrigger((CronTrigger) scheduler.getTrigger(triggerId));
            return newTrigger;
        } else {
            throw new AccessDeniedException("Can not create this trigger");
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ONC', 'ROLE_ACB')")
    public void deleteTrigger(final String triggerGroup, final String triggerName)
            throws SchedulerException, ValidationException {
        Scheduler scheduler = getScheduler();
        TriggerKey triggerKey = triggerKey(triggerName, triggerGroup);

        if (doesUserHavePermissionToTrigger(scheduler.getTrigger(triggerKey))) {
            scheduler.unscheduleJob(triggerKey);
        } else {
            throw new AccessDeniedException("Can not update this trigger");
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ONC', 'ROLE_ACB')")
    public List<ChplTrigger> getAllTriggers() throws SchedulerException {
        ArrayList<ChplTrigger> triggers = new ArrayList<ChplTrigger>();
        Scheduler scheduler = getScheduler();
        for (String group: scheduler.getTriggerGroupNames()) {
            // enumerate each trigger in group
            for (TriggerKey triggerKey : scheduler.getTriggerKeys(groupEquals(group))) {
                if (scheduler.getTrigger(triggerKey).getJobKey().getGroup().equalsIgnoreCase("chplJobs")) {
                    if (doesUserHavePermissionToTrigger(scheduler.getTrigger(triggerKey))
                            && getScheduler().getTrigger(triggerKey) instanceof CronTrigger) {
                        ChplTrigger newTrigger = getChplTrigger(triggerKey);
                        triggers.add(newTrigger);
                    }
                }
            }
        }
        return triggers;
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_ONC', 'ROLE_ACB')")
    public ChplTrigger updateTrigger(final ChplTrigger trigger) throws SchedulerException, ValidationException {
        Scheduler scheduler = getScheduler();
        Trigger oldTrigger = scheduler.getTrigger(triggerKey(trigger.getName(), trigger.getGroup()));
        Trigger qzTrigger = null;
        if (doesUserHavePermissionToTrigger(oldTrigger)) {
            if (trigger.getJob().getJobDataMap().getBooleanValue("acbSpecific")) {
                qzTrigger = newTrigger()
                        .withIdentity(oldTrigger.getKey())
                        .startNow()
                        .forJob(oldTrigger.getJobKey())
                        .usingJobData(oldTrigger.getJobDataMap())
                        .usingJobData("acb", trigger.getAcb())
                        .withSchedule(cronSchedule(trigger.getCronSchedule()))
                        .build();
            } else {
                qzTrigger = newTrigger()
                        .withIdentity(oldTrigger.getKey())
                        .startNow()
                        .forJob(oldTrigger.getJobKey())
                        .usingJobData(oldTrigger.getJobDataMap())
                        .withSchedule(cronSchedule(trigger.getCronSchedule()))
                        .build();
            }
            scheduler.rescheduleJob(oldTrigger.getKey(), qzTrigger);

            ChplTrigger newTrigger = getChplTrigger(qzTrigger.getKey());
            return newTrigger;
        } else {
            throw new AccessDeniedException("Can not update this trigger");
        }
    }

    /* (non-Javadoc)
     * @see gov.healthit.chpl.manager.SchedulerManager#getAllJobs()
     * As new jobs are added that have authorities other than ROLE_ADMIN, those authorities
     * will need to be added to the list.
     */
    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ONC', 'ROLE_ACB')")
    public List<ChplJob> getAllJobs() throws SchedulerException {
        List<ChplJob> jobs = new ArrayList<ChplJob>();
        Scheduler scheduler = getScheduler();
        for (String group : scheduler.getJobGroupNames()) {
            if (group.equals("chplJobs")) {
                for (JobKey jobKey : scheduler.getJobKeys(groupEquals(group))) {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    if (doesUserHavePermissionToJob(jobDetail)) {
                        jobs.add(new ChplJob(jobDetail));
                    }
                }
            }
        }
        return jobs;
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_ONC', 'ROLE_ACB')")
    public ChplJob updateJob(final ChplJob job) throws SchedulerException {
        Scheduler scheduler = getScheduler();
        JobKey jobId = jobKey(job.getName(), job.getGroup());
        JobDetail oldJob = scheduler.getJobDetail(jobId);
        if (doesUserHavePermissionToJob(oldJob)) {
            JobDetail newJob = newJob(oldJob.getJobClass())
                    .withIdentity(jobId)
                    .withDescription(oldJob.getDescription())
                    .usingJobData(job.getJobDataMap())
                    .storeDurably(oldJob.isDurable())
                    .requestRecovery(oldJob.requestsRecovery())
                    .build();

            scheduler.addJob(newJob, true);
            ChplJob newChplJob = new ChplJob(newJob);
            return newChplJob;
        } else {
            throw new AccessDeniedException("Can not update this job");
        }
    }

    private Scheduler getScheduler() throws SchedulerException {
        return chplScheduler.getScheduler();
    }

    private ChplTrigger getChplTrigger(final TriggerKey triggerKey) throws SchedulerException {
        ChplTrigger chplTrigger = new ChplTrigger((CronTrigger) getScheduler().getTrigger(triggerKey));

        JobDetail jobDetail = getScheduler().getJobDetail(getScheduler().getTrigger(triggerKey).getJobKey());
        ChplJob chplJob = new ChplJob(jobDetail);
        chplTrigger.setJob(chplJob);
        return chplTrigger;
    }

    private Boolean doesUserHavePermissionToJob(final JobDetail jobDetail) {
        //Get the authorities from the job
        if (jobDetail.getJobDataMap().containsKey("authorities")) {
            List<String> authorities = new ArrayList<String>(
                    Arrays.asList(jobDetail.getJobDataMap().get("authorities").toString().split(AUTHORITY_DELIMITER)));
            Set<GrantedPermission> userRoles = Util.getCurrentUser().getPermissions();
            for (GrantedPermission permission : userRoles) {
                for (String authority : authorities) {
                    if (permission.getAuthority().equalsIgnoreCase(authority)) {
                        return true;
                    }
                }
            }
        } else {
            //If no authorities are present, we assume there are no permissions on the job
            //and everyone has access
            return true;
        }
        //At this point we have fallen through all of the logic, and the user does not have the appropriate rights
        return false;
    }

    private Boolean doesUserHavePermissionToTrigger(final Trigger trigger) throws SchedulerException {
        // first check user has permission on job
        if (doesUserHavePermissionToJob(getScheduler().getJobDetail(trigger.getJobKey()))) {
            if (!StringUtils.isEmpty(trigger.getJobDataMap().getString("acb"))) {
                // get acbs user has access to
                List<CertificationBodyDTO> validAcbs = acbManager.getAllForUser();
                for (String acb : trigger.getJobDataMap().getString("acb").split("\u263A")) {
                    boolean found = false;
                    for (CertificationBodyDTO validAcb : validAcbs) {
                        if (acb.equalsIgnoreCase(validAcb.getName())) {
                            found = true;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }
            // if not acb specific no need to check acbs
            return true;
        }
        return false;
    }

    private String createTriggerGroup(final ChplTrigger trigger) {
        String group = trigger.getJob().getName().replaceAll(" ", "");
        group += "Trigger";
        return group;
    }

    private String createTriggerName(final ChplTrigger trigger) {
        String name = trigger.getEmail().replaceAll("\\.",  "_");
        if (!StringUtils.isEmpty(trigger.getAcb())) {
            name += trigger.getAcb();
        }
        return name;
    }
}
