package com.tilofy.test.imagecrawler.model;

public class EnqueueJobResponse {

    private int jobId;

    public EnqueueJobResponse(int jobId) {
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

}
