package com.example.user.mymap;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.example.user.mymap.R.id.map;
import static java.lang.Math.min;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    private GoogleMap mMap;
    public int draw_dist = 5000;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;

    public double latitude;
    public double longitude;

    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;

    //initialize a string used later to save the downloaded n=bus feature collection geojson
    String geodata = "{aa:xxxxx}";
    JSONObject geoJsonData = new JSONObject(geodata);
    GeoJsonLayer layer1 = new GeoJsonLayer(getMap(), geoJsonData);

    //for closest bus
    String geodata1 = "{aa:xxxxx}";
    JSONObject geoJsonData1 = new JSONObject(geodata1);
    GeoJsonLayer layer2 = new GeoJsonLayer(getMap(), geoJsonData1);

    //for closest bus
    String geodata2 = "{aa:xxxxx}";
    JSONObject geoJsonData2 = new JSONObject(geodata2);
    GeoJsonLayer layer3 = new GeoJsonLayer(getMap(), geoJsonData2);

    //used to compute closest bus/veloh
    public float min_distance = 1000000;
    public int closest = 0;

    //booleans to reconstruct markers/circle from shared prefs
    boolean display_radius = true;
    boolean bus = true;

    //for saving markers on rotate device
    private List<Marker> markerList;
    public MapsActivity() throws JSONException {
        if(markerList == null){
            markerList = new ArrayList<Marker>();
        }
    }

    //clear the markers/poly on new action
    public final void clear() {
            mMap.clear();
        }

////// ON CREATE /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mMap = null;
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        // EDIT TEXT FIELD
        final EditText mEdit = (EditText) findViewById(R.id.distance);

        mEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    DeletePreferences();
                    clear();
                      mEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
                      Log.e("EditText", mEdit.getText().toString());
                      String value = mEdit.getText().toString();

                    try {

                        draw_dist = Integer.parseInt(value);
                        Log.e("draw_dist", Integer.toString(draw_dist));

                    } catch (NumberFormatException e){
                        Context context = getApplicationContext();
                        CharSequence text = "Please enter a number!";
                        int duration = Toast.LENGTH_SHORT;

                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }

                    InputMethodManager inputManager = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                    handled = true;
                }
                return handled;
            }
        });


        Button Velohbutton = (Button) findViewById(R.id.button_veloh);
        Velohbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bus = false;
                    clear();
                    removeGeoJsonLayerFromMap(layer1);
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


        Button ClosestVelohbutton = (Button) findViewById(R.id.button_closest_veloh);
        ClosestVelohbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    bus = false;
                    clear();
                    DeletePreferences();
                    performJSON_closest_veloh();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        Button GeoJSONbutton = (Button) findViewById(R.id.button_geoJSON);
        GeoJSONbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    bus = true;
                    clear();
                    DeletePreferences();
                    removeGeoJsonLayerFromMap(layer1);
                    removeGeoJsonLayerFromMap(layer2);
                    startDownload();
            }
        });

        Button GeoJSONbutton1 = (Button) findViewById(R.id.button_closest_bus);
        GeoJSONbutton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bus = true;
                clear();
                DeletePreferences();
                removeGeoJsonLayerFromMap(layer1);
                removeGeoJsonLayerFromMap(layer2);
                startDownload1();
            }
        });


    }


