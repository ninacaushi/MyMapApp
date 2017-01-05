package com.example.user.mymap;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.HttpAuthHandler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.example.user.mymap.R.id.map;
import static java.lang.Math.min;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        LocationListener {

    private GoogleMap mMap;
    private Circle Circle;
    private Marker mMarker;
    public int draw_dist = 5000;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;

    //Location location;
    public double latitude;
    public double longitude;

    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;

    public float min_distance = 1000000;
    public int closest = 0;

    boolean display_radius = true;

    //save markers on rotate device
    private List<Marker> markerList;
    public MapsActivity(){
        if(markerList == null){
            markerList = new ArrayList<Marker>();
        }
    }



    //clear the markers/poly on new action
    public final void clear() {
            mMap.clear();
        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        //LoadPreferences();

        final EditText mEdit = (EditText) findViewById(R.id.distance);

        mEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    DeletePreferences();
                    clear();

                      Log.e("EditText", mEdit.getText().toString());
                      String value = mEdit.getText().toString();
                      draw_dist = Integer.parseInt(value);
                      Log.e("draw_dist", Integer.toString(draw_dist));

                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                    handled = true;
                }
                return handled;
            }
        });


        Button Busbutton = (Button) findViewById(R.id.button_bus);
        Busbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    clear();
                    DeletePreferences();
                    performJSON_buses();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button Velohbutton = (Button) findViewById(R.id.button_veloh);
        Velohbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    clear();
                    DeletePreferences();
                    performJSON_veloh();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button ClosestBusbutton = (Button) findViewById(R.id.button_closest_bus);
        ClosestBusbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    clear();
                    DeletePreferences();
                    performJSON_closest_buses();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void performJSON_closest_buses() throws ExecutionException, InterruptedException, JSONException {

        locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

        //You can still do this if you like, you might get lucky:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            Log.e("TAG", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            JSONTask myJson = new JSONTask(this);
            myJson.execute("https://api.tfl.lu/stations");
            String result = myJson.get();
            Log.d("Result", result);

            JSONArray jsonArray_buses = new JSONArray(result);
            Log.d("JSON array", jsonArray_buses.toString());

             for (int i = 0; i < jsonArray_buses.length(); i++) {
                JSONObject jsonObject = jsonArray_buses.getJSONObject(i);
                double lng = Double.parseDouble(jsonArray_buses.getJSONObject(i).getString("longitude"));
                double lat = Double.parseDouble(jsonArray_buses.getJSONObject(i).getString("latitude"));

                Log.e("TAG_latlong", "latitude:" + latitude + " longitude:" + longitude + "marker_lat:" + lat + "marker_long:" + lng);

                Location locationA = new Location("point A");
                locationA.setLatitude(lat);
                locationA.setLongitude(lng);

                Location locationB = new Location("point B");
                locationB.setLatitude(latitude);
                locationB.setLongitude(longitude);


                float min_distance_old = min_distance;
                min_distance = min(min_distance, locationA.distanceTo(locationB));

                if (min_distance_old != min_distance) {
                    closest = i;
                }

             } //end for


            JSONObject display_jsonObject = jsonArray_buses.getJSONObject(closest);
            //save
            double lng = Double.parseDouble(jsonArray_buses.getJSONObject(closest).getString("longitude"));
            double lat = Double.parseDouble(jsonArray_buses.getJSONObject(closest).getString("latitude"));
            markerList.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                    .title(display_jsonObject.getString("name"))
                    .snippet(Integer.toString((display_jsonObject.getInt("id"))))
                    .position(new LatLng(lat, lng))));
            display_radius = false;

            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {
                    // TODO Auto-generated method stub
                    Log.d("", marker.getSnippet());
                    try {
                        clear();
                        perform_Json_departures();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });

