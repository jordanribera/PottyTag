package net.spiralpower.pottytag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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


public class StatusActivity extends ActionBarActivity {

    private boolean mCheckedIn = false;
    private RequestQueue mRequestQueue;
    private String mGender;
    private int mLastCheckIn;

    private final String mAPILocation = "http://spiralpower.net/pottytag/api/";

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

            // Instantiate the cache
            Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

            // Set up the network to use HttpURLConnection as the HTTP client.
            Network network = new BasicNetwork(new HurlStack());

            // Instantiate the RequestQueue with the cache and network.
            mRequestQueue = new RequestQueue(cache, network);

            // Start the queue
            mRequestQueue.start();
        }
        else
        {
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
            //actionButton.setText("Check Out");
            Context context = getApplicationContext();
            CharSequence text = "checked in";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        else
        {
            //actionButton.setText("Check In");
            Context context = getApplicationContext();
            CharSequence text = "checked out";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
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
        if (maleCount > 1) malePopulation = true;
        if (femaleCount > 1) femalePopulation = true;
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
            leftSign.refreshDrawableState();
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
            leftSign.setVisibility(View.INVISIBLE);
        }

        if (!leftToiletValid && !rightToiletValid) return;

        if (populationLayout[0].equals("female") && leftToiletValid)
        {
            leftPooper.setImageResource(R.drawable.f_cutiepoo_use);
            leftPooper.setVisibility(View.VISIBLE);
        }
        else if (populationLayout[0].equals("male") && leftToiletValid)
        {
            leftPooper.setImageResource(R.drawable.m_cutiepoo_use);
            leftPooper.setVisibility(View.VISIBLE);
        }
        else
        {
            leftPooper.setVisibility(View.INVISIBLE);
        }

        if (populationLayout[1].equals("female") && leftToiletValid)
        {
            rightPooper.setImageResource(R.drawable.f_cutiepoo_use);
            rightPooper.setVisibility(View.VISIBLE);
        }
        else if (populationLayout[1].equals("male") && leftToiletValid)
        {
            rightPooper.setImageResource(R.drawable.m_cutiepoo_use);
            rightPooper.setVisibility(View.VISIBLE);
        }
        else
        {
            rightPooper.setVisibility(View.INVISIBLE);
        }
        Log.w("potty_debug", "what is going on");
    }
}
