package com.wayneyong.poibrowser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.wayneyong.poibrowser.databinding.ActivityMapsBinding;
import com.wayneyong.poibrowser.network.GeocodeResponse;
import com.wayneyong.poibrowser.network.RetrofitInterface;
import com.wayneyong.poibrowser.network.geocode.Location;
import com.wayneyong.poibrowser.network.geocode.Result;
import com.wayneyong.poibrowser.utils.PermissionCheck;
import com.wayneyong.poibrowser.utils.UtilsCheck;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener
        , GoogleApiClient.ConnectionCallbacks, GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener{


    private ActivityMapsBinding binding;
    private final static String TAG = "MapsActivity";

    SharedPreferences getPrefs;
    boolean isFirstStart;
    private GoogleApiClient googleApiClient;
    private GoogleMap mMap;
    private Location location;
    private Marker RevMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        PermissionCheck.initialPermissionCheckAll(this, this);
        Init_intro();
        binding.progressBarMaps.setVisibility(View.GONE);

        if (!UtilsCheck.isNetworkConnected(this)) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.main_content),
                    "Turn Internet On", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }


        binding.arNavBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, NavActivity.class);
                startActivity(intent);
                Log.e(TAG, "arNavBtn clicked");

            }
        });

        binding.poiBrowserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapsActivity.this, PoiBrowserActivity.class);
                startActivity(intent);

                Log.e(TAG, "poiBrowserBtn clicked");
                Timber.d("poiBrowserBtn clicked");
            }
        });


//        binding.aboutBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MapsActivity.this, AboutActivity.class);
//                startActivity(intent);
//                Timber.d("aboutBtn clicked");
//                Log.e(TAG, "aboutBtn clicked");
//
//            }
//        });

        binding.decodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (TextUtils.isEmpty(binding.decodeBox.getText())) {
                        Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                                "Search Field is Empty", Snackbar.LENGTH_SHORT);
                        mySnackbar.show();
                        Timber.d("Search Field is Empty");
                        Log.e(TAG, "Search Field is Empty");
                    } else {
                        Geocode_Call(binding.decodeBox.getText().toString());
                        Timber.d("decodeBtn:L calling geocode_call function");
                        Log.e(TAG, "decodeBtn:L calling geocode_call function");


                    }
                } catch (NullPointerException npe) {
                    Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_content),
                            "Search Field is Empty", Snackbar.LENGTH_SHORT);
                    mySnackbar.show();
                    Timber.e("decodeBtn:L NullPointerException npe");
                    Log.e(TAG, "decodeBtn:L NullPointerException npe");


                }
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    void Init_intro() {

        Timber.i("Heading to app intro");
        Log.e(TAG, "Heading to app intro");

        getPrefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());

        //  Create a new boolean and preference and set it to true
        isFirstStart = getPrefs.getBoolean("firstStart", true);

        //  Declare a new thread to do a preference check
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                //  If the activity has never started before...
//                if (isFirstStart) {

                //  Launch app intro
                Intent i = new Intent(MapsActivity.this, DefaultIntro.class);
                startActivity(i);

                //  Make a new preferences editor
                SharedPreferences.Editor e = getPrefs.edit();

                //  Edit preference to make it false because we don't want this to run again
                e.putBoolean("firstStart", false);

                //  Apply changes
                e.apply();
//                }
            }
            //}
        });

        if (isFirstStart) {
            t.start();
        }
    }

    void Geocode_Call(String address) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        binding.progressBarMaps.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<GeocodeResponse> call = apiService.getGecodeData(address,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {

                binding.progressBarMaps.setVisibility(View.GONE);

//                List<Result> results = response.body().getResults();

                List<Result> results = response.body().getResults();
                location = results.get(0).getGeometry().getLocation();
                Toast.makeText(MapsActivity.this, location.getLat() + "," + location.getLng(), Toast.LENGTH_SHORT).show();

                try {
                    mMap.clear();
                    LatLng loc = new LatLng(location.getLat(), location.getLng());
                    mMap.addMarker(new MarkerOptions()
                            .position(loc)
                            .title(results.get(0).getFormattedAddress())
                            .snippet(results.get(0).getGeometry().getLocationType()));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14.0f));
                    mMap.getUiSettings().setMapToolbarEnabled(false);
                    //decode_button.setBackground(getDrawable());

//                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//                        @Override
//                        public boolean onMarkerClick(Marker marker) {
//                            if(marker.isInfoWindowShown())
//                                fab_menu.hideMenu(true);
//                            else
//                                fab_menu.hideMenu(false);
//                            return false;
//                        }
//                    });
                } catch (Exception e) {
                    Log.e(TAG, "onMapReady: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {
                binding.progressBarMaps.setVisibility(View.GONE);
                Toast.makeText(MapsActivity.this, "Invalid Request", Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 10: {
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.e(TAG, "calling onMapReady");
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

            if (!success) {
                Log.e(TAG, "Style parseing failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Cant find style" + e.getMessage());
        }

        mMap.setOnMapLongClickListener(this);
        Log.e(TAG, "onMapReady: MAP IS ready");
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        Timber.tag(TAG).d("onMapClick: Short Click " + latLng.toString());
        Log.e(TAG, "onMapClick:Short Click " + latLng.toString());

    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();

        RevMarker = mMap.addMarker(new MarkerOptions().position(latLng));
        Toast.makeText(this, latLng.latitude + " " + latLng.longitude, Toast.LENGTH_SHORT).show();
        Timber.d("onMapLongClick called: passing location: " + latLng.latitude + " " + latLng.longitude);
        Log.e(TAG, "onMapLongClick called: passing location: " + latLng.latitude + " " + latLng.longitude);


        Rev_Geocode_Call(latLng);
    }

    private void Rev_Geocode_Call(LatLng latLng) {

        Timber.d("Rev_Geocode_Call initiated");
        Log.e(TAG, "Rev_Geocode_Call initiated");

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        binding.progressBarMaps.setVisibility(View.VISIBLE);

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<GeocodeResponse> call = apiService.getRevGecodeData(latLng.latitude + "," + latLng.longitude,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<GeocodeResponse>() {
            @Override
            public void onResponse(Call<GeocodeResponse> call, Response<GeocodeResponse> response) {

                binding.progressBarMaps.setVisibility(View.GONE);
                List<Result> results = response.body().getResults();
                String address = results.get(0).getFormattedAddress();
                Toast.makeText(MapsActivity.this, address, Toast.LENGTH_SHORT).show();

                RevMarker.setTitle(address);
                RevMarker.setSnippet(results.get(0).getGeometry().getLocationType());
//                try{
//
//                }catch (NullPointerException npe){
//                    Log.d(TAG, "onMapReady: Location is NULL");
//                }
            }

            @Override
            public void onFailure(Call<GeocodeResponse> call, Throwable t) {
                binding.progressBarMaps.setVisibility(View.GONE);
                Toast.makeText(MapsActivity.this, "Invalid Request", Toast.LENGTH_SHORT).show();
                Timber.e("Rev_Geocode_Call failed");
                Log.e(TAG, "Rev_Geocode_Call failed");
            }
        });

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}