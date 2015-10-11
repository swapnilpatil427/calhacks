package com.example.swap.blind;
import java.io.FilterReader;
import java.lang.*;

import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.widget.AdapterView.OnItemClickListener;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;

import com.google.gson.*;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.search.Address;
import android.os.StrictMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import com.google.android.gms.maps.*;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String LOG_TAG = "Google Places Autocomplete";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static final String API_KEY = "AIzaSyBY-cp1LFymi4qew5_kY4ixWgtUcnV8UTQ";
    private static TextToSpeech tts;
    private Map map = null;

    private TextView txtSpeechInput;
    private Button btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;


private  String text="";
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    // TextView for displaying the current map scheme
    private TextView textViewResult = null;
    private AutoCompleteTextView autoCompView;
    // MapRoute for this activity
    private static MapRoute mapRoute = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);
        setContentView(R.layout.activity_main);
        text = "Welcome, Please Enter the Location you want to go to";
        speakOut();
       autoCompView = (AutoCompleteTextView) findViewById(R.id.editText);
        Button btn = (Button) findViewById(R.id.button);

        txtSpeechInput = (TextView) findViewById(R.id.textView);
        btnSpeak = (Button) findViewById(R.id.record);

        // hide the action bar
       // getActionBar().hide();
        promptSpeechInput();
//        btnSpeak.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                promptSpeechInput();
//            }
//        });



        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getDirections(v);
                //myClick(v); /* my method to call new intent or activity */
            }
        });
        autoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, android.R.layout.simple_list_item_1));
        autoCompView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                autoCompView.setText((String) parent.getItemAtPosition(position));
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    autoCompView.setText(result.get(0).toCharArray(),0,result.get(0).length());
                  //  txtSpeechInput.setText(result.get(0));
                }
                break;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (Exception a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }



    //////////////////////////////////////////
// map embedded in the map fragment
    private void speakOut() {

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void getDirections(View view) {
        // 1. clear previous results
//        textViewResult.setText("");
//        if (map != null && mapRoute != null) {
//            map.removeMapObject(mapRoute);
//            mapRoute = null;
//        }
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Set the map center coordinate to the Vancouver region (no animation)
                    map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                            Map.Animation.NONE);
                    // Set the map zoom level to the average between min and max (no animation)
                    map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
                } else {
                    //Log.e(LOG_TAG, "Cannot initialize MapFragment (" + error + ")");
                }
            }
        });

        textViewResult = (TextView) findViewById(R.id.title);
        // textViewResult.setText(R.string.textview_routecoordinates_2waypoints);
        // 2. Initialize RouteManager
        RouteManager routeManager = new RouteManager();

        // 3. Select routing options
        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.PEDESTRIAN);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);

        // 4. Select Waypoints for your routes
        // START: Nokia, Burnaby
        routePlan.addWaypoint(new GeoCoordinate(49.1966286, -123.0053635));

        // END: Airport, YVR
        routePlan.addWaypoint(new GeoCoordinate(49.1947289, -123.1762924));

        // 5. Retrieve Routing information via RouteManagerEventListener
        try {
            RouteManager.Error error = routeManager.calculateRoute(routePlan, routeManagerListener);
            if (error != RouteManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "Route calculation failed with: " + error.toString(), Toast.LENGTH_SHORT)
                        .show();
            }
        }catch(Exception e){
            String s=e.toString();
        }

    }
    private RouteManager.Listener routeManagerListener = new RouteManager.Listener() {
        public void onCalculateRouteFinished(RouteManager.Error errorCode, List<RouteResult> result) {

            if (errorCode == RouteManager.Error.NONE && result.get(0).getRoute() != null) {
                // create a map route object and place it on the map
                mapRoute = new MapRoute(result.get(0).getRoute());
                map.addMapObject(mapRoute);

                // Get the bounding box containing the route and zoom in (no animation)
                GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                map.zoomTo(gbb, Map.Animation.NONE, Map.MOVE_PRESERVE_ORIENTATION);

//                textViewResult.setText(String.format("Route calculated with %d maneuvers.", result
//                        .get(0).getRoute().getManeuvers().get(0).getRoadName().toString()));
//


                text =  "You are at the Road" +result.get(0).getRoute().getManeuvers().get(0).getRoadName()
                        + result.get(0).getRoute().getManeuvers().get(0).getRoadNumber()+
                        "Your next turn is " + result.get(0).getRoute().getManeuvers().get(0).getTurn()+
                        "Distance to Next Turn" + result.get(0).getRoute().getManeuvers().get(0).getDistanceToNextManeuver();
                speakOut();
//                for(int i=0;i<result.get(0).getRoute().getManeuvers().size();i++) {
//                    text = result.get(0).getRoute().getManeuvers().get(0).getTrafficDirection().toString();
//                    speakOut();
//                    textViewResult.setText(textViewResult.getText() +
//                             result.get(0).getRoute().getManeuvers().get(i).getTrafficDirection().toString());
//                }
            } else {
                textViewResult.setText(String.format("Route calculation failed: %s",
                        errorCode.toString()));
            }
        }

        public void onProgress(int percentage) {
            textViewResult.setText(String.format("... %d percent done ...", percentage));
        }
    };



    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                   text = "Done";
                }

                @Override
                public void onError(String utteranceId) {
                   text = "Error";
                }
            });
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {

                speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }



    public  ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
        //    sb.append("&components=country:gr");
            sb.append("&input=" + URLEncoder.encode(input, "utf-8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            //   Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            // Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
                System.out.println("============================================================");
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
                speaksuggestions(predsJsonArray.getJSONObject(i).getString("description").toString());

            }
        } catch (JSONException e) {
            // Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }
    private Runnable mMyRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            //Change state here
        }
    };
public void speaksuggestions(String suggestion)
{
    text = suggestion;
    speakOut();
    Handler myHandler = new Handler();
    myHandler.postDelayed(mMyRunnable, 4000);//Message will be delivered in 1 second.
}


    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index).toString();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults  results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }
}
