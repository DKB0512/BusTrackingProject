package com.example.studentbustracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.example.studentbustracker.NotificationApp.Channel_1_ID;

public class ClientMap extends FragmentActivity implements RoutingListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    private NotificationManagerCompat manager;
    private FirebaseAuth mAuth;
    GoogleApiClient mGoogleApiClient;
    private Button mCFS, Track;
    private Switch Alwaysonbus;
    private Integer Point = 0, i = 0, z = 0;
    Location mLastLocation;
    private Integer KL = 1, DS = 0, AB = 0;
    LocationRequest mLocationRequest;
    private String Bus, Route, BusService, RouteService, userID, DriverBus, DriverRoute;
    private LatLng pickuplocation, TrackLocation, latLng;
    private boolean requestCFS = false, requestTrack = false, isLogginOut = false, Request, poly = false, boo = false;
    private Marker pickupMarker;
    String NOTIFICATION_CHANNEL_ID = "my_channel_id_01";
    private Integer Distance;
    private Integer Dis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_map);
        polylines = new ArrayList<>();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        manager = NotificationManagerCompat.from(this);

        Button logout = findViewById(R.id.Logout);

        mCFS = findViewById(R.id.CFS);

        Track = findViewById(R.id.Track);

        userID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        getClosestDriver();


        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLogginOut = true;

                disconnect();

                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(ClientMap.this, MainActivity.class));
                finish();
            }
        });

        mCFS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (requestCFS) {
                    requestCFS = false;

                    if (DLref != null) {
                        geoQueryCFS.removeAllListeners();
                        DLref.removeEventListener(DLrefListener);
                    }
                    if (driverFoundID != null) {
                        DatabaseReference Dataref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                        Dataref.setValue(true);
                        driverFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;

                    String ReqId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                    DatabaseReference Reqref = FirebaseDatabase.getInstance().getReference().child("ClientRequest");
                    GeoFire geoFire = new GeoFire(Reqref);

                    geoFire.removeLocation(ReqId);

                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    if (mMarker != null) {
                        mMarker.remove();
                    }

                    mCFS.setText("CFS");

                } else {


                    requestCFS = true;

                    String ReqId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
                    DatabaseReference Reqref = FirebaseDatabase.getInstance().getReference().child("ClientRequest");
                    GeoFire geoFire = new GeoFire(Reqref);

                    geoFire.setLocation(ReqId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    pickuplocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickuplocation).title("You are Here"));

                    Request = true;

                    getClosestDriver();
                }


            }
        });

        Track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (requestTrack) {
                    polyLines();

                    requestTrack = false;

                    driverFound = false;
                    radius = 1;

                    boo = false;

                    Track.setText("Track the Bus");

                    if (mdrivermarker != null) {
                        mdrivermarker.remove();
                    }

                } else {
                    requestTrack = true;

                    boo = true;


                    Track.setText("Looking For Driver");

                    TrackLocation = (new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

                    GetDriver();

                    Request = false;
                }
            }
        });
    }

    private int radius = 1;

    private Boolean driverFound = false;
    private String driverFoundID;


    //This is for normal Tracking
    GeoQuery geoQueryTrack;

    private void GetDriver() {
        final DatabaseReference DriverTrack = FirebaseDatabase.getInstance().getReference().child("Location").child("Driver");

        GeoFire geoFire = new GeoFire(DriverTrack);

        geoQueryTrack = geoFire.queryAtLocation(new GeoLocation(TrackLocation.latitude, TrackLocation.longitude), radius);
        geoQueryTrack.removeAllListeners();

        geoQueryTrack.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound) {
                    driverFound = true;
                    driverFoundID = key;
                    Toast.makeText(ClientMap.this, "Driver Found", Toast.LENGTH_LONG).show();

                    Track.setText("Driver Found");

                    DatabaseReference Driverref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    String CustomerId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                    trackDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius++;
                    GetDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private Marker mdrivermarker;

    private void trackDriverLocation() {

        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Location").child("Driver").child(driverFoundID).child("l");

        driverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Double> map = (List<Double>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);

                    drawRoute(driverLatLng);

                    /*getRouteToBus(driverLatLng);*/

                    if (mdrivermarker != null) {
                        mdrivermarker.remove();
                    }
                    if (AB == 1) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(locationLat, locationLng)));
                    } else {
                        while (z == 0) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude())));
                            z++;
                        }
                    }
                    if (boo) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mdrivermarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Bus"));
                        } else {
                            mdrivermarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Bus"));
                        }
                    } else if (!boo) {
                        if (mdrivermarker != null) {
                            mdrivermarker.remove();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (mdrivermarker != null) {
                    mdrivermarker.remove();
                }
            }
        });
    }

    private void drawRoute(LatLng driverLatlng) {
            mMap.addPolyline(
                    new PolylineOptions()
                            .add(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), driverLatlng)
                            .width(9)
                            .color(Color.BLUE)
            );
    }


    private void getRouteToBus(LatLng driverLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), driverLatLng)
                .key("AIzaSyD4UDbB74HVmOyOYWl-6YSNiOgAwbrNUYc")
                .build();
        routing.execute();
    }


    DatabaseReference Df = FirebaseDatabase.getInstance().getReference().child("Location").child("Driver");


    //This is for CFS

    GeoQuery geoQueryCFS;

    private void getClosestDriver() {
        if (Request) {
            DatabaseReference DriverLocation = FirebaseDatabase.getInstance().getReference().child("Location").child("Driver");

            GeoFire geoFire = new GeoFire(DriverLocation);

            geoQueryCFS = geoFire.queryAtLocation(new GeoLocation(pickuplocation.latitude, pickuplocation.longitude), radius);
            geoQueryCFS.removeAllListeners();

            geoQueryCFS.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (!driverFound && requestCFS) {
                        driverFound = true;
                        driverFoundID = key;
                        Toast.makeText(getApplicationContext(), "Driver Found", Toast.LENGTH_SHORT).show();

                        String ClientId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                        DatabaseReference Dataref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);


                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("ClientRiderId", ClientId);
                        Dataref.updateChildren(map);

                        getDriverLocation();
                    }
                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                }

                @Override
                public void onGeoQueryReady() {
                    if (!driverFound) {
                        radius++;
                        getClosestDriver();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });
        }
    }

    //This is for CFS
    Marker mMarker;

    private DatabaseReference DLref;
    private ValueEventListener DLrefListener;

    private void getDriverLocation() {
        DLref = FirebaseDatabase.getInstance().getReference().child("DriverCFS").child(driverFoundID).child("l");
        DLrefListener = DLref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestCFS) {
                    List<Double> map = (List<Double>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mCFS.setText("Done");

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }

                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng DriverLatLng = new LatLng(locationLat, locationLng);
                    if (mMarker != null) {
                        mMarker.remove();
                    }
                    mMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Bus"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildgoogleapiclient();
    }

    protected synchronized void buildgoogleapiclient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            mLastLocation = location;

            if (KL > 0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15.5f));
                KL = 0;
            }


            String UserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Location").child("Client");

            GeoFire geoFire = new GeoFire(ref);
            geoFire.setLocation(UserId, new GeoLocation(location.getLatitude(), location.getLongitude()));


        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, ClientMap.this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void disconnect() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, ClientMap.this);
        String UserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Location").child("Client");
        DatabaseReference Tref = FirebaseDatabase.getInstance().getReference().child("ClientRequest");
        DatabaseReference Stu = FirebaseDatabase.getInstance().getReference().child("Users").child("Students");

        Stu.removeValue();

        GeoFire geoFire = new GeoFire(ref);
        GeoFire TgeoFire = new GeoFire(Tref);
        geoFire.removeLocation(UserId);
        TgeoFire.removeLocation(UserId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLogginOut) {
            disconnect();
            radius = 1;
        }
    }


    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.colorAccent};


    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {
    }

    @Override
    public void onRoutingSuccess(ArrayList<com.directions.route.Route> route, int shortestRouteIndex) {

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            //Zooming as per the Polyline

            zoomRoute(route.get(i).getPoints());

            //Saving the Duration of Bus to the Current Users in Seconds

            Distance = route.get(i).getDurationValue();

            //Showing the Distance variable in a marker's snippet with the / 60 to get Minutes

            mdrivermarker.setSnippet("ETA : " + route.get(i).getDurationValue() / 60 + " Minutes & " + "Distance : " + route.get(i).getDistanceValue() / 1000 + " Kms");
            mdrivermarker.showInfoWindow();

            Dis = route.get(i).getDistanceValue() / 1000;

            if (Dis < 20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    API25();
                }
            }

            //To display the necessary stuff

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {
    }

    private void polyLines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    @Override
    public void onBackPressed() {
        String SID = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference Recall = FirebaseDatabase.getInstance().getReference().child("Users").child("Students").child(SID).child("Selection");
        Recall.setValue(null);
        if (driverFound) {
            DatabaseReference Dataref = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
            Dataref.setValue(true);
        }
        Intent intent = new Intent(ClientMap.this, SelectionActivity.class);
        startActivity(intent);
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (mMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        mMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    /*
    public void NotificationChannel1(){
        Notification notification = new NotificationCompat.Builder(this, Channel_1_ID)
                .setContentTitle("Bus is near")
                .setContentText("Bus is less than 1 KM away")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        manager.notify(1, notification);
    }
    */


    public void API25() {
        NotificationCompat.Builder mbuilder1 = (NotificationCompat.Builder) new NotificationCompat.Builder(this, "1")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Bus is Near")
                .setContentText("Bus is within 1 KM")
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        manager.notify(1, mbuilder1.build());
    }

}

