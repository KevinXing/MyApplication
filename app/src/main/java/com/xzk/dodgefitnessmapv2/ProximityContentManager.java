package com.xzk.dodgefitnessmapv2;

import android.content.Context;

import com.estimote.coresdk.recognition.packets.Beacon;

import java.util.List;

public class ProximityContentManager {

    private DistanceManager distanceManager;

    private Listener listener;

    public ProximityContentManager(Context context) {
        distanceManager = new DistanceManager(context);
        distanceManager.setListener(new DistanceManager.Listener() {
            @Override
            public void onDistanceChanged(List<Beacon> beacons) {
                if (listener == null) {
                    return;
                }
                listener.onContentChanged(beacons);
            }
        });
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onContentChanged(Object content);
    }

    public void startContentUpdates() {
        distanceManager.startDistanceUpdates();
    }

    public void stopContentUpdates() {
        distanceManager.stopDistanceUpdates();
    }

    public void destroy() {
        distanceManager.destroy();
    }
}
