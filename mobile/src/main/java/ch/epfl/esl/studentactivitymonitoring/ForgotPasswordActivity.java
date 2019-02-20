package ch.epfl.esl.studentactivitymonitoring;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
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
 * Created by Lara on 3/6/2018.
 * Lets users reset their password if they forgot it
 */

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText mEmailView;

    //Queue of JSON objects to be sent to be.care server
    RequestQueue MyRequestQueue;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);
        context = getApplicationContext();

        mEmailView = findViewById(R.id.registered_emailid);
        //Set up queue for JSON objects
        MyRequestQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        mEmailView.setText(intent.getStringExtra("email"));

        TextView submitButton = findViewById(R.id.submitForgotPassword);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendForgotPwdRequest();
            }
        });

        TextView backButton = findViewById(R.id.backToLoginBtn);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backToLogin();
            }
        });
    }

    private void backToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_forgot_pwd);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), "ForgotPasswordActivity");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void sendForgotPwdRequest() {
        //Create a JSON object with the entered email
        String url = BeCare.baseURL + "public/forgotPassword";
        JSONObject loginCredentials = new JSONObject();
        try {
            loginCredentials.put("email",mEmailView.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Prepare to send the JSON object to be.care
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.v("Volley","Response: " + response.toString());
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
            switch (message.what) {
                case 0:
                    String text = getString(R.string.change_password_email);
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };
}
