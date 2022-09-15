package com.example.fgbtracker;

public class TargetObject {
    private double lat;
    private double lon;
    private double ele;
    private double velocity;
    private double heading;

    public TargetObject(double la, double lo, double el, double v, double h)
    {
        lat = la;
        lon = lo;
        ele = el;
        velocity = v;
        heading = h;
    }
}
