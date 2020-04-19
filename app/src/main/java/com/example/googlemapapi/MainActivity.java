package com.example.googlemapapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemapapi.NetworkRequests.OpenWeatherClient;
import com.example.googlemapapi.NetworkRequests.pojo.MainObject;
import com.example.googlemapapi.NetworkRequests.pojo.WeatherInfo;
import com.example.googlemapapi.NetworkRequests.pojo.WeatherObject;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    private GoogleMap gMap;
    private FusedLocationProviderClient mLocationClient;
    private boolean locationPermissionGranted;
    private Retrofit retrofit;
    private EditText searchText;
    private ArrayList<Marker> markers;
    private BroadcastReceiver receiver;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float ZOOM_DEFAULT = 5f;
    private static final String BASE_URL = "https://api.openweathermap.org/";
    private static final String APP_ID = "83515b4790a7f7af472dd3fd2c38636f";
    private static final String UNIT_CELSIUS = "metric";
    private static final int UPDATE_TIME = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchText = findViewById(R.id.serch_et);
        markers = new ArrayList<>();

        initSearchEditText();

        initRetrofit();

        initBroadcastReceiver();


        getLocationPermission();

        if (locationPermissionGranted) {
            getDeviceLocation();
        }

        infoUpdateInterval(UPDATE_TIME);


    }

    private void infoUpdateInterval(final int min) {
        final Handler mHandler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                makeRequestsForMarkers();
                mHandler.postDelayed(this, min * 60000);
            }
        };

        mHandler.post(runnable);
    }

    private void initBroadcastReceiver() {

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                String name = extras.getString("city");
                String description = extras.getString("msg");
                LatLng latLng = getLatLngOfCity(name);
                setAlertMarker(latLng, description);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("my.action.fbservice");

        this.registerReceiver(receiver, filter);

    }

    public void setAlertMarker(final LatLng latLng, final String description){
        OpenWeatherClient client = retrofit.create(OpenWeatherClient.class);
        double lat = latLng.latitude;
        double lon = latLng.longitude;
        Call<WeatherInfo> call = client.weatherInfoLatLon(lat, lon, APP_ID, UNIT_CELSIUS);

        call.enqueue(new Callback<WeatherInfo>() {

            @Override
            public void onResponse(Call<WeatherInfo> call, Response<WeatherInfo> response) {
                WeatherInfo info = response.body();
                double temp = info.getMain().getTemp();
                info.getWeather().setIcon("fb_img");
                info.getWeather().setDescription(description);
                addMarker(info, latLng);
                moveCamera(latLng, ZOOM_DEFAULT);
//                Toast.makeText(MainActivity.this, "Temperature: " + temp + "°C", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<WeatherInfo> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error in making request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeRequestsForMarkers() {

        OpenWeatherClient client = retrofit.create(OpenWeatherClient.class);

        if (!markers.isEmpty()){
            for (final Marker marker: markers){
                double lat = marker.getPosition().latitude;
                double lon = marker.getPosition().longitude;
                Call<WeatherInfo> call = client.weatherInfoLatLon(lat, lon, APP_ID, UNIT_CELSIUS);
                call.enqueue(new Callback<WeatherInfo>() {
                    @Override
                    public void onResponse(Call<WeatherInfo> call, Response<WeatherInfo> response) {
                        WeatherInfo info = response.body();
                        long temp = Math.round(info.getMain().getTemp());
                        marker.setSnippet(temp + "°C \n " + info.getWeather().getDescription());
                        marker.setIcon(getBitmapFromIcon(info.getWeather().getIcon()));

                    }

                    @Override
                    public void onFailure(Call<WeatherInfo> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Error in making request", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            Toast.makeText(MainActivity.this, "Info Updated!", Toast.LENGTH_SHORT).show();
        }


    }

    private void initSearchEditText() {
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH){
                    String query = searchText.getText().toString();
                    findRequestedLocation(query);

                    return true;
                }
                return false;
            }
        });


    }

    private void findRequestedLocation(String query){
        Log.d(TAG, "findRequestedLocation: Finding your city from your request");



        LatLng latLng = getLatLngOfCity(query);

        if (latLng!=null){

            double lat =  latLng.latitude;
            double lon =  latLng.longitude;

            moveCamera(new LatLng(lat, lon), ZOOM_DEFAULT);
            getTemperature(lat, lon);

            searchText.clearFocus();
            searchText.onEditorAction(EditorInfo.IME_ACTION_DONE);
        }
    }


    public LatLng getLatLngOfCity(String query){
        Geocoder geocoder = new Geocoder(this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(query, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (list.size()!=0){
            Address address = list.get(0);
            double lat =  address.getLatitude();
            double lon =  address.getLongitude();

            return new LatLng(lat, lon);
        }

        return null;
    }

    private void initRetrofit() {
        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create());

        retrofit = builder.build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                getTemperature(latLng.latitude, latLng.longitude);
            }
        });

        gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });

        gMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                Context mContext = MainActivity.this;
                LinearLayout info = new LinearLayout(mContext);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(mContext);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(mContext);
                snippet.setTextColor(Color.GRAY);
                snippet.setGravity(Gravity.CENTER);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }

    private void addMarker(WeatherInfo info, LatLng latLng){

        String icon = info.getWeather().getIcon();
        Log.d(TAG, "addMarker: icon name is " + icon);
        BitmapDescriptor smallMarkerIcon = getBitmapFromIcon(icon);

        int temp = info.getMain().getTemp().intValue();

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(smallMarkerIcon)
                .title(getCityNameFromLatLng(latLng))
                .snippet(temp+"°C \n" + info.getWeather().getDescription())
                .position(latLng);

        Marker marker = gMap.addMarker(markerOptions);
        gMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                markers.remove(marker);
                marker.remove();
            }
        });
        markers.add(marker);
        marker.showInfoWindow();
    }

    private BitmapDescriptor getBitmapFromIcon(String icon) {
        int height = 160;
        int width = 160;
        int iconId = getResources().getIdentifier(icon , "drawable", getPackageName());
        Bitmap b = BitmapFactory.decodeResource(getResources(), iconId);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        return BitmapDescriptorFactory.fromBitmap(smallMarker);
    }

    private void getTemperature(final double lat, final double lon) {
        OpenWeatherClient client = retrofit.create(OpenWeatherClient.class);
        Call<WeatherInfo> call = client.weatherInfoLatLon(lat, lon, APP_ID, UNIT_CELSIUS);


        call.enqueue(new Callback<WeatherInfo>() {

            @Override
            public void onResponse(Call<WeatherInfo> call, Response<WeatherInfo> response) {
                LatLng latLng = new LatLng(lat, lon);
                WeatherInfo info = response.body();
                addMarker(info, latLng);
//                double temp = info.getMain().getTemp();
//                Toast.makeText(MainActivity.this, "Temperature: " + temp + "°C", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<WeatherInfo> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error in making request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getDeviceLocation() {
        mLocationClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (locationPermissionGranted) {
                Task location = mLocationClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Location was found");
                            Location currentLocation = (Location) task.getResult();
                            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            moveCamera(latLng, ZOOM_DEFAULT);
                        } else {
                            Log.d(TAG, "onComplete: Could not find your location!");
                        }
                    }
                });
            }
        } catch (SecurityException ex) {
        }


    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera to Lattitude:" + latLng.latitude + " Longitude: " + latLng.longitude);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.weather_icon_simple);


