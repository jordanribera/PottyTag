package net.spiralpower.pottytag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SelectionActivity extends ActionBarActivity {

    private static final ScheduledExecutorService mBlinkWorker = Executors.newSingleThreadScheduledExecutor();

    MediaPlayer popPlayer;

    public void onMaleClick(View view) {
        setGenderAndContinue("m");
    }

    public void onFemaleClick(View view) {
        setGenderAndContinue("f");
    }

    private void setGenderAndContinue(String gender) {
        // Store gender
        popPlayer.start();
        SharedPreferences prefs = getSharedPreferences("net.spiralpower.pottytag", MODE_PRIVATE);
        Editor editor  = prefs.edit();
        editor.putString("gender", gender);
        editor.commit();
        prefs = getPreferences(MODE_PRIVATE);
        Log.d("potty_debug", ((Boolean)prefs.contains("gender")).toString());

        // Move to status screen
        Intent statusActivityIntent = new Intent(this, StatusActivity.class);
        startActivity(statusActivityIntent);
        this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        popPlayer = MediaPlayer.create(this, R.raw.pop);

        ImageView malePoop = (ImageView)findViewById(R.id.maleButton);
        ImageView femalePoop = (ImageView)findViewById(R.id.femaleButton);

        View.OnTouchListener malePoopTouchHandler = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("potty_debug", event.toString());
                ImageView touchingPoop = (ImageView)v;

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    touchingPoop.setImageResource(R.drawable.m_cutiepoo_blink);
                }
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    touchingPoop.setImageResource(R.drawable.m_cutiepoo);
                }

                return false;
            }
        };

        View.OnTouchListener femalePoopTouchHandler = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("potty_debug", event.toString());
                ImageView touchingPoop = (ImageView)v;

                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    touchingPoop.setImageResource(R.drawable.f_cutiepoo_blink);
                }
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    touchingPoop.setImageResource(R.drawable.f_cutiepoo);
                }

                return false;
            }
        };

        malePoop.setOnTouchListener(malePoopTouchHandler);
        femalePoop.setOnTouchListener(femalePoopTouchHandler);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_selection, menu);
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

    /**public void doBlink()
    {

        ImageView malePoop = (ImageView)findViewById(R.id.maleButton);
        ImageView femalePoop = (ImageView)findViewById(R.id.femaleButton);

        Random random = new Random();
        int whichBlink = random.nextInt(2);

        Log.w("potty_debug", "doBlink(), on button " + whichBlink);

        if (whichBlink == 0)
        {
            malePoop.setImageResource(R.drawable.m_cutiepoo_blink);
            malePoop.postInvalidate();
        }
        else
        {
            femalePoop.setImageResource(R.drawable.f_cutiepoo_blink);
            femalePoop.postInvalidate();
        }

        Runnable removeBlink = new Runnable() {
            @Override
            public void run() {
                unBlink();
            }
        };

        mBlinkWorker.schedule(removeBlink, 2000, TimeUnit.MILLISECONDS);

    }

    public void unBlink()
    {

        ImageView malePoop = (ImageView)findViewById(R.id.maleButton);
        ImageView femalePoop = (ImageView)findViewById(R.id.femaleButton);

        malePoop.setImageResource(R.drawable.m_cutiepoo);
        femalePoop.setImageResource(R.drawable.f_cutiepoo);

        Random random = new Random();
        int blinkRestDuration = random.nextInt(5 - 2) + 2;
        Log.w("potty_debug", "unBlink(), resting for " + blinkRestDuration);

        Runnable nextBlink = new Runnable() {
            @Override
            public void run() {
                doBlink();
            }
        };

        mBlinkWorker.schedule(nextBlink, blinkRestDuration, TimeUnit.SECONDS);

    }*/
}
