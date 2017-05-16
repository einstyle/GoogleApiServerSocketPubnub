package gps.wear.list.gpsapp; /**
 * Created by target001 on 16/05/2017.
 */

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;
//import com.pubnub.api.callbacks.PNCallback;

public class PubNubManager {

    public final static String TAG = "PUBNUB";
    private static String PUBNUB_SUBSCRIBE_KEY = "YOUR_SUB_KEY";
    private static String PUBNUB_PUBLISH_KEY ="YOUR_PUB_KEY";
    public static final String DATASTREAM_PREFS = "com.pubnub.example.android.datastream.mapexample.DATASTREAM_PREFS";
    public static final String DATASTREAM_UUID = "com.pubnub.example.android.datastream.mapexample.DATASTREAM_UUID";
    public static final String PUBLISH_KEY = "YOUR_PUB_KEY";
    public static final String SUBSCRIBE_KEY = "YOUR_SUB_KEY";
    public static final String CHANNEL_NAME = "maps-channel";

    public static Pubnub startPubnub() {
        Log.d(TAG, "Initializing PubNub");
        return new Pubnub(PUBNUB_PUBLISH_KEY, PUBNUB_SUBSCRIBE_KEY);
    }

    public static void subscribe(Pubnub mPubnub, String channelName, Callback subscribeCallback) {
        // Subscribe to channel
        try {
            mPubnub.subscribe("LocationTrackingChannel", subscribeCallback);
            Log.d(TAG, "Subscribed to Channel");
        } catch (PubnubException e) {
            Log.e(TAG, e.toString());
        }
    }

    public static void broadcastLocation(Pubnub pubnub, String channelName, double latitude,
                                         double longitude, double altitude) {
        JSONObject message = new JSONObject();
        try {
            message.put("lat", latitude);
            message.put("lng", longitude);
            message.put("alt", altitude);
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }
        Log.d(TAG, "Sending JSON Message: " + message.toString());
        pubnub.publish("location", message, publishCallback);
    }

    public static Callback publishCallback = new Callback() {

        @Override
        public void successCallback(String channel, Object response) {
            Log.d("PUBNUB", "Sent Message: " + response.toString());
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            Log.d("PUBNUB", error.toString());
        }
    };
}