//        int height = 100;
//        int width = 100;
//        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.weather_icon_simple);
//        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
//        BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);
//
//        MarkerOptions markerOptions = new MarkerOptions()
//                .icon(smallMarkerIcon)
//                .title(getCityNameFromLatLng(latLng))
//                .position(latLng);
//
//        gMap.addMarker(markerOptions);
    }


    private String getCityNameFromLatLng(LatLng latLng){

        String name = "Default city name";

        Geocoder geocoder = new Geocoder(this.getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addresses.size() > 0){
            name = addresses.get(0).getLocality();
        }
        Log.d(TAG, "getCityNameFromLatLng: " + name);

        return name == null ? "Unknown" : name;

    }

    private void getLocationPermission(){

        Log.d(TAG, "getLocationPermission: ");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){
                locationPermissionGranted = true;
                Log.d(TAG, "getLocationPermission: All permissions are granted already!");
                initializeMap();
            }else{
                Log.d(TAG, "getLocationPermission: Permissions should be requested");
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            Log.d(TAG, "getLocationPermission: Permissions should be requested");
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if (grantResults.length > 0){

                for (int i = 0; i < grantResults.length; i++){
                    if (grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                        locationPermissionGranted = false;
                        return;
                    }
                }

                locationPermissionGranted = true;
                initializeMap();

            }
        }

    }

    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);


    }
}
