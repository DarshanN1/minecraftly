package com.minecraftly.core.bungee.handlers.job;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Keir on 28/06/2015.
 */
public class JobManager {

    private final Map<JobType, JobQueue<?>> jobQueues = new HashMap<>();

    public JobQueue<?> getJobQueue(JobType jobType) {
        return jobQueues.get(jobType);
    }

    public void addJobQueue(JobType jobType, JobQueue<?> jobQueue) {
        if (jobQueues.containsKey(jobType)) {
            throw new UnsupportedOperationException("Attempted to double register JobType: " + jobType.name());
        }

        if (!jobType.getClassType().isAssignableFrom(jobQueue.getParameterType())) {
            throw new UnsupportedOperationException(
                    "JobQueue's parameter type must be the same, or a super class of the class type: "
                            + jobType.getClassType().getName()
            );
        }

        jobQueues.put(jobType, jobQueue);
    }

    public void removeJobQueue(JobType jobType) {
        jobQueues.remove(jobType);
    }

}
