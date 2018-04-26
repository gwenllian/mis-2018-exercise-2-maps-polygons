package com.example.mis.polygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private static final String TAG = "MainActivity";

    private GoogleMap mMap;
    private EditText mEditText;
    private int mMarkers;
    private ToggleButton toggle;
    private boolean drawPoly = false;
    private List<LatLng> mPolyLatLngs = new ArrayList<>();
    private Polygon poly;
    private double m2tokm2 = 0.000001;
    Marker cen;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //https://developer.android.com/guide/topics/ui/controls/togglebutton
        toggle = (ToggleButton) findViewById(R.id.poly);

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    drawPoly = true;
                } else {
                    drawPoly = false;
                    mPolyLatLngs.clear();
                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mEditText = (EditText) findViewById(R.id.label);
    }




    // https://stackoverflow.com/questions/16097143/google-maps-android-api-v2-detect-long-click-on-map-and-add-marker-not-working
    @Override
    public void onMapLongClick(final LatLng point) {
                if(drawPoly) {
                    mPolyLatLngs.add(new LatLng(point.latitude, point.longitude));
                    if (mPolyLatLngs.size() == 1){
                        PolygonOptions polyOptions = new PolygonOptions().add(point).fillColor(Color.argb(80, 255, 60, 0)).strokeColor(Color.argb(40, 255, 60, 0));
                        poly = mMap.addPolygon(polyOptions);
                    }
                    else poly.setPoints(mPolyLatLngs);

                    if (mPolyLatLngs.size() > 2){
                        if (cen!= null) cen.remove();
                        double area = area(mPolyLatLngs);
                        double areaKM = area * m2tokm2;

                        LatLng centroid = calculateCentroid(mPolyLatLngs);
                        Log.i(TAG, Double.toString(centroid.latitude) + Double.toString(centroid.longitude));
                        MarkerOptions options;

                        DecimalFormat f = new DecimalFormat("#.###");

                        if (areaKM > 1.0){
                            options = new MarkerOptions().title((f.format(areaKM)) + " km^2").position(new LatLng(centroid.latitude, centroid.longitude));
                        }
                        else {
                            options = new MarkerOptions().title(f.format(area) + " m^2").position(new LatLng(centroid.latitude, centroid.longitude));
                        }

                        cen = mMap.addMarker(options);
                    }
                }
                else{
                    String label = mEditText.getText().toString();
                    MarkerOptions options = new MarkerOptions().title(label).position(new LatLng(point.latitude, point.longitude));
                    mMap.addMarker(options);
                    mMarkers++;

                    // https://developer.android.com/training/data-storage/shared-preferences.html#java
                    // https://developers.google.com/android/reference/com/google/android/gms/maps/model/MarkerOptions.html

                    SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();


                    editor.putInt("MarkerCount", mMarkers);
                    editor.putString("Title" + Integer.toString(mMarkers), label);
                    editor.putString("Lat" + Integer.toString(mMarkers), Double.toString(point.latitude));
                    editor.putString("Lon" + Integer.toString(mMarkers), Double.toString(point.longitude));
                    editor.apply();
                }
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        getMarkers();



        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    public void getMarkers() {
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        int num = sharedPreferences.getInt("MarkerCount", 0);
        mMarkers = num;
        String label = "";
        double lat = 0;
        double lon = 0;
        for (int i = 0; i < num; i++){
            label = sharedPreferences.getString("Title" + i, "");
            lat = Double.valueOf(sharedPreferences.getString("Lat" + i, "0"));
            lon = Double.valueOf(sharedPreferences.getString("Lon" + i, "0"));

            MarkerOptions options = new MarkerOptions().title(label).position(new LatLng(lat, lon));
            mMap.addMarker(options);
        }

    }

    // https://stackoverflow.com/a/18444984
    public LatLng calculateCentroid (List<LatLng> LatLongs) {
        Double lat = 0.0;
        Double lon = 0.0;

        for(LatLng l: LatLongs) {
            lat += l.latitude;
            lon += l.longitude;
        }
        int s = LatLongs.size();
        lat = lat/s;
        lon = lon/s;
        return new LatLng(lat, lon);
    }

    // http://googlemaps.github.io/android-maps-utils/
    // http://googlemaps.github.io/android-maps-utils/javadoc/com/google/maps/android/SphericalUtil.html#computeArea-java.util.List-
    public double area(List<LatLng> LatLongs) {
        return SphericalUtil.computeArea(LatLongs);  // SphericalUtils.computeArea returns area in square METERS
    }



}
