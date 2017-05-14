package com.xzk.dodgefitnessmapv2;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.estimote.coresdk.common.requirements.SystemRequirementsChecker;
import com.estimote.coresdk.observation.region.RegionUtils;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.repackaged.gson_v2_3_1.com.google.gson.internal.Primitives;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.BaseMarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // debug
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST = 0;
    // sensors
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;

    private float[] mAccelerometerValues = new float[3];
    private float[] mMagneticFieldValues = new float[3];
    private float[] mValues = new float[3];
    private float[] mMatrix = new float[9];

    private float curDirect = 0;

    // beacon
    private ProximityContentManager proximityContentManager;
    private List<Beacon> beaconList;
    private LocSystem locSystem;

    // mapbox
    private MapView mapView;
    private MarkerView marker;
    private MapboxMap map;
    //private int apiCallTime = 300;
    private int apiCallTime = 300;
    private int animationTime = 3000;
    private Handler handler;
    private Runnable runnable;

    private Point pre = null;
    private int stayCount = 0;

    // test
    //private LatLng testLatLng;
    private int testCount1 = 0;
    private boolean testDone1 = false;
    private String testStr1 = "";

    private int testCount2 = 0;
    private boolean testDone2 = false;
    private String testStr2 = "";

    private int testCount3 = 0;
    private boolean testDone3 = false;
    private String testStr3 = "";

    private Filter filter = new Filter();

    private List<Point> routeList;
    private int routeCount = 0;

    private boolean done = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //sd card
        verifyStoragePermissions();
        //writeToFile("test");


        // init sensor
        initSensorService();

        // read routes and beacon positions
        final InputStream routeStream = getResources().openRawResource(R.raw.route);
        final InputStream beaconStream = getResources().openRawResource(R.raw.beacons);
        routeList = getRoute(getString(routeStream));
        locSystem = new LocSystem(getBeacons(getString(beaconStream)), routeList);

        // beacon

        proximityContentManager = new ProximityContentManager(this);
        proximityContentManager.setListener(new ProximityContentManager.Listener() {
            @Override
            public void onContentChanged(Object content) {
                Random r = new Random();
                //testLatLng = new LatLng(r.nextDouble() + 40, r.nextDouble() - 74);
                beaconList = (List<Beacon>) content;
                // test

            }
        });

        // mapbox

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.tty_token));

        // This contains the MapView in XML and needs to be called after the account manager
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
                Icon icon = iconFactory.fromResource(R.drawable.dot_1);
                /*
                for (Point p : routeList) {
                    Log.d(TAG, "route lat = " + p.y  + " log = " + p.x);
                    mapboxMap.addMarker(new MarkerOptions()
                            .position(new LatLng(p.y, p.x))
                            .icon(icon));
                }
                */
                callApi();
            }
        });
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        // sensor
        registerSensorService();
        // mapbox
        mapView.onResume();
        // beacon
        if (!SystemRequirementsChecker.checkWithDefaultDialogs(this)) {
            Log.e(TAG, "Can't scan for beacons, some pre-conditions were not met");
            Log.e(TAG, "Read more about what's required at: http://estimote.github.io/Android-SDK/JavaDocs/com/estimote/sdk/SystemRequirementsChecker.html");
            Log.e(TAG, "If this is fixable, you should see a popup on the app's screen right now, asking to enable what's necessary");
        } else {
            Log.d(TAG, "Starting ProximityContentManager content updates");
            proximityContentManager.startContentUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // sensor
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        //mapbox
        mapView.onPause();

        // beacon
        Log.d(TAG, "Stopping ProximityContentManager content updates");
        proximityContentManager.stopContentUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        proximityContentManager.destroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public static List<String> getString(InputStream inputStream) {
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(inputStream, "gbk");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        List<String> res = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(inputStreamReader);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                res.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static List<Point> getRoute(List<String> list) {
        List<Point> points = new ArrayList<>();
        for (String s : list) {
            if (s.charAt(0) == '/') {
                continue;
            }
            String[] tmp = s.split(",");
            points.add(new Point(Double.parseDouble(tmp[0]), Double.parseDouble(tmp[1])));
        }
        return points;
    }

    public static List<BeaconID> getBeacons(List<String> list) {
        List<BeaconID> beacons = new ArrayList<>();
        for (String s : list) {
            Log.d(TAG, "getBeacons: " + s);
            String[] tmp = s.split(",");
            beacons.add(new BeaconID(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Double.parseDouble(tmp[2]), Double.parseDouble(tmp[3])));
        }
        return beacons;
    }

    /* sensor methods */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mMagneticFieldValues = event.values;
        }
        SensorManager.getRotationMatrix(mMatrix, null, mAccelerometerValues, mMagneticFieldValues);
        SensorManager.getOrientation(mMatrix, mValues);
        curDirect = (float) Math.toDegrees(mValues[0]);
        //Log.d(TAG, "degree = " + curDirect);
        if (curDirect < 0) {
            curDirect = 360 + curDirect;
        }
        //Random r = new Random();
        //testLatLng = new LatLng(r.nextDouble() + 40, r.nextDouble() - 74);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initSensorService() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void registerSensorService() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /* sensor methods end */

    /* marker methods begin */
    private void callApi() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {

                if (beaconList != null && beaconList.size() > 0) {
                    for (Beacon x : beaconList) {
                        if (x.getMajor() == 30700) {
                            double dis = RegionUtils.computeAccuracy(x);
                            Log.d(TAG, "test1 dis = " + dis);
                            testStr1 += String.valueOf(dis) + "\n";
                            testCount1++;
                            Log.d(TAG, "testCount1 = " + testCount1);
                        }
                        if (x.getMajor() == 37861) {
                            double dis = filter.filter(RegionUtils.computeAccuracy(x));
                            testStr2 += String.valueOf(dis) + "\n";
                            testCount2++;
                            Log.d(TAG, "testCount2 = " + testCount2);
                        }
                        if (x.getMajor() == 47129) {
                            double dis = filter.filter(RegionUtils.computeAccuracy(x));
                            testStr3 += String.valueOf(dis) + "\n";
                            testCount3++;
                            Log.d(TAG, "testCount3 = " + testCount3);
                        }

                    }
                }
                if (testCount1 == 1000 && !testDone1) {
                    writeToFile(testStr1, "test1.txt");
                    testDone1 = true;
                    Log.d(TAG, "write to file1");
                }
                if (testCount2 == 1000 && !testDone2) {
                    writeToFile(testStr2, "test2.txt");
                    testDone2 = true;
                    Log.d(TAG, "write to file2");
                }

                if (testCount3 == 1000 && !testDone3) {
                    writeToFile(testStr3, "test3.txt");
                    testDone3 = true;
                    Log.d(TAG, "write to file3");
                }

                Point cur = locSystem.getLocation(beaconList);
                // fix
                cur.y = cur.y - 0.7;

                if (pre != null &&pre.x == cur.x && pre.y == cur.y) {
                    stayCount++;
                    if (stayCount == 3) {
                        Dialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setTitle("")
                                .setMessage("Watch Video?")
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                })
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // TODO Auto-generated method stub
                                        Uri uri = Uri.parse("https://www.youtube.com/watch?v=E15Q3Z9J-Zg");
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        startActivity(intent);
                                    }
                                }).create();
                        dialog.show();
                    }
                } else {
                    pre = cur;
                    stayCount = 0;
                }

                updateMarkerPosition(new LatLng(cur.y, cur.x));
                handler.postDelayed(this, apiCallTime);
            }
        };
        // first time
        handler.post(runnable);
    }


    private void updateMarkerPosition(LatLng position) {
        if (marker == null) {
            // Create an Icon object for the marker to use
            IconFactory iconFactory = IconFactory.getInstance(MainActivity.this);
            Icon icon = iconFactory.fromResource(R.drawable.arrow);
            // Customize map with markers, polylines, etc.

            marker = map.addMarker(new MarkerViewOptions()
                    .position(new LatLng(40.73581, -73.99155))
                    .icon(icon));




            return;
        }

        marker.setRotation(curDirect);

        ValueAnimator markerAnimator = ObjectAnimator.ofObject(marker, "position",
                new LatLngEvaluator(), marker.getPosition(), position);
        markerAnimator.setDuration(animationTime);
        markerAnimator.start();
    }

    private static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.

        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }

    // sd card
    public void writeToFile(String data, String name) {
        // Get the directory for the user's public pictures directory.
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (
                                //Environment.DIRECTORY_PICTURES
                                Environment.DIRECTORY_DCIM
                        );

        // Make sure the path directory exists.
        if (!path.exists()) {
            // Make it, if it doesn't exit
            path.mkdirs();
        }

        final File file = new File(path, name);

        // Save your stream, don't forget to flush() it before closing it.

        try {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     */
    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST
            );
        } else {
            Log.d(TAG, "storage permission got");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "storage permission result");
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