//            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
//                @Override
//                public void onInfoWindowClick(Marker marker) {
//                    Intent intent = new Intent(MapsActivity.this,NewActivity.class);
//                    startActivity(intent);
//
//
//                }
//            });

        SavePreferences();
        } //end if location null
        else{
            //This is what you need:
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    } //end json

    public void perform_Json_departures () throws ExecutionException, InterruptedException, JSONException {

        Log.e("TAG_title_click", "CLICKED Title!");

        //Log.d("", marker.getTitle());

        JSONTask myJson1 = new JSONTask(this);
        myJson1.execute("https://api.tfl.lu/departures/8800088");
        String result = myJson1.get();
        Log.d("Result", result);

        JSONArray jsonArray_departures = new JSONArray(result);
        Log.d("JSON array", jsonArray_departures.toString());

    }


    @Override
    public void onInfoWindowClick(Marker marker) {
        Toast.makeText(this, "Info window clicked",
                Toast.LENGTH_SHORT).show();
    }

    public void performJSON_buses() throws ExecutionException, InterruptedException, JSONException {


        locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

        //You can still do this if you like, you might get lucky:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            Log.e("TAG", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            JSONTask myJson = new JSONTask(this);
            myJson.execute("https://api.tfl.lu/stations");
            String result = myJson.get();
            Log.d("Result", result);

            JSONArray jsonArray_buses = new JSONArray(result);
            //jsonArray_save = jsonArray_buses;
            Log.d("JSON array", jsonArray_buses.toString());

            display_radius = true;
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .radius(draw_dist)
                    .strokeColor(Color.DKGRAY)
                    .fillColor(Color.LTGRAY));



            for (int i = 0; i < jsonArray_buses.length(); i++) {
                JSONObject jsonObject = jsonArray_buses.getJSONObject(i);

                double lng = Double.parseDouble(jsonArray_buses.getJSONObject(i).getString("longitude"));
                double lat = Double.parseDouble(jsonArray_buses.getJSONObject(i).getString("latitude"));

                Log.e("TAG_latlong", "latitude:" + latitude + " longitude:" + longitude + "marker_lat:" + lat + "marker_long:" + lng);

                Location locationA = new Location("point A");
                locationA.setLatitude(lat);
                locationA.setLongitude(lng);

                Location locationB = new Location("point B");
                locationB.setLatitude(latitude);
                locationB.setLongitude(longitude);

                float distance = locationA.distanceTo(locationB);

                if (distance < draw_dist) {


                    markerList.add(mMap.addMarker (new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                        .title(jsonObject.getString("name"))
                        .snippet(Integer.toString((jsonObject.getInt("id"))))
                        .position(new LatLng(lat, lng))));

                //markerList.add(mMap.addMarker(mo));
                }

            }

            SavePreferences();

        }
        else{
            //This is what you need:
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }


        }

    public void performJSON_veloh() throws ExecutionException, InterruptedException, JSONException {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

        //You can still do this if you like, you might get lucky:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if (location != null) {
            Log.e("TAG", "GPS is on");
            latitude = location.getLatitude();
            longitude = location.getLongitude();


            JSONTask myJson = new JSONTask(this);
            myJson.execute("https://developer.jcdecaux.com/rest/vls/stations/Luxembourg.json");
            String result = myJson.get();
            Log.d("Result", result);

            JSONArray jsonArray_veloh = new JSONArray(result);
            Log.d("JSON array", jsonArray_veloh.toString());
            display_radius = true;

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(location.getLatitude(), location.getLongitude()))
                    .radius(draw_dist)
                    .strokeColor(Color.DKGRAY)
                    .fillColor(Color.LTGRAY));

            for (int i = 0; i < jsonArray_veloh.length(); i++) {
                JSONObject jsonObject = jsonArray_veloh.getJSONObject(i);
                double lng = Double.parseDouble(jsonArray_veloh.getJSONObject(i).getString("longitude"));
                double lat = Double.parseDouble(jsonArray_veloh.getJSONObject(i).getString("latitude"));
                String address = jsonArray_veloh.getJSONObject(i).getString("address");
                String name = jsonArray_veloh.getJSONObject(i).getString("name");
                String number = jsonArray_veloh.getJSONObject(i).getString("number");

                Log.e("TAG_latlong", "latitude:" + latitude + " longitude:" + longitude + "marker_lat:" + lat + "marker_long:" + lng);

                Location locationA = new Location("point A");
                locationA.setLatitude(lat);
                locationA.setLongitude(lng);

                Location locationB = new Location("point B");
                locationB.setLatitude(latitude);
                locationB.setLongitude(longitude);

                float distance = locationA.distanceTo(locationB);

                if (distance < draw_dist) {


                    markerList.add(mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                            .title(jsonObject.getString("name"))
                            .snippet(Integer.toString((jsonObject.getInt("number"))))
                            .position(new LatLng(lat, lng))));
                }
            }

            SavePreferences();
        } else {
            //This is what you need:
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
                mMap.setOnInfoWindowClickListener(this);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
            mMap.setOnInfoWindowClickListener(this);
        }
        LoadPreferences();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.title("Current position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        //Here was the circle

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void SavePreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.putInt("listSize", markerList.size());
        editor.putInt("draw_dist", draw_dist);
        editor.putBoolean("display_radius", display_radius);
        //editor.putLong("latitude", latitude);

        for(int i = 0; i <markerList.size(); i++){
            editor.putFloat("lat"+i, (float) markerList.get(i).getPosition().latitude);
            editor.putFloat("lng"+i, (float) markerList.get(i).getPosition().longitude);
            editor.putString("title"+i, markerList.get(i).getTitle());
            editor.putString("snippet"+i, markerList.get(i).getSnippet());
        }

        editor.commit();
        Log.e("TAG_save", "saved preferences");
    }


    private void DeletePreferences() {
        markerList.clear();
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    private void LoadPreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        int size = sharedPreferences.getInt("listSize", 0);
        draw_dist = sharedPreferences.getInt("draw_dist", 0);
        display_radius = sharedPreferences.getBoolean("display_radius", false);

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                double lat = (double) sharedPreferences.getFloat("lat" + i, 0);
                double lng = (double) sharedPreferences.getFloat("lng" + i, 0);
                String title = sharedPreferences.getString("title" + i, "NULL");
                String snippet = sharedPreferences.getString("snippet" + i, "NULL");

                markerList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(title).snippet(snippet)));
            }

            locationManager = (LocationManager)  this.getSystemService(Context.LOCATION_SERVICE);
            criteria = new Criteria();
            bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkLocationPermission();
            }

            Location location = locationManager.getLastKnownLocation(bestProvider);

            if ((location != null)) {
                Log.e("TAG", "GPS is on");
                if (display_radius) {

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(new LatLng(location.getLatitude(), location.getLongitude()))
                            .radius(draw_dist)
                            .strokeColor(Color.DKGRAY)
                            .fillColor(Color.LTGRAY));
                }
            }
            else{
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
        }


    }
    protected void onPause() {
        super.onPause();
        SavePreferences();
    }
    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
            SavePreferences();
        super.onSaveInstanceState(savedInstanceState);
    }

    //onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

    }
//update

}