///// CLOSEST VELOH ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        String cancel = "cancel";
        Log.e("cancel", "cancel");
        toast(cancel);
        return super.dispatchTouchEvent(ev);
    }
    public void performJSON_closest_veloh() throws ExecutionException, InterruptedException, JSONException {

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
            myJson.execute("https://developer.jcdecaux.com/rest/vls/stations/Luxembourg.json");
            String result = myJson.get();
            if (result!=null) {
                Log.d("Result", result);
            }else{
                //toast
                Toast.makeText(this, "An error occurred.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray jsonArray_closest_veloh = new JSONArray(result);
            Log.d("JSON array", jsonArray_closest_veloh.toString());

            for (int i = 0; i < jsonArray_closest_veloh.length(); i++) {
                JSONObject jsonObject = jsonArray_closest_veloh.getJSONObject(i);
                double lng = Double.parseDouble(jsonArray_closest_veloh.getJSONObject(i).getString("longitude"));
                double lat = Double.parseDouble(jsonArray_closest_veloh.getJSONObject(i).getString("latitude"));

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


            JSONObject display_jsonObject = jsonArray_closest_veloh.getJSONObject(closest);
            //save
            double lng = Double.parseDouble(jsonArray_closest_veloh.getJSONObject(closest).getString("longitude"));
            double lat = Double.parseDouble(jsonArray_closest_veloh.getJSONObject(closest).getString("latitude"));
            markerList.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                    .title(display_jsonObject.getString("name"))
                    .snippet(Integer.toString((display_jsonObject.getInt("number"))))
                    .position(new LatLng(lat, lng))));
            display_radius = false;
            bus = false;

            SavePreferences();
        } //end if location null
        else{
            //This is what you need:
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        setInfoWindowListener();
    } //end json

    private void setInfoWindowListener () {

    mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                @Override
                public void onInfoWindowClick(Marker marker) {
                    Log.e("Info clicked called:", "click");
                    if (bus==false) {
                        Log.d("Requested for stop no:", marker.getSnippet());
                        try {
                            perform_real_time_veloh(marker.getSnippet());
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    }

public void perform_real_time_veloh (String station_no) throws ExecutionException, InterruptedException, JSONException {


    JSONTask myJson_Veloh_numbers = new JSONTask(this);
    myJson_Veloh_numbers.execute("https://api.jcdecaux.com/vls/v1/stations/"+station_no+"?contract=luxembourg&apiKey=730248eb8c028d2c233b519757ea8969740b39c8");
    String result = myJson_Veloh_numbers.get();
    if (result != null) {
        Log.d("Result", result);
    } else {
        Toast.makeText(this, "Error occurred.",
                Toast.LENGTH_SHORT).show();
        return;
    }

    JSONObject veloh_data = new JSONObject(result);
    String bike_stands = veloh_data.getString("bike_stands");
    String available_bikes = veloh_data.getString("available_bikes");
    Log.d("available_bikes", available_bikes);
    Toast.makeText(this, "Stands: "+bike_stands+", Available bikes: "+available_bikes,
            Toast.LENGTH_LONG).show();

}

////// VELOH STATIONS //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
            if (result!=null) {
                Log.d("Result", result);
            }else{
                Toast.makeText(this, "Error downloading data", Toast.LENGTH_LONG).show();
            }

            JSONArray jsonArray_veloh = new JSONArray(result);
            Log.d("JSON array", jsonArray_veloh.toString());
            display_radius = true;
            bus = false;
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
            //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        setInfoWindowListener();
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

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
//////// SHARED PREFERENCES ///////////////////////////////////////////////////////////////////////////////////
    private void SavePreferences(){
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();

        editor.putInt("listSize", markerList.size());
        editor.putInt("draw_dist", draw_dist);

        editor.putBoolean("display_radius", display_radius);
        editor.putBoolean("bus", bus);

        editor.putString("geojson", geodata);
        editor.putString("geojson1", geodata1);
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
        bus = sharedPreferences.getBoolean("bus", false);

        /////////recreate buses in area layer ///////////////
        geodata = sharedPreferences.getString("geojson", "{aa:xxxxx}");
        JSONObject geoJsonData1 = null;
            try {
                geoJsonData1 = new JSONObject(geodata);
            } catch (JSONException e) {
                e.printStackTrace();
            }


            GeoJsonLayer layer2 = new GeoJsonLayer(getMap(), geoJsonData1);

            if ((geodata!="{aa:xxxxx}")&&(geodata!="NULL")&&(bus)&&(display_radius)) {
                addGeoJsonLayerToMap(layer2);
            }
        // not used
        //geodata1 = sharedPreferences.getString("geojson1", "NULL");
        //// to do - what to do with this string

        //recreate marker list if needed
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                double lat = (double) sharedPreferences.getFloat("lat" + i, 0);
                double lng = (double) sharedPreferences.getFloat("lng" + i, 0);
                String title = sharedPreferences.getString("title" + i, "NULL");
                String snippet = sharedPreferences.getString("snippet" + i, "NULL");

                if ((bus)&&(display_radius==false)) {
                    markerList.add(mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                            .position(new LatLng(lat, lng))
                            .title(title)
                            .snippet(snippet)));
                    if ((bus)&&(display_radius==false)) {

                        closestBusListener ();
                    }
                }
                else {
                    markerList.add(mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                            .position(new LatLng(lat, lng))
                            .title(title)
                            .snippet(snippet)));
                    if (bus==false) {
                        setInfoWindowListener();
                    }
                }

            }

            ///recreate circle if needed
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
                    LatLng latlng = new LatLng(latitude,longitude);
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

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



                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }


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
        String cancel = "cancel";
        Log.e("cancel", "cancel");
        toast(cancel);
        super.onSaveInstanceState(savedInstanceState);
    }

    //onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        super.onRestoreInstanceState(savedInstanceState);

    }

