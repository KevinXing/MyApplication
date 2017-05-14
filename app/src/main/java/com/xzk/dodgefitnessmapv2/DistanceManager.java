package com.xzk.dodgefitnessmapv2;

import android.content.Context;
import android.util.Log;

import com.estimote.coresdk.observation.region.RegionUtils;
import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;


import java.util.List;

public class DistanceManager {

    private static final String TAG = "DistanceManager";

    private static final BeaconRegion ALL_ESTIMOTE_BEACONS = new BeaconRegion("all Estimote beacons", null, null, null);

    private Listener listener;

    private BeaconManager beaconManager;

    public DistanceManager(Context context) {
        beaconManager = new BeaconManager(context);
        beaconManager.setForegroundScanPeriod(300, 0);
        beaconManager.setRangingListener(new BeaconManager.BeaconRangingListener() {
            @Override
            public void onBeaconsDiscovered(BeaconRegion region, List<Beacon> list) {
                getDistances(list);
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onDistanceChanged(List<Beacon> beacons);
    }

    public void startDistanceUpdates() {
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
            }
        });
    }

    public void stopDistanceUpdates() {
        beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
    }

    public void destroy() {
        beaconManager.disconnect();
    }

    private void getDistances(List<Beacon> beacons) {

        if (listener != null) {
            listener.onDistanceChanged(beacons);
        }
        for (Beacon beacon : beacons) {
            double distance = RegionUtils.computeAccuracy(beacon);
            Log.d(TAG, "beacon: " + beacon + ", distance: " + distance);
        }
    }
}
