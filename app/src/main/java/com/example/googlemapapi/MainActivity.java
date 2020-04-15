package com.example.googlemapapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemapapi.NetworkRequests.OpenWeatherClient;
import com.example.googlemapapi.NetworkRequests.pojo.WeatherInfo;
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

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float ZOOM_DEFAULT = 5f;
    private static final String BASE_URL = "https://api.openweathermap.org/";
    private static final String APP_ID = "83515b4790a7f7af472dd3fd2c38636f";
    private static final String UNIT_CELSIUS = "metric";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchText = findViewById(R.id.serch_et);
        markers = new ArrayList<>();

        initSearchEditText();

        initRetrofit();


        getLocationPermission();

        if (locationPermissionGranted) {
            getDeviceLocation();
        }




    }

    private void initSearchEditText() {
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH){

                    findRequestedLocation();

                    return true;
                }
                return false;
            }
        });


    }

    private void findRequestedLocation(){
        Log.d(TAG, "findRequestedLocation: Finding your city from your request");

        String query = searchText.getText().toString();

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

            moveCamera(new LatLng(lat, lon), ZOOM_DEFAULT);
            getTemperature(lat, lon);

            searchText.clearFocus();
            searchText.onEditorAction(EditorInfo.IME_ACTION_DONE);
        }
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
                marker.remove();
                return true;
            }
        });
    }

    private void addMarker(WeatherInfo info, LatLng latLng){
        int height = 130;
        int width = 130;
        String icon = info.getWeather().getIcon();
        int iconId = getResources().getIdentifier(icon , "drawable", getPackageName());
        Bitmap b = BitmapFactory.decodeResource(getResources(), iconId);
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);
        BitmapDescriptor smallMarkerIcon = BitmapDescriptorFactory.fromBitmap(smallMarker);

        int temp = info.getMain().getTemp().intValue();

        MarkerOptions markerOptions = new MarkerOptions()
                .icon(smallMarkerIcon)
                .title(""+temp+"°C")
                .snippet(info.getWeather().getDescription())
                .position(latLng);

        Marker marker = gMap.addMarker(markerOptions);
        gMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.remove();

            }
        });
        markers.add(marker);
        marker.showInfoWindow();
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
                double temp = info.getMain().getTemp();
                Toast.makeText(MainActivity.this, "Temperature: " + temp + "°C", Toast.LENGTH_SHORT).show();
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

        return name;

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