//////// REAL TIME DEPARTURES METHODS //////////////////////////////////////
    String lines1 = " ";
public void perform_Json_departures (String stop_id) throws ExecutionException, InterruptedException, JSONException {

    Log.e("TAG_title_click", "CLICKED Title!");

    //Log.d("", marker.getTitle());

    JSONTask myJson1 = new JSONTask(this);
    myJson1.execute("https://api.tfl.lu/v1/StopPoint/Departures/"+stop_id);
    String result = myJson1.get();
    if (result!=null) {
        Log.d("Result", result);
    }else {
            Toast.makeText(this, "Error occurred.",
            Toast.LENGTH_SHORT).show();
        return;
    }
    JSONArray jsonArray_departures = new JSONArray(result);
    Log.d("JSON array", jsonArray_departures.toString());
//    Toast.makeText(this, "Departures obtained:",
//            Toast.LENGTH_SHORT).show();

    //keep only uniques bus lines in the list
    String lines = " ";
    int [] number = new int[jsonArray_departures.length()];
    String [] direction = new String[jsonArray_departures.length()];
    for (int i=0; i< jsonArray_departures.length(); i++) {
        if (jsonArray_departures.getJSONObject(i).getString("line").matches("\\d+")) {
            number[i] = Integer.parseInt(jsonArray_departures.getJSONObject(i).getString("line"));
            direction[i] = (jsonArray_departures.getJSONObject(i).getString("destination"));
            Log.e("TAG_line_departure", i+", "+Integer.toString(number[i])+" ");
        }
        else {
            number[i] = 0;
            direction[i] = "NULL";
            Log.e("TAG_line_departure", i+", "+Integer.toString(number[i])+" ");
        }
    }

    int [] uniques = new int[jsonArray_departures.length()];
    String [] directions = new String[jsonArray_departures.length()];
    int first = 0;                             // number of number in the array

    for(int i = 0; i < number.length; i++) {
         int newNum = number[i];
         String newDir = direction[i];
         boolean alreadyThere = false;           // flag not in the array
         for(int j = 0; j < first; j++) {           // loop through already registered number
             if(newNum == uniques[j]) {          // check if already there
                alreadyThere = true;               // yes it is there flag it
                break;                                      // no need to check the other
            }
        }
        if(!alreadyThere)                              // if was not there
        uniques[first] = newNum;         // add it and increment number of numbers registered
        directions[first] = newDir;         // add direction
        first=first+1;
    }

    //create string to print in toast
    for(int i= 0; i < uniques.length; i++) {
        if (uniques[i] != 0){
            lines = lines + uniques[i] + " -> " + directions[i] + " | ";
        }
    }
    lines1 = lines;
    String show = "show";
    toast(show);

}
    CountDownTimer timer = null;
    public void toast (String show_cancel) {
        final Toast tag = Toast.makeText(getBaseContext(), "Departures: "+lines1, Toast.LENGTH_SHORT);
            if (show_cancel=="show") {
                tag.show();

                timer = new CountDownTimer(9000, 500) {
                    public void onTick(long millisUntilFinished) {
                        tag.show();
                    }

                    public void onFinish() {
                        tag.show();
                    }
                }.start();
            }else{
                if ((show_cancel=="cancel")&&(timer!=null)){
                    Log.e("cancel", "cancel");
                    timer.cancel();
                }
            }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
//        Toast.makeText(this, "Info window clicked",
//                Toast.LENGTH_SHORT).show();
    }

///// GEO_JSON_BUSES_IN_AREA //////////////////////////////////////////////////////////////////////////////////////////


        private final static String mLogTag = "GeoJsonDemo";

        protected void startDownload() {
            // Download the GeoJSON file.
            retrieveFileFromUrl();
            // Alternate approach of loading a local GeoJSON file.
            //retrieveFileFromResource();
        }

        private void retrieveFileFromUrl() {

            //get user location
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

                    SavePreferences();
                } //end if location null
                else{
                    //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }

            //get file for location and specific distance
            new DownloadGeoJsonFile().execute("https://api.tfl.lu/v1/StopPoint/around/"+longitude+"/"+latitude+"/"+draw_dist);
        }
