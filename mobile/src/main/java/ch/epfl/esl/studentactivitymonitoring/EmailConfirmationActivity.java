package ch.epfl.esl.studentactivitymonitoring;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import ch.epfl.esl.commons.BeCare;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by orlandic on 27/02/2018.
 * Activity for sending the confirmation email to users
 */

public class EmailConfirmationActivity extends Activity {

    Button doneButton;

    String email;

    //Queue of JSON objects to be sent to be.care server
    RequestQueue MyRequestQueue;

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_confirmation_activity);

        email = getIntent().getStringExtra("email");

        context =getApplicationContext();

        //Set up queue for JSON objects
        MyRequestQueue = Volley.newRequestQueue(this);

        doneButton = findViewById(R.id.backToLogin);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBackToLogin();
            }
        });

        Button resendConf = findViewById(R.id.resendConfirmation);
        resendConf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resendConfirmation();
            }
        });

    }

    //Set up menu
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    //Set up menu items
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_email_confirmation);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), "EmailConfirmationActivity");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    //Tell be.care to resend the confirmation email
    private void resendConfirmation(){
        String url = BeCare.baseURL + "public/resendConfirmationLink";
        JSONObject loginCredentials = new JSONObject();
        try {
            loginCredentials.put("email",email);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Prepare to send the JSON object to be.care
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v("Volley","Response: " + response.toString());
                        //Tell user the email was successfully sent
                        Message message = uiHandler.obtainMessage(0);
                        message.sendToTarget();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v("Volley","Error: " + error.toString());
                        Message message = uiHandler.obtainMessage(0);
                        message.sendToTarget();
                    }
                });

        //Send JSON object to be.care
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);
    }

    //UI handler for communicating between backend and frontent
    public Handler uiHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            String text = getString(R.string.confirmation_email);
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    };

    private void goBackToLogin() {
        final Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

}
