package com.xzk.dodgefitnessmapv2;

import com.estimote.coresdk.recognition.packets.Beacon;

import java.util.UUID;

public class BeaconID {

    private int major;
    private int minor;
    public double x;
    public double y;
    public double dis;

    public static BeaconID fromBeacon(Beacon beacon) {
        return new BeaconID(beacon.getMajor(), beacon.getMinor());
    }

    public BeaconID(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public BeaconID(int major, int minor, double x, double y) {
        this.major = major;
        this.minor = minor;
        this.x = x;
        this.y = y;
    }


    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public String toString() {
        return getMajor() + ":" + getMinor();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (getClass() != o.getClass()) {
            return super.equals(o);
        }

        BeaconID other = (BeaconID) o;

        return getMajor() == other.getMajor()
                && getMinor() == other.getMinor();
    }
}