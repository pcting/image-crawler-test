package com.patrickting.test.imagecrawler.messages;

public class ImageCrawlerActorJob {

    private int jobId;
    private String url;
    private int height;
    private int width;

    public ImageCrawlerActorJob(int jobId, String url, int width, int height) {
        this.jobId = jobId;
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
