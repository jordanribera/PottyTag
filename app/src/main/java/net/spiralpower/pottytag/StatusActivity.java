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


public class StatusActivity extends ActionBarActivity {

    private boolean mCheckedIn = false;
    private RequestQueue mRequestQueue;
    private String mGender;
    private int mLastCheckIn;
    private boolean mDebugSelection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("net.spiralpower.pottytag", MODE_PRIVATE);

        if (mDebugSelection)
        {
            SharedPreferences.Editor prefsEditor = prefs.edit();
            prefsEditor.clear();
            prefsEditor.commit();
            prefs = getSharedPreferences("net.spiralpower.pottytag", MODE_PRIVATE);
        }

        if(prefs.contains("gender") || !mDebugSelection) {
            this.mGender = prefs.getString("gender", "z");

            setContentView(R.layout.activity_status);

            Button actionButton = (Button) this.findViewById(R.id.actionButton);
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
        Button actionButton = (Button)v;

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
        Button actionButton = (Button)this.findViewById(R.id.actionButton);
        if (mCheckedIn)
        {
            actionButton.setText("Check Out");
        }
        else
        {
            actionButton.setText("Check In");
        }

    }

    public void doCheckIn()
    {
        String url = "http://spiralpower.net/pottytag/?r=action&action=checkin&gender=" + this.mGender;
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

                //getStatus();
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
        String url = "http://spiralpower.net/pottytag/?r=action&action=checkout&checkin_id=" + this.mLastCheckIn;
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
        String url = "http://spiralpower.net/pottytag/?r=status";
        Log.d("potty_debug", url);

        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>()
        {
            @Override
            public void onResponse(JSONObject response)
            {
                Log.d("potty_debug", response.toString());
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
}
