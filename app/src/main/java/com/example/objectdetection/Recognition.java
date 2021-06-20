package com.example.objectdetection;

import android.graphics.RectF;

public class Recognition {
    private final String id;
    private final String title;
    private final Float confidence;
    private RectF location;

    public Recognition(String id, String title, Float confidence, RectF location)
    {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
    }

    public String getId(){return this.id;}
    public String getTitle(){return this.title;}
    public Float getConfidence(){return this.confidence;}
    public RectF getLocation(){return this.location;}

    public void setLocation(RectF location){
        this.location = location;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (id != null) {
            resultString += "[" + id + "] ";
        }

        if (title != null) {
            resultString += title + " ";
        }

        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f);
        }

        if (location != null) {
            resultString += location + " ";
        }

        return resultString.trim();
    }
}