////////// if file is local //////////////
//        private void retrieveFileFromResource() {
//            try {
//                GeoJsonLayer layer = new GeoJsonLayer(getMap(), R.raw.earthquakes_with_usa, this);
//                addGeoJsonLayerToMap(layer);
//            } catch (IOException e) {
//                Log.e(mLogTag, "GeoJSON file could not be read");
//            } catch (JSONException e) {
//                Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
//            }
//        }

        /**
         * Adds a point style to all features
         */
        private void addColorsToMarkers(GeoJsonLayer layer) {
            // Iterate over all the features stored in the layer
            for (GeoJsonFeature feature : layer.getFeatures()) {
                // Check if the magnitude property exists
                if (feature.getProperty("id") != null && feature.hasProperty("name")) {
                    //double name = Double.parseDouble(feature.getProperty("name"));

                    // Get the icon for the feature
                    BitmapDescriptor pointIcon = BitmapDescriptorFactory
                            //.defaultMarker(magnitudeToColor(magnitude));
                            .defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);

                    // Create a new point style
                    GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();

                    // Set options for the point style
                    pointStyle.setIcon(pointIcon);
                    //pointStyle.setTitle("Name" + name);
                    pointStyle.setTitle("Name: " + feature.getProperty("name"));
                    pointStyle.setSnippet("ID: " + feature.getProperty("id")+", distance: "+feature.getProperty("distance")+"m");

                    // Assign the point style to the feature
                    feature.setPointStyle(pointStyle);
                }
            }
        }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


    private class DownloadGeoJsonFile extends AsyncTask<String, Void, GeoJsonLayer> {

            @Override
            protected GeoJsonLayer doInBackground(String... params) {
                try {
                    // Open a stream from the URL
                    InputStream stream = new URL(params[0]).openStream();

                    String line;
                    StringBuilder result = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                    while ((line = reader.readLine()) != null) {
                        // Read and save each line of the stream
                        result.append(line);
                    }

                    // Close the stream
                    reader.close();
                    stream.close();
                    geodata = result.toString();
                    SavePreferences();
                    Log.e("TAG saved data string:", geodata);

                    return new GeoJsonLayer(getMap(), new JSONObject(result.toString()));
                } catch (IOException e) {
                    Log.e(mLogTag, "GeoJSON file could not be read");
                } catch (JSONException e) {
                    Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
                }
                return null;
            }

            @Override
            protected void onPostExecute(GeoJsonLayer layer) {
                if (layer != null) {
                    layer1 = layer;
                    addGeoJsonLayerToMap(layer1);
                }
            }

        }

        public void removeGeoJsonLayerFromMap(GeoJsonLayer layer1){
            //layer.addLayerToMap();
            layer1.removeLayerFromMap();
        }
        private void addGeoJsonLayerToMap(GeoJsonLayer layer1) {


            ///////////////  get user lat, long and display radius circle ////////////////////////
            display_radius = true;
            bus = true;
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

                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius(draw_dist)
                        .strokeColor(Color.DKGRAY)
                        .fillColor(Color.LTGRAY));

                SavePreferences();
            } //end if location null
            else{
                //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }

            //////////// Add geoJSON markers, layer , set listener ////////////////////

            addColorsToMarkers(layer1);
            layer1.addLayerToMap();

                // Receiving features via GeoJsonLayer clicks.
                layer1.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
                    @Override
                    public void onFeatureClick(GeoJsonFeature feature) {
                        if ((bus)&&(display_radius)) {
//                            Toast.makeText(MapsActivity.this,
//                                    "Requesting departures for: " + feature.getProperty("name"),
//                                    Toast.LENGTH_SHORT).show();
                            try {
                                perform_Json_departures(feature.getProperty("id"));
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        }



    protected GoogleMap getMap() {
            return mMap;
        }

//////////////CLOSEST BUS GEOJSON //////////////////////////////////////////////////////////////

private final static String mLogTag1 = "GeoJsonDemo";

    protected void startDownload1() {
        // Download the GeoJSON file.
        retrieveFileFromUrl1();
        // Alternate approach of loading a local GeoJSON file.
        //retrieveFileFromResource();
    }

    private void retrieveFileFromUrl1() {

                //get user location
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

                    SavePreferences();
                } //end if location null
                else{
                    //locationManager.requestLocationUpdates(bestProvider, 1000, 0, LocationListener listener);
                    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                }

        //get file for location and specific distance
        new DownloadGeoJsonFile1().execute("https://api.tfl.lu/v1/StopPoint/around/"+longitude+"/"+latitude+"/100000");
    }



    private class DownloadGeoJsonFile1 extends AsyncTask<String, Void, GeoJsonLayer> {

        @Override
        protected GeoJsonLayer doInBackground(String... params) {
            try {
                // Open a stream from the URL
                InputStream stream = new URL(params[0]).openStream();

                String line;
                StringBuilder result = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

                while ((line = reader.readLine()) != null) {
                    // Read and save each line of the stream
                    result.append(line);
                }

                // Close the stream
                reader.close();
                stream.close();
                geodata1 = result.toString();
                SavePreferences();
                Log.e("TAG saved data string:", geodata1);
                return new GeoJsonLayer(getMap(), new JSONObject(result.toString()));
            } catch (IOException e) {
                Log.e(mLogTag1, "GeoJSON1 file could not be read");
            } catch (JSONException e) {
                Log.e(mLogTag1, "GeoJSON1 file could not be converted to a JSONObject");
            }
            return null;
        }

        @Override
        protected void onPostExecute(GeoJsonLayer layer) {
            if (layer != null) {
                layer2 = layer;
                addGeoJsonLayerToMap1(layer2);
            }
        }

    }

    private void addGeoJsonLayerToMap1(GeoJsonLayer layer2) {
        //////////// Add geoJSON markers, layer , set listener ////////////////////
        try {
            checkMarkers(layer2);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    int ID_kept = 0;
    private void checkMarkers(GeoJsonLayer layer2) throws JSONException {


        double distance_to = 10000000;

        // Iterate over all the features stored in the layer
        for (GeoJsonFeature feature : layer2.getFeatures()) {
            // Check if the magnitude property exists
            if (feature.getProperty("id") != null && feature.hasProperty("name")) {

                double distance_to_x = Double.parseDouble(feature.getProperty("distance"));

                if (distance_to_x < distance_to) {
                    distance_to = distance_to_x;
                    ID_kept = Integer.parseInt(feature.getProperty("id"));

                    Log.e("distance found", Double.toString(distance_to));
                    Log.e("ID:", Integer.toString(ID_kept));
                }
            }
        }


        JSONObject test = new JSONObject(geodata1);
        JSONArray feature_arr = test.getJSONArray("features");

                for (int n = 0; n<feature_arr.length(); n++) {

                    // Getting the coordinates
                    JSONObject obj_geometry = feature_arr.getJSONObject(n)
                            .getJSONObject("geometry");
                    double lon = Double.parseDouble(obj_geometry.getJSONArray("coordinates")
                            .get(0).toString());
                    double lat = Double.parseDouble(obj_geometry.getJSONArray("coordinates")
                            .get(1).toString());

                    JSONObject obj_properties = feature_arr.getJSONObject(n)
                            .getJSONObject("properties");

                    String name = obj_properties.getString("name").toString();
                    String id = obj_properties.getString("id").toString();
                    String distance = obj_properties.getString("distance").toString();
                    Log.e("properties", "lon: "+lon+" lat: "+lat+" id: "+id+" name: "+name+" distance: "+distance);
                    Log.e("ID_kept", Integer.toString(ID_kept));
                    if (Integer.parseInt(id) == ID_kept) {

                        Log.e("ID MATCH", "id: "+id+" ID_kept: "+ID_kept);
                        markerList.add(mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA))
                                .title(name+" - "+distance+"m")
                                //.snippet("ID: "+id+" Distance: " + distance)
                                .snippet(id)
                                .position(new LatLng(lat, lon))));

                        display_radius = false;
                        bus = true;
                        closestBusListener ();
                        SavePreferences();
                        break;
                    }
                }
    }

    private void closestBusListener () {
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {
                if ((bus) && (display_radius == false)) // if marker source is clicked
                {
                    Log.d("Marker click", marker.getSnippet());
                    try {
                        perform_Json_departures(marker.getSnippet());
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });

    }

}

