package net.spiralpower.pottytag;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class StatusActivity extends ActionBarActivity {

    private boolean mCheckedIn = false;
    private RequestQueue mRequestQueue;
    private String mGender;
    private int mLastCheckIn;

    private boolean mLeftToiletFlagged;
    private boolean mRightToiletFlagged;

    private boolean mActivityVisible = true;

    private Timer timer = new Timer();

    private final String mAPILocation = "http://spiralpower.net/pottytag/api/";

    private int mNotificationID = 1337;
    private boolean mNotificationActive;

    private int lennyCounter = 0;

    @Override
    protected void onResume()
    {
        super.onResume();
        mActivityVisible = true;
        Log.d("potty_debug", "onResume()");
        nextTimer();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mActivityVisible = false;
        Log.d("potty_debug", "onPause()");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("net.spiralpower.pottytag", MODE_PRIVATE);

        if(prefs.contains("gender")) {
            this.mGender = prefs.getString("gender", "z");

            setContentView(R.layout.activity_status);

            ImageView actionButton = (ImageView) this.findViewById(R.id.actionButton);
            actionButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    handleActionButtonClick(v);
                }
            });

            ImageView leftToilet = (ImageView)findViewById(R.id.toiletLeft);
            leftToilet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFlag(0);
                }
            });

            ImageView rightToilet = (ImageView)findViewById(R.id.toiletRight);
            rightToilet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFlag(1);
                }
            });

            // Instantiate the cache
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

            // Set up the network to use HttpURLConnection as the HTTP client.
            Network network = new BasicNetwork(new HurlStack());

            // Instantiate the RequestQueue with the cache and network.
            mRequestQueue = new RequestQueue(cache, network);

            // Start the queue
            mRequestQueue.start();

            getStatus();
            nextTimer();
        }
        else
        {
            mActivityVisible = false;
            Intent settingsActivityIntent = new Intent(this, SelectionActivity.class);
            startActivity(settingsActivityIntent);
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void handleActionButtonClick(View v)
    {
        //Button actionButton = (Button)this.findViewById(R.id.actionButton);
        ImageView actionButton = (ImageView)v;

        if (this.mCheckedIn)
        {
            doCheckOut();
        }
        else
        {
            doCheckIn();
        }

        getStatus();

        /**Context context = getApplicationContext();
        CharSequence text = actionButton.getText();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();*/
    }

    public void updateActionButton()
    {
        ImageView actionButton = (ImageView)this.findViewById(R.id.actionButton);
        if (mCheckedIn)
        {
            actionButton.setImageResource(R.drawable.action_button_checkout_states);
        }
        else
        {
            actionButton.setImageResource(R.drawable.action_button_checkin_states);
        }
    }

    public void doCheckIn()
    {
        String url = mAPILocation + "?r=action&action=checkin&gender=" + this.mGender;
        if (this.mLastCheckIn > 0)
        {
            url = url + "&last_checkin=" + this.mLastCheckIn;
        }
        Log.d("potty_debug", url);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.d("potty_debug", response.toString());

                //check success?
                Boolean responseSuccess = false;
                try
                {
                    responseSuccess = response.getBoolean("success");
                }
                catch (Exception e) { }

                if (responseSuccess)
                {
                    int responseID = 0;
                    try
                    {
                        responseID = response.getInt("id");
                    }
                    catch (Exception e) { }

                    mLastCheckIn = responseID;
                    mCheckedIn = true;
                    updateActionButton();
                    startWarningTimer();
                    startExpirationTimer();
                }
            }
        };

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //you have failed.
            }
        };

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, responseErrorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    public void doCheckOut()
    {
        String url = mAPILocation + "?r=action&action=checkout&last_checkin=" + this.mLastCheckIn;
        Log.d("potty_debug", url);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.d("potty_debug", response.toString());

                //check success?
                mCheckedIn = false;
                updateActionButton();
            }
        };

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //you have failed.
            }
        };

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, responseErrorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    public void toggleFlag(int whichToilet)
    {
        String flagAction;
        if (whichToilet == 0)
        {
            if (mLeftToiletFlagged)
            {
                flagAction = "removeflag";
            }
            else
            {
                flagAction = "addflag";
            }
        }
        else
        {
            if (mRightToiletFlagged)
            {
                flagAction = "removeflag";
            }
            else
            {
                flagAction = "addflag";
            }
        }

        String url = mAPILocation + "?r=action&action=" + flagAction + "&toilet_id=" + whichToilet;

        Log.d("potty_debug", url);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                getStatus();
            }
        };

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //you have failed.
            }
        };

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, responseErrorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    public void getStatus()
    {
        String url = mAPILocation + "?r=status";
        Log.d("potty_debug", url);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.d("potty_debug", response.toString());
                //Context context = getApplicationContext();
                //CharSequence text = response.toString();
                //int duration = Toast.LENGTH_SHORT;

                //Toast toast = Toast.makeText(context, text, duration);
                //toast.show();

                int maleCount = 0;
                int femaleCount = 0;
                boolean leftToiletValid = true;
                boolean rightToiletValid = true;

                try
                {
                    maleCount = response.getInt("m_population");
                    femaleCount = response.getInt("f_population");
                    leftToiletValid = response.getBoolean("left_toilet");
                    rightToiletValid = response.getBoolean("right_toilet");
                }
                catch (Exception e) {}

                displayStatus(maleCount, femaleCount, leftToiletValid, rightToiletValid);
                mLeftToiletFlagged = !leftToiletValid;
                mRightToiletFlagged = !rightToiletValid;

            }
        };

        Response.ErrorListener responseErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //you have failed.
            }
        };

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, responseErrorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    public void displayStatus(int maleCount, int femaleCount, boolean leftToiletValid, boolean rightToiletValid)
    {
        Random random = new Random();

        boolean mixedPopulation = false;
        boolean malePopulation = false;
        boolean femalePopulation = false;
        if (maleCount > 0) malePopulation = true;
        if (femaleCount > 0) femalePopulation = true;
        if (malePopulation && femalePopulation) mixedPopulation = true;

        String[] populationLayout = new String[]{"empty", "empty"};
        if (malePopulation)
        {
            populationLayout = new String[]{"male", "empty"};
            if (maleCount > 1) populationLayout = new String[]{"male", "male"};
        }
        if (femalePopulation)
        {
            populationLayout = new String[]{"female", "empty"};
            if (femaleCount > 1) populationLayout = new String[]{"female", "female"};
        }
        if (mixedPopulation) populationLayout = new String[]{"female", "male"};

        Log.d("potty_debug", populationLayout[0] + ":" + populationLayout[1]);

        //poopers
        ImageView leftPooper = (ImageView)findViewById(R.id.leftPooper);
        ImageView rightPooper = (ImageView)findViewById(R.id.rightPooper);

        //signs
        ImageView leftSign = (ImageView)findViewById(R.id.leftSign);
        ImageView rightSign = (ImageView)findViewById(R.id.rightSign);

        //toilet flags
        if (!leftToiletValid)
        {
            Log.w("potty_debug", "left toilet invalid");
            leftSign.setVisibility(View.VISIBLE);
            leftPooper.setVisibility(View.INVISIBLE);
        }
        else
        {
            Log.w("potty_debug", "left toilet valid");
            leftSign.setVisibility(View.INVISIBLE);
        }

        if (!rightToiletValid)
        {
            Log.w("potty_debug", "right toilet invalid");
            rightSign.setVisibility(View.VISIBLE);
            rightPooper.setVisibility(View.INVISIBLE);
        }
        else
        {
            Log.w("potty_debug", "right toilet valid");
            rightSign.setVisibility(View.INVISIBLE);
        }

        if (!leftToiletValid && !rightToiletValid) return;

        if (populationLayout[0].equals("female") && leftToiletValid)
        {
            Log.d("potty_debug", "showing female left");
            leftPooper.setImageResource(R.drawable.f_cutiepoo_use);
            leftPooper.setVisibility(View.VISIBLE);
        }
        else if (populationLayout[0].equals("male") && leftToiletValid)
        {
            Log.d("potty_debug", "showing male left");
            leftPooper.setImageResource(R.drawable.m_cutiepoo_use);
            leftPooper.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.d("potty_debug", "hiding left");
            leftPooper.setVisibility(View.INVISIBLE);
        }

        if (populationLayout[1].equals("female") && rightToiletValid)
        {
            Log.d("potty_debug", "showing female right");
            rightPooper.setImageResource(R.drawable.f_cutiepoo_use);
            rightPooper.setVisibility(View.VISIBLE);
        }
        else if (populationLayout[1].equals("male") && rightToiletValid)
        {
            Log.d("potty_debug", "showing male right");
            rightPooper.setImageResource(R.drawable.m_cutiepoo_use);
            rightPooper.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.d("potty_debug", "hiding right");
            rightPooper.setVisibility(View.INVISIBLE);
        }

        //special case for shifting layout[0] to the right if left toilet is invalidated
        if (!leftToiletValid && rightToiletValid)
        {
            if (populationLayout[0].equals("female"))
            {
                Log.d("potty_debug", "showing female right");
                rightPooper.setImageResource(R.drawable.f_cutiepoo_use);
                rightPooper.setVisibility(View.VISIBLE);
            }
            else if (populationLayout[0].equals("male"))
            {
                Log.d("potty_debug", "showing male right");
                rightPooper.setImageResource(R.drawable.m_cutiepoo_use);
                rightPooper.setVisibility(View.VISIBLE);
            }
            else
            {
                Log.d("potty_debug", "hiding right");
                rightPooper.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void rechooseGender(View v)
    {
        //createNotification();
        Intent settingsActivityIntent = new Intent(this, SelectionActivity.class);
        startActivity(settingsActivityIntent);
    }

    public void nextTimer()
    {

        int timerInterval = 3000;
        //if (!mActivityVisible) timerInterval = 30000;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getStatus();
                Log.d("potty_debug", "timer tick");
                if (mActivityVisible) nextTimer();
            }
        }, timerInterval);
    }

    public void createNotification()
    {
        Intent noteIntent = new Intent(this, StatusActivity.class);
        PendingIntent notePendingIntent = PendingIntent.getActivity(this, 0, noteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder noteBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Potty Tag")
                .setContentText("Your checked in status is about to expire.")
                .setContentIntent(notePendingIntent);

        NotificationManager noteManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        noteManager.notify(mNotificationID, noteBuilder.build());
    }

    public void startWarningTimer()
    {
        int warningTimeMs = 270000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getStatus();
                Log.d("potty_debug", "warning timer execute");
                if (mCheckedIn) createNotification();
            }
        }, warningTimeMs);
    }

    public void startExpirationTimer()
    {
        int expirationTimeMs = 300000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getStatus();
                Log.d("potty_debug", "expiration timer execute");
                doCheckOut();
            }
        }, expirationTimeMs);
    }

    public void handleLenny(View v)
    {
        lennyCounter++;
        if (lennyCounter > 5)
        {
            Context context = getApplicationContext();
            CharSequence text = "( ͡° ͜ʖ ͡°)";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            lennyCounter = 0;
        }

    }
}
