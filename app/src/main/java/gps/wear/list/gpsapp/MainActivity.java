package gps.wear.list.gpsapp;


/*
*   Created by Irene Gironacci
*   References: https://www.pubnub.com/docs/java-se-java/api-reference#init
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.pubnub.api.Pubnub;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // The minimum distance to change Updates in meters
    //private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;

    // The minimum time between updates in milliseconds
    //private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 60 minute
    private static final long MIN_TIME_BW_UPDATES = 100 * 1 * 1;

    /**
     * Overlay that shows a short help text when first launched. It also provides an option to
     * exit the app.
     */
    private DismissOverlayView mDismissOverlay;

    /**
     * The map. It is initialized when the map has been fully loaded and is ready to be used.
     *
     * @see #onMapReady(com.google.android.gms.maps.GoogleMap)
     */
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private String provider;
    double latitude; // latitude
    double longitude; // longitude
    private boolean canGetLocation;
    Location location;
    TextView mGPSLonView;
    TextView mGPSLatView;
    Marker currLocationMarker;
    //private GoogleApiClient mGoogleApiClient;
    //TextView infoip, info, msg;
    String message = "";
    ServerSocket serverSocket;
    JSONObject json = new JSONObject();
    JSONArray coord = new JSONArray();
    private String channelName = "LocationTrackingChannel";
    private Pubnub mPubnub;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        //show error dialog if GoolglePlayServices not available
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        Location loc = null;
        location = getLocation(loc);
        Log.d(String.valueOf(location), "{LOGGING} Location? ");

        mPubnub = PubNubManager.startPubnub();

        startSharingLocation();

        // Set the layout. It only contains a MapFragment and a DismissOverlay.
        setContentView(R.layout.activity_main);

        //infoip = (TextView) findViewById(R.id.infoip);

        // Socket TCP:
        //infoip.setText(getIpAddress());

        Thread socketServerThread = new Thread(new SocketServerThread());
        socketServerThread.start();
        Log.d("onCreate","{LOGGING} Thread started.");

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        // Set the system view insets on the containers when they become available.
        topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Call through to super implementation and apply insets
                insets = topFrameLayout.onApplyWindowInsets(insets);

                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                // Add Wearable insets to FrameLayout container holding map as margins
                params.setMargins(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
                mapFrameLayout.setLayoutParams(params);

                return insets;
            }
        });

        // Obtain the DismissOverlayView and display the introductory help text.
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.intro_text);
        mDismissOverlay.showIntroIfNecessary();

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Map is ready to be used.
        mMap = googleMap;
        // Set the long click listener as a way to exit the map.
        mMap.setOnMapLongClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        drawMarker(49.5058376, 5.9437973, "LIST 1", 17.0f);
        drawMarker(49.506818, 5.944064, "LIST 2", 17.0f);
        drawMarker(49.505905, 5.943209, "LIST 3", 17.0f);
        drawCurrentPosition(location, 19.0f);
    }

    public void drawMarker(double latitude, double longitude, String name, float zoom) {
        LatLng newMarker = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(newMarker).title(name));
    }


    public void drawCurrentPosition(Location location, float zoom) {
        UpdateCurrentLocation(location);
        LatLng myLocation = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(myLocation);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        currLocationMarker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));
    }

    @Override
    public void onLocationChanged(Location location) {
        UpdateCurrentLocation(location);
        String tempLocation = String.valueOf(location.getLongitude()) + " - " + String.valueOf(location.getLatitude());
        Log.d("onLocationChanged", "{LOGGING} Changed location: " + tempLocation);// object to put in array
        //buildLocationJSON(location);// Broadcast information on PubNub Channel
        PubNubManager.broadcastLocation(mPubnub, channelName, location.getLatitude(),
                location.getLongitude(), location.getAltitude());

    }

    public void buildLocationJSON(Location location){
        JSONObject coord1 = new JSONObject();
        try {
            coord1.put("lat", location.getLatitude());
            coord1.put("lng", location.getLongitude());
            // put object in array
            coord.put(coord1);
            // put array in containing object
            json.put("coord", coord);
            System.out.println(json.toString());
            //publishToPubNubChannel(location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void startSharingLocation() {
        // TODO
    }

    public void publishToPubNubChannel(Location location) {
        /*
        double temp_longitude = location.getLongitude();
        double temp_latitude = location.getLatitude();

        String subscribeChannel = String.valueOf(channelName);
        String yourSubscribeChannel = "Subscribed to the " + subscribeChannel + " Channel";
        try {
            pubnub.publish("channel_name", json, new PNCallback() {

                @Override
                public void onResponse(Object result, PNStatus status) {
                    Log.d("PUBNUB", status.toString());
                }

                @Override
                public void successCallback(String channel, Object response) {
                    Log.d("PUBNUB", response.toString());
                }

                @Override
                public void errorCallback(String channel, PubNubError error) {
                    Log.d("PUBNUB", error.toString());
                }

            });
            // NO:
            pubnub.subscribe(subscribeChannel, new Callback() {

                        @Override
                        public void connectCallback(String channel, Object message) {
                            Log.d("PUBNUB", "SUBSCRIBE : CONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }


                        @Override
                        public void disconnectCallback(String channel, Object message) {
                            Log.d("PUBNUB", "SUBSCRIBE : DISCONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        public void reconnectCallback(String channel, Object message) {
                            Log.d("PUBNUB", "SUBSCRIBE : RECONNECT on channel:" + channel
                                    + " : " + message.getClass() + " : "
                                    + message.toString());
                        }

                        //Updates textview with message
                        @Override
                        public void successCallback(String channel, Object message) {
                            Log.d("PUBNUB", "SUBSCRIBE : " + channel + " : "
                                    + message.getClass() + " : " + message.toString());
                        }

                        @Override
                        public void errorCallback(String channel, PubNubError error) {
                            Log.d("PUBNUB", "SUBSCRIBE : ERROR on channel " + channel
                                    + " : " + error.toString());
                        }

                    }
            );

        } catch (PubNubException e) {
            Log.d("PUBNUB", e.toString());
        }*/
    }

    public void UpdateCurrentLocation(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    public Location getLocation(Location location) {
        try {
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            location = null;
            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.d("No Network", "{LOGGING} !isGPSEnabled && !isNetworkEnabled");
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    Log.d("Network", "{LOGGING} Network Enabled true");
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    if (mLocationManager != null) {
                        location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        Log.d("Location Manager", "{LOGGING} mLocationManager not null");
                        if (location != null) {
                            UpdateCurrentLocation(location);
                        }
                    }
                } else {
                    Log.d("Network", "{LOGGING} Network Enabled false");
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    Log.d("isGPSEnabled", "{LOGGING} isGPSEnabled true");
                    if (location == null) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return null;
                        }
                        mLocationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (mLocationManager != null) {
                            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                UpdateCurrentLocation(location);
                            }
                        }
                    }
                } else {
                    Log.d("isGPSEnabled", "{LOGGING} isGPSEnabled false");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Display the dismiss overlay with a button to exit this activity.
        mDismissOverlay.show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Enabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates((LocationListener) this);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        Integer.parseInt(Manifest.permission.ACCESS_COARSE_LOCATION));

            } else {
                //location = mLocationManager.getLocation(provider);
            }
            //location = mLocationManager.getLastKnownLocation(provider);
        } catch (Exception e) {
            Log.e("ERROR", "ERROR IN CODE. ");
            e.printStackTrace();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(this,"onConnected",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this,"onConnectionSuspended",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this,"onConnectionFailed",Toast.LENGTH_SHORT).show();
    }

    public class SocketServerThread extends Thread {
        static final int SocketServerPORT = 8017;
        int count = 0;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                Log.d("SocketServerThread", "{LOGGING} hi on SocketServerPORT");
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                    //info.setText("I'm waiting here: "+ serverSocket.getLocalPort());
                    }
                });

                while (true) {
                    Socket socket = serverSocket.accept();
                    Log.d("SocketServerThread", "{LOGGING} accepted!");
                    count++;
                    message += "#" + count + " from " + socket.getInetAddress()
                            + ":" + socket.getPort() + "\n";

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //msg.setText(message);
                        }

                    });

                    SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
                            socket, count);
                    socketServerReplyThread.run();

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    private class SocketServerReplyThread extends Thread {

        private Socket hostThreadSocket;
        int cnt;

        SocketServerReplyThread(Socket socket, int c) {
            hostThreadSocket = socket;
            cnt = c;
        }

        @Override
        public void run() {
            while(true){
            OutputStream outputStream;
            //String msgReply = "Hello from Android, client #" + cnt + "";

                String msgReply = String.valueOf(longitude) + " " + String.valueOf(latitude);
                //String msgReply = String.valueOf(123) + " " + String.valueOf(456);

                try {

                    outputStream = hostThreadSocket.getOutputStream();
                    Log.d("SocketServerReplyThread", "{LOGGING} send location: " + msgReply);
                    PrintStream printStream = new PrintStream(outputStream);
                    printStream.print(msgReply);
                    printStream.print("\n");
                    TimeUnit.SECONDS.sleep(10);
                    //Thread.sleep(500000);
                    //PrintWriter pw = new PrintWriter(outputStream);
                    //pw.println(msgReply); // call repeatedly to send messages.


                    //printStream.close();
                    message += "Sent to Client: " + msgReply ;
                    /*
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run () {
                            //msg.setText(message);
                        }
                    });
                    */

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    message += "Something wrong! " + e.toString() + "\n";
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        //msg.setText(message);
                    }
                });
            }


        }

    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
}
