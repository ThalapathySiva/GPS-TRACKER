package com.example.sivaram.cranes;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import com.example.sivaram.cranes.Common.Common;
import com.example.sivaram.cranes.Remote.IGoogleApi;
import com.google.android.gms.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Welcome extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private GoogleMap mMap;
    private static final int my_permission_request=7000;
    private static final int play_service=7001;

    private  LocationRequest mlocationrequest;
    private GoogleApiClient mgoogleapiclient;
    private Location mlastlocation;

    private static int updateinterval=5000;
    private static int fatestinterval=3000;
    private static int displacement=10;
    private IGoogleApi iGoogleApi;
    private LatLng  startposition,endposition,currentposition;

    DatabaseReference drivers;
    GeoFire geoFire;
    private String destination;
    Marker mcurrent;
    MaterialAnimatedSwitch location_width;
    SupportMapFragment mapFragment;
    
    private List<LatLng> polylinelist;
    private Marker carMarker;
    private float v;
    private double lat,lon;
    private Handler handler;
    private int index,next;
    private Polyline blackpoly,greypoly;
    private PolylineOptions polylineOptions,blackPoly;

    Runnable draw=new Runnable() {
        @Override
        public void run() {
            if(index<polylinelist.size()-1){
                index++;
                next=index+1;
            }
            if (index<polylinelist.size()-1){
                startposition=polylinelist.get(index);
                endposition=polylinelist.get(next);
            }

            ValueAnimator valueAnimator =ValueAnimator.ofFloat(0,1);
            valueAnimator.setDuration(3000);
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    v=valueAnimator.getAnimatedFraction();
                    lon=v*endposition.longitude+(1-v)*startposition.longitude;
                    lat=v*endposition.latitude+(1-v)*startposition.latitude;
                    LatLng newpos =new LatLng(lat,lon);
                    carMarker.setPosition(newpos);
                    carMarker.setAnchor(0.5f,0.5f);
                    carMarker.setRotation(getBearing(startposition,newpos));
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(newpos).zoom(15.5f).build()));

                }
            });

            valueAnimator.start();
            handler.postDelayed(this,3000);
        }
    };

    private float getBearing(LatLng startposition, LatLng endposition) {
        double lat =Math.abs(startposition.latitude-endposition.latitude);
        double lon=Math.abs(startposition.longitude-endposition.longitude);

        if(startposition.latitude<endposition.latitude&&startposition.longitude<startposition.longitude)
            return (float)(Math.toDegrees(Math.atan(lon/lat)));
        else if (startposition.latitude>=endposition.latitude&&startposition.longitude<endposition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lon/lat)))+90);
        else if(startposition.latitude>=endposition.latitude&&startposition.longitude>=endposition.longitude)
            return (float)(Math.toDegrees(Math.atan(lon/lat))+180);
        else if(startposition.latitude<endposition.latitude&&startposition.longitude>=endposition.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lon/lat)))+270);
        return -1;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
         mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        location_width=findViewById(R.id.location);
        location_width.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline){
                    startLocationUpdate();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(),"You are Online",Snackbar.LENGTH_SHORT).show();
                }
                else {
                    stopLocationUpdate();
                    mcurrent.remove();
                    mMap.clear();
                    handler.removeCallbacks(draw);
                    Snackbar.make(mapFragment.getView(),"You are Offline",Snackbar.LENGTH_SHORT).show();
                }

            }
        });

        /*Button btngo=findViewById(R.id.btnGo);
        final EditText editplace=findViewById(R.id.editPlace);
        btngo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destination=editplace.getText().toString();
                destination=destination.replace(" ","+");
                Log.e("Siva",destination);
                getDirection();
            }
        });*/

        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);
        setUpLocation();
        iGoogleApi= Common.getGoogleAPI();


    }

    private void getDirection() {
        currentposition= new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
        String requestapi=null;
        try {
            requestapi="https://maps.googleapis.com/maps/api/directions/json?"+"mode=driving&"+"transit_routing_preference=less_driving&"+
                    "origin="+currentposition.latitude+","+currentposition.longitude+"&"+
                    "destination="+destination+"&"+"key="+getResources().getString(R.string.google_direction);
            Log.e("hello",requestapi);
            iGoogleApi.getpath(requestapi).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        JSONObject jsonObject= new JSONObject(response.body().toString());
                        JSONArray jsonArray=jsonObject.getJSONArray("routes");
                        for (int i=0;i<jsonArray.length();i++){
                            JSONObject route =jsonArray.getJSONObject(i);
                            JSONObject poly =route.getJSONObject("overview_polyline");
                            String polyline =poly.getString("points");
                            polylinelist=decodePoly(polyline);
                        }
                        LatLngBounds.Builder builder=new LatLngBounds.Builder();
                        for(LatLng latLng:polylinelist)
                            builder.include(latLng);
                        LatLngBounds bounds =builder.build();
                        CameraUpdate cameraUpdate =CameraUpdateFactory.newLatLngBounds(bounds,2);
                        mMap.animateCamera(cameraUpdate);

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.endCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polylinelist);
                        greypoly=mMap.addPolyline(polylineOptions);

                        blackPoly= new PolylineOptions();
                        blackPoly.color(Color.BLACK);
                        blackPoly.width(5);
                        blackPoly.startCap(new SquareCap());
                        blackPoly.endCap(new SquareCap());
                        blackPoly.jointType(JointType.ROUND);
                        blackpoly=mMap.addPolyline(blackPoly);

                        mMap.addMarker(new MarkerOptions().position(polylinelist.get(polylinelist.size()-1)).title("Pick up location"));
                        ValueAnimator polyani=ValueAnimator.ofInt(0,100);
                        polyani.setDuration(2000);
                        polyani.setInterpolator(new LinearInterpolator());
                        polyani.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                List<LatLng> points =greypoly.getPoints();
                                int percentvalue =(int)valueAnimator.getAnimatedValue();
                                int size =points.size();
                                int newpoints=(int)(size * (percentvalue/100.0f));
                                List<LatLng> p=points.subList(0,newpoints);
                                blackpoly.setPoints(p);
                            }
                        });
                        polyani.start();
                        carMarker=mMap.addMarker(new MarkerOptions().position(currentposition).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.automobile)));
                        handler=new Handler();
                        index=-1;
                        next=1;
                        handler.postDelayed(draw,3000);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {

                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void setUpLocation() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, my_permission_request);


        }
        else {
            if (checkplayservice()){
                buildGoogleApiClient();
                createLocationRequest();
                if (location_width.isChecked())
                    displayLocation();
            }
        }

    }

    private void createLocationRequest() {
        mlocationrequest= new LocationRequest();
        mlocationrequest.setInterval(updateinterval);
        mlocationrequest.setFastestInterval(fatestinterval);
        mlocationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mlocationrequest.setSmallestDisplacement(displacement);
    }

    private void buildGoogleApiClient() {

        mgoogleapiclient= new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        mgoogleapiclient.connect();
    }

    private boolean checkplayservice() {
        int resultcode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultcode != ConnectionResult.SUCCESS)
        {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultcode))
                GooglePlayServicesUtil.getErrorDialog(resultcode,this,play_service).show();
            else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;

    }

    private void stopLocationUpdate() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mgoogleapiclient, (com.google.android.gms.location.LocationListener) this);

    }

    private void displayLocation() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        mlastlocation=LocationServices.FusedLocationApi.getLastLocation(mgoogleapiclient);
        if (location_width.isChecked()){
            final double latitude=mlastlocation.getLatitude();
            final double longitude=mlastlocation.getLongitude();

            geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if(mcurrent!=null)
                        mcurrent.remove();
                    mcurrent =mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude)).title("Your location "));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),15.0f));
                }
            });
        }
        else {
            Log.e("error","cannot get location");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case my_permission_request:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    if (checkplayservice()){
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_width.isChecked())
                            displayLocation();
                    }
                }
        }
    }

    private void rotateMarker(final Marker mcurrent, final float i, GoogleMap mMap) {
        final Handler handler=new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mcurrent.getRotation();
        final long duration=1500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed =SystemClock.uptimeMillis()-start;
                float t = interpolator.getInterpolation((float)elapsed/duration);
                float rot = t*i+(1-t)*startRotation;
                mcurrent.setRotation(-rot>180?rot/2:rot);
                if (t<1.0){
                    handler.postDelayed(this,16);
                }
            }
        });

    }

    private void startLocationUpdate() {
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mgoogleapiclient,mlocationrequest, (com.google.android.gms.location.LocationListener) this);


    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    @Override
    public void onLocationChanged(Location location) {
        mlastlocation=location;
        displayLocation();

    }

   /* @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mgoogleapiclient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
