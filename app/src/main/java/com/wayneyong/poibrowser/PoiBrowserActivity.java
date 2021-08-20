package com.wayneyong.poibrowser;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.SphericalUtil;
import com.wayneyong.poibrowser.ar.ArBeyondarGLSurfaceView;
import com.wayneyong.poibrowser.ar.ArFragmentSupport;
import com.wayneyong.poibrowser.ar.OnTouchBeyondarViewListenerMod;
import com.wayneyong.poibrowser.databinding.ActivityPoiBrowserBinding;
import com.wayneyong.poibrowser.network.PlaceResponse;
import com.wayneyong.poibrowser.network.PoiResponse;
import com.wayneyong.poibrowser.network.RetrofitInterface;
import com.wayneyong.poibrowser.network.place.PlaceResult;
import com.wayneyong.poibrowser.network.poi.Result;
import com.wayneyong.poibrowser.utils.UtilsCheck;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class PoiBrowserActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener, OnClickBeyondarObjectListener,
        OnTouchBeyondarViewListenerMod {

    private ActivityPoiBrowserBinding binding;
    private final static String TAG = "PoiBrowserActivity";
    private TextView textView;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private LayoutInflater layoutInflater;
    private ArFragmentSupport arFragmentSupport;
    private World world;

//    @BindView(R.id.poi_place_detail)
//    CardView poi_place_detail;
//
//    @BindView(R.id.poi_place_close_btn)
//    ImageButton poi_place_close_btn;
//
//    @BindView(R.id.poi_place_name)
//    TextView poi_place_name;
//
//    @BindView(R.id.poi_place_address)
//    TextView poi_place_address;
//
//    @BindView(R.id.poi_place_image)
//    ImageView poi_place_image;
//
//    @BindView(R.id.poi_place_ar_direction)
//    Button poi_place_ar_direction;
//
//    @BindView(R.id.poi_place_maps_direction)
//    Button poi_place_maps_direction;
//
//    @BindView(R.id.poi_brwoser_progress)
//    ProgressBar poi_browser_progress;
//
//    @BindView(R.id.seekBar)
//    SeekBar seekbar;
//
//    @BindView(R.id.seekbar_cardview)
//    CardView seekbar_cardview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPoiBrowserBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

//        setContentView(R.layout.activity_poi_browser);
//        ButterKnife.bind(this);

        binding.seekbarCardview.setVisibility(View.GONE);
        binding.poiBrwoserProgress.setVisibility(View.GONE);
        binding.poiPlaceDetail.setVisibility(View.GONE);

        if (!UtilsCheck.isNetworkConnected(this)) {
            Snackbar mySnackbar = Snackbar.make(findViewById(R.id.poi_layout),
                    "Turn Internet On", Snackbar.LENGTH_SHORT);
            mySnackbar.show();
        }

        arFragmentSupport = (ArFragmentSupport) getSupportFragmentManager().findFragmentById(
                R.id.poi_cam_fragment);
        arFragmentSupport.setOnClickBeyondarObjectListener(this);
        arFragmentSupport.setOnTouchBeyondarViewListener(this);


        textView = (TextView) findViewById(R.id.loading_text);

        Set_googleApiClient(); //Sets the GoogleApiClient

        binding.poiPlaceCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.seekbarCardview.setVisibility(View.VISIBLE);
                binding.poiPlaceDetail.setVisibility(View.GONE);
                binding.poiPlaceImage.setImageResource(android.R.color.transparent);
                binding.poiPlaceName.setText(" ");
                binding.poiPlaceAddress.setText(" ");
            }
        });

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                if (i == 0) {
                    Poi_list_call(300);
                } else {
                    Poi_list_call((i + 1) * 300);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() == 0) {
                    Toast.makeText(PoiBrowserActivity.this, "Radius: 300 Metres", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PoiBrowserActivity.this, "Radius: " + (seekBar.getProgress() + 1) * 300 + " Metres", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void Poi_list_call(int radius) {

        binding.poiBrwoserProgress.setVisibility(View.VISIBLE);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<PoiResponse> call = apiService.listPOI(String.valueOf(mLastLocation.getLatitude()) + "," +
                        String.valueOf(mLastLocation.getLongitude()), radius,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<PoiResponse>() {
            @Override
            public void onResponse(Call<PoiResponse> call, Response<PoiResponse> response) {

                binding.poiBrwoserProgress.setVisibility(View.GONE);
                binding.seekbarCardview.setVisibility(View.VISIBLE);

                List<Result> poiResult = response.body().getResults();

                Configure_AR(poiResult);
            }

            @Override
            public void onFailure(Call<PoiResponse> call, Throwable t) {
                binding.poiBrwoserProgress.setVisibility(View.GONE);
            }
        });

    }

    void Poi_details_call(String placeid) {

        binding.poiBrwoserProgress.setVisibility(View.VISIBLE);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.directions_base_url))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RetrofitInterface apiService =
                retrofit.create(RetrofitInterface.class);

        final Call<PlaceResponse> call = apiService.getPlaceDetail(placeid,
                getResources().getString(R.string.google_maps_key));

        call.enqueue(new Callback<PlaceResponse>() {
            @Override
            public void onResponse(Call<PlaceResponse> call, Response<PlaceResponse> response) {

                binding.seekbarCardview.setVisibility(View.GONE);
                binding.poiPlaceDetail.setVisibility(View.VISIBLE);
                binding.poiBrwoserProgress.setVisibility(View.GONE);

//                final PlaceResult result=response.body().getResult();

                final PlaceResult placeResult = response.body().getResult();
                binding.poiPlaceName.setText(placeResult.getName());
                binding.poiPlaceAddress.setText(placeResult.getFormattedAddress());


                try {
                    HttpUrl url = new HttpUrl.Builder()
                            .scheme("https")
                            .host("maps.googleapis.com")
                            .addPathSegments("maps/api/place/photo")
                            .addQueryParameter("maxwidth", "400")
                            .addQueryParameter("photoreference", placeResult.getPhotos().get(0).getPhotoReference())
                            .addQueryParameter("key", getResources().getString(R.string.google_maps_key))
                            .build();

                    new PoiPhotoAsync().execute(url.toString());

                } catch (Exception e) {
                    Log.e(TAG, "onResponse: " + e.getMessage());
                    Toast.makeText(PoiBrowserActivity.this, "No image available", Toast.LENGTH_SHORT).show();
                }

                binding.poiPlaceMapsDirection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent;
                        try {
                            Uri.Builder builder = new Uri.Builder();
                            builder.scheme("http")
                                    .authority("maps.google.com")
                                    .appendPath("maps")
                                    .appendQueryParameter("saddr", mLastLocation.getLatitude() + "," + mLastLocation.getLongitude())
                                    .appendQueryParameter("daddr", placeResult.getGeometry().getLocation().getLat() + "," +
                                            placeResult.getGeometry().getLocation().getLng());

                            intent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse(builder.build().toString()));
                            startActivity(intent);
                            finish();
                        } catch (Exception e) {
                            Log.d(TAG, "onClick: mapNav Exception caught");
                            Toast.makeText(PoiBrowserActivity.this, "Unable to Open Maps Navigation", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                binding.poiPlaceArDirection.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(PoiBrowserActivity.this, ArCamActivity.class);

                        try {
                            intent.putExtra("SRC", "Current Location");
                            intent.putExtra("DEST", placeResult.getGeometry().getLocation().getLat() + "," +
                                    placeResult.getGeometry().getLocation().getLng());
                            intent.putExtra("SRCLATLNG", mLastLocation.getLatitude() + "," + mLastLocation.getLongitude());
                            intent.putExtra("DESTLATLNG", placeResult.getGeometry().getLocation().getLat() + "," +
                                    placeResult.getGeometry().getLocation().getLng());
                            startActivity(intent);
                            finish();
                        } catch (NullPointerException npe) {
                            Log.d(TAG, "onClick: The IntentExtras are Empty");
                        }
                    }
                });

            }

            @Override
            public void onFailure(Call<PlaceResponse> call, Throwable t) {
                binding.poiBrwoserProgress.setVisibility(View.GONE);
            }
        });

    }

    public class PoiPhotoAsync extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            binding.poiPlaceImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.poiPlaceImage.setImageBitmap(bitmap);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageURL = urls[0];

            Bitmap bitmap = null;
            try {
                InputStream input = new java.net.URL(imageURL).openStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }
    }


    private void Configure_AR(List<Result> pois) {

        layoutInflater = getLayoutInflater();

        world = new World(getApplicationContext());
        world.setGeoPosition(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        world.setDefaultImage(R.drawable.ar_sphere_default);

        arFragmentSupport.getGLSurfaceView().setPullCloserDistance(25);

        GeoObject geoObjects[] = new GeoObject[pois.size()];

        for (int i = 0; i < pois.size(); i++) {
            GeoObject poiGeoObj = new GeoObject(1000 * (i + 1));
            //ArObject2 poiGeoObj=new ArObject2(1000*(i+1));

//            poiGeoObj.setImageUri(getImageUri(this,textAsBitmap(pois.get(i).getName(),10.0f, Color.WHITE)));
            poiGeoObj.setGeoPosition(pois.get(i).getGeometry().getLocation().getLat(),
                    pois.get(i).getGeometry().getLocation().getLng());
            poiGeoObj.setName(pois.get(i).getPlaceId());
            //poiGeoObj.setPlaceId(pois.get(0).getPlaceId());

            //Bitmap bitmap=textAsBitmap(pois.get(i).getName(),30.0f,Color.WHITE);

            Bitmap snapshot = null;
            View view = getLayoutInflater().inflate(R.layout.poi_container, null);
            TextView name = (TextView) view.findViewById(R.id.poi_container_name);
            TextView dist = (TextView) view.findViewById(R.id.poi_container_dist);
            ImageView icon = (ImageView) view.findViewById(R.id.poi_container_icon);

            name.setText(pois.get(i).getName());
            String distance = String.valueOf((SphericalUtil.computeDistanceBetween(
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()),
                    new LatLng(pois.get(i).getGeometry().getLocation().getLat(),
                            pois.get(i).getGeometry().getLocation().getLng()))) / 1000);
            String d = distance + " KM";
            dist.setText(d);

            String type = pois.get(i).getTypes().get(0);
            Log.d(TAG, "Configure_AR: TYPE:" + type + "LODGING:" + R.string.logding);

            if (type.equals("subway_station")) {

                icon.setImageResource(R.drawable.ic_city_railway_station);

            } else if (type.equals("light_rail_station")) {

                icon.setImageResource(R.drawable.ic_subway);

            } else if (type.equals("transit_station")) {

                icon.setImageResource(R.drawable.ic_bus_station);

            } else if (type.equals("train_station")) {

                icon.setImageResource(R.drawable.ic_train_station);

            } else if (type.equals("bus_station")) {

                icon.setImageResource(R.drawable.ic_train);

            } else if (type.equals("taxi_stand")) {

                icon.setImageResource(R.drawable.ic_taxi_stand);

            } else if (type.equals("car_rental")) {

                icon.setImageResource(R.drawable.ic_car_rental);

            } else if (type.equals("airport")) {

                icon.setImageResource(R.drawable.ic_airport);


            } else {
                icon.setImageResource(R.drawable.map_icon);
            }


            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);

            try {
                //  Paint paint = new Paint(ANTI_ALIAS_FLAG);
//                paint.setTextSize(textSize);
//                paint.setColor(textColor);
                //paint.setTextAlign(Paint.Align.LEFT);
//                float baseline = -paint.ascent(); // ascent() is negative
//                int width = (int) (paint.measureText(pois.get(i).getName()) + 0.5f); // round
//                int height = (int) (baseline + paint.descent() + 0.5f);

                view.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                snapshot = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight()
                        , Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(snapshot);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                view.draw(canvas);
                //canvas.drawBitmap(snapshot);
                //snapshot = Bitmap.createBitmap(view.getDrawingCache(),10,10,200,100); // You can tell how to crop the snapshot and whatever in this method
            } finally {
                view.setDrawingCacheEnabled(false);
            }


            String uri = saveToInternalStorage(snapshot, pois.get(i).getId() + ".png");

            //icon.setImageURI(Uri.parse(uri));
            poiGeoObj.setImageUri(uri);

            world.addBeyondarObject(poiGeoObj);
        }

        textView.setVisibility(View.INVISIBLE);

        // ... and send it to the fragment
        arFragmentSupport.setWorld(world);

    }

    private String saveToInternalStorage(Bitmap bitmapImage, String name) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, name);

        Log.d(TAG, "saveToInternalStorage: PATH:" + mypath.toString());

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mypath.toString();

    }

    public String getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path).toString();


