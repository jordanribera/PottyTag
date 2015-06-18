package net.spiralpower.pottytag;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class StatusActivity extends ActionBarActivity {

    private boolean checkedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("net.spiralpower.pottytag", MODE_PRIVATE);


        if(prefs.contains("gender")) {
            setContentView(R.layout.activity_status);

            Button actionButton = (Button) this.findViewById(R.id.actionButton);
            actionButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    handleActionButtonClick(v);
                }
            });
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

        this.checkedIn = !this.checkedIn;

        if (this.checkedIn)
        {
            actionButton.setText("Check Out");
            this.checkedIn = true;
        }
        else
        {
            actionButton.setText("Check In");
            this.checkedIn = false;
        }

        /**Context context = getApplicationContext();
        CharSequence text = actionButton.getText();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();*/
    }
}
