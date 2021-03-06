package gov.healthit.chpl.manager;

import java.util.List;

import org.quartz.SchedulerException;

import gov.healthit.chpl.domain.schedule.ChplJob;
import gov.healthit.chpl.domain.schedule.ChplTrigger;
import gov.healthit.chpl.exception.ValidationException;

/**
 * Interface for managing schedules.
 * @author alarned
 *
 */
public interface SchedulerManager {
    /**
     * Create a new Trigger.
     * @param trigger the CHPL Trigger to create
     * @throws SchedulerException if scheduler has an issue
     * @throws ValidationException if job values aren't correct
     * @return the new trigger
     */
    ChplTrigger createTrigger(ChplTrigger trigger) throws SchedulerException, ValidationException;

    /**
     * Delete an existing Trigger.
     * @param triggerGroup group name of trigger
     * @param triggerName trigger name of trigger
     * @throws SchedulerException if scheduler has an issue
     * @throws ValidationException if job values aren't correct
     */
    void deleteTrigger(String triggerGroup, String  triggerName) throws SchedulerException, ValidationException;

    /**
     * Get all active Triggers.
     * @throws SchedulerException if scheduler has an issue
     * @return the triggers
     */
    List<ChplTrigger> getAllTriggers() throws SchedulerException;

    /**
     * Update trigger with new data.
     * @param trigger old trigger
     * @return updated trigger
     * @throws SchedulerException if scheduler has issue
     * @throws ValidationException if job values aren't correct
     */
    ChplTrigger updateTrigger(ChplTrigger trigger) throws SchedulerException, ValidationException;


    /**
     * Get all jobs which the current logged in user has access to.
     * @return List of ChplJob objects
     * @throws SchedulerException is thrown
     */
    List<ChplJob> getAllJobs() throws SchedulerException;

    /**
     * Update job with new data.
     * @param job old job
     * @return updated job
     * @throws SchedulerException if scheduler has issues
     */
    ChplJob updateJob(ChplJob job) throws SchedulerException;
}