//        ContextWrapper cw = new ContextWrapper(getApplicationContext());
//        // path to /data/data/yourapp/app_data/imageDir
//        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
//        // Create imageDir
//        File mypath=new File(directory,"profile.jpg");
//
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(mypath);
//            // Use the compress method on the BitMap object to write image to the OutputStream
//            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        return directory.getAbsolutePath();
    }


    public Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }


    private void Set_googleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                try {
                    Poi_list_call(900);
                } catch (Exception e) {
                    Log.d(TAG, "onCreate: Intent Error");
                }
            }
        }
    }

    @Override
    public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
        if (beyondarObjects.size() > 0) {
            Poi_details_call(beyondarObjects.get(0).getName());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onTouchBeyondarView(MotionEvent event, ArBeyondarGLSurfaceView var2) {
        float x = event.getX();
        float y = event.getY();

        ArrayList<BeyondarObject> geoObjects = new ArrayList<BeyondarObject>();

        // This method call is better to don't do it in the UI thread!
        // This method is also available in the BeyondarFragment
        var2.getBeyondarObjectsOnScreenCoordinates(x, y, geoObjects);

        String textEvent = "";
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                textEvent = "Event type ACTION_DOWN: ";
                break;
            case MotionEvent.ACTION_UP:
                textEvent = "Event type ACTION_UP: ";
                break;
            case MotionEvent.ACTION_MOVE:
                textEvent = "Event type ACTION_MOVE: ";
                break;
            default:
                break;
        }

        Iterator<BeyondarObject> iterator = geoObjects.iterator();
        while (iterator.hasNext()) {
            BeyondarObject geoObject = iterator.next();
            textEvent = textEvent + " " + geoObject.getName();
            Log.d(TAG, "onTouchBeyondarView: ATTENTION !!! " + textEvent);

            // ...
            // Do something
            // ...
        }
    }

}