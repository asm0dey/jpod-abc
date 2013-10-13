package com.github.asm0dey.m4bconverter.model;

/**
 * User: finkel
 * Date: 13.10.13
 * Time: 13:09
 */
public class Track {
    private String fileName;
    private String track;

    public Track(String fileName) {
        this.fileName = fileName;
    }

    public Track(String fileName, String track) {
        this.fileName = fileName;
        this.track = track;
    }

    public Track() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track)) return false;

        Track track1 = (Track) o;

        if (fileName != null ? !fileName.equals(track1.fileName) : track1.fileName != null) return false;
        if (track != null ? !track.equals(track1.track) : track1.track != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (track != null ? track.hashCode() : 0);
        return result;
    }
}
