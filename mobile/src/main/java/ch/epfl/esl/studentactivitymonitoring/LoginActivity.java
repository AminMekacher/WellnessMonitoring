package ch.epfl.esl.studentactivitymonitoring;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.HeartRateData;
import ch.epfl.esl.commons.ID;
import ch.epfl.esl.commons.MyDatabase;
import ch.epfl.esl.commons.TRIMP;
import ch.epfl.esl.commons.User;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.CredentialsOptions;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A login screen that offers login or registration via email/password.
 * Authenticates login credentials via the be.care API
 * Saves authentication token for use in future be.care communication
 * If there is workout HR data stored in Room, it is uploaded when login is successful
 */

public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

    String TAG = "LoginActivity";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    Activity thisActivity;

    //Google Smart Lock constants
    private static final int RC_SAVE = 1;
    private static final int RC_HINT = 2;
    private static final int RC_READ = 3;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressBar mProgressView;
    private View mLoginFormView;

    //Queue of JSON objects to be sent to be.care server
    RequestQueue MyRequestQueue;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;

    //App context
    private static Context context;

    //Room database storing HR info from watch-tracked workouts
    MyDatabase database;

    String authorizationToken; //authorization token used for be.care server communication
    SharedPreferences sharedPref; //shared preferences to store the token across activities

    //Google Smart Lock objects
    CredentialsClient mCredentialsClient;
    CredentialRequest mCredentialRequest;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context = getApplicationContext();

        //Get reference to Room database to retrieve workout HR
        database = MyDatabase.getDatabase(context);
        thisActivity = this;

        sharedPref = this.getSharedPreferences("strings", Context.MODE_PRIVATE);;

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);

        //Set up queue for JSON objects
        MyRequestQueue = Volley.newRequestQueue(this);

        //Set up the "Show Password" Check Box
        final CheckBox showPassword = (CheckBox) findViewById(R.id.show_hide_password);

        //If the user presses "Enter" on the password view, attempt login
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        //If the user checks the "Show Password" button, show the password
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPasswordView.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                else {
                    mPasswordView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        //If the user presses the "Sign in" button, attempt login
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        //If the "Register" button is clicked, go to register activity
        TextView registerButton = (TextView) findViewById(R.id.email_register_button);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                registerNewUser();
            }
        });

        TextView forgotPwd = findViewById(R.id.forgotPwd);
        forgotPwd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                forgotPassword();
            }
        });

        //View containing login UI
        mLoginFormView = findViewById(R.id.login_layout);
        //View containing loading animation
        mProgressView = findViewById(R.id.login_progress);
        mProgressView.setVisibility(View.INVISIBLE);
        mProgressView.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        //Set up Google Smart Lock to autofill username and password
        CredentialsOptions options = new CredentialsOptions.Builder()
                .forceEnableSaveDialog()
                .build();

        mCredentialsClient = Credentials.getClient(this, options);
        mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .build();

        mCredentialsClient.request(mCredentialRequest).addOnCompleteListener(
                new OnCompleteListener<CredentialRequestResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {
                        Log.v("Credentials","Credential request response");
                        if (task.isSuccessful()) {
                            // See "Handle successful credential requests"
                            Log.v("Credentials","Credential request success");
                            onCredentialRetrieved(task.getResult().getCredential());
                            return;
                        }

                        // See "Handle unsuccessful and incomplete credential requests"
                        // ...
                    }
                });


    }

    //If the user has saved their credentials to Smart Lock, retrieve the credentials and fill the text fields
    private void onCredentialRetrieved(Credential credential) {
        Log.v("Credentials","Credential retrieved");
        mEmailView.setText(credential.getId());
        mPasswordView.setText(credential.getPassword());

    }

    //Go to forgot password activity
    private void forgotPassword() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("email",mEmailView.getText().toString());
        startActivity(intent);
    }

    //Create options menu
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    //Set up options menu items
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_login_activity);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), "LoginActivity");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false; //set to true if there's an error
        View focusView = null; //set the problematic view into focus

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    //Save username and password entered into TextViews and go to register activity
    private void registerNewUser() {
        final Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("email",mEmailView.getText().toString());
        intent.putExtra("password",mPasswordView.getText().toString());
        startActivity(intent);
    }

    //Determine whether the e-mail is valid or not
    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    //Determine whether pass word is valid (this is totally arbitrary..)
    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            //Hide login form and show loading animation if true
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    //Useless functions that came with the login activity
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        //mEmailView.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            //Create a JSON object with the entered email and password
            String url = BeCare.baseURL + "login";
            JSONObject loginCredentials = new JSONObject();
            try {
                loginCredentials.put("email",mEmail);
                loginCredentials.put("password",mPassword);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Response.Listener<JSONObject> jsonObjectListener = new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    //If the profile authentication is successful, go to the WelcomeActivity
                    mAuthTask = null;
                    Log.v("Volley","Response: " + response.toString());
                    try {
                        //Save user ID to Room for firebase data saving purposes
                        ID.userID = response.getInt("id");
                        DeleteLastUserID dluid = new DeleteLastUserID();
                        dluid.execute(); //delete old user id
                        StoreToDatabase storeToDatabase = new StoreToDatabase();
                        storeToDatabase.execute(ID.userID);

                        Log.v("Volley","Trying to get token: " + response.getString("Authorization"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        //Save authorization token in Shared Preferences
                        JSONObject headers = response.getJSONObject("headers");
                        String bearer = headers.getString("Authorization");
                        String[] split = bearer.split(" ");
                        String token = split[1];
                        authorizationToken = token;

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("authorizationToken", token);
                        editor.commit();

                        Log.v("Volley", "Token: " + token);
                    } catch (JSONException e) {
                        Log.v("Volley","Getting headers failed");
                        e.printStackTrace();
                    }
                    saveUserCredentials(mEmail, mPassword); //Save credentials in Google SmartLock
                    showProgress(false); //stop loading
                    goToWelcomeActivity(); //switch activities
                    sendFirebaseToken(); //send FB token for FB notifications to work
                    sendCachedHRArraysToTRIMP(); //send HR arrays saved on phone to be.care
                }
            };

            Response.ErrorListener jsonErrorListener = new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    //If be.care does not authenticate the user, prompt the user that their login credentials are invalid
                    mAuthTask = null;
                    showProgress(false);
                    Log.v("Volley","Error: " + error.toString());
                    mPasswordView.setError(getString(R.string.error_invalid_login_credentials));
                    mPasswordView.requestFocus();

                }
            };

            //Prepare to send the JSON object to be.care
            MetaRequest jsObjRequest = new MetaRequest
                    (Request.Method.POST, url, loginCredentials, jsonObjectListener, jsonErrorListener);

            //Increase request timeout time to give the server some time to respond
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            //Send JSON object to be.care
            MyRequestQueue.add(jsObjRequest);

            return true;
        }

    }

    //Save user credentials in Google Smart Lock
    private void saveUserCredentials(String email, String password) {
        Log.v(TAG,"Saving user credentials");
        Credential credential = new Credential.Builder(email)
                .setPassword(password)
                // Important: only store passwords in this field.
                // Android autofill uses this value to complete
                // sign-in forms, so repurposing this field will
                // likely cause errors.
                .build();
        requestSaveCredentials(credential);
    }

    //callback function to check if credential saving worked
    private void requestSaveCredentials(Credential credential) {

        mCredentialsClient.save(credential).addOnCompleteListener(
                new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d("Credentials", "SAVE: OK");
                            return;
                        }

                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // Try to resolve the save request. This will prompt the user if
                            // the credential is new.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            try {
                                rae.startResolutionForResult(thisActivity, RC_SAVE);
                            } catch (IntentSender.SendIntentException error) {
                                // Could not resolve the request
                                Log.e("Credentials", "Failed to send resolution.", error);
                            }
                        } else {
                            // Request has no resolution
                            Log.e("Credentials","Save failed.", e);
                        }
                    }
                });
    }

    //Further debugging of Google Smart Lock
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Credentials", "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        switch (requestCode) {
            case RC_HINT:
                // Drop into handling for RC_READ
            case RC_READ:
                if (resultCode == RESULT_OK) {
                    Log.d("Credentials", "Credential Read: OK");
                    boolean isHint = (requestCode == RC_HINT);
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    //processRetrievedCredential(credential, isHint);
                } else {
                    Log.e("Credentials", "Credential Read: NOT OK");
                    //showToast("Credential Read Failed");
                }

                //mIsResolving = false;
                break;
            case RC_SAVE:
                if (resultCode == RESULT_OK) {
                    Log.d("Credentials", "Credential Save: OK");
                    //showToast("Credential Save Success");
                } else {
                    Log.e("Credentials", "Credential Save: NOT OK");
                    //showToast("Credential Save Failed");
                }


                break;
        }
    }


    private void goToWelcomeActivity() {
        final Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
    }

    //Take workout data that was send from watch to phone but was unable to be sent to be.care at the time
    //Send to be.care as a TRIMP value
    private void sendCachedHRArraysToTRIMP(){
        GetDataFromDatabase gdfd = new GetDataFromDatabase();
        gdfd.execute(context);
    }

    class GetDataFromDatabase extends AsyncTask<Context, Void, Void> {
        String TAG = "GetDataFromDatabase";

        @Override
        protected Void doInBackground(Context... contexts) {
            // Retrieving cached data
            List<ch.epfl.esl.commons.HeartRateData> hr_data = database.sensorDataDao().getLastValues();
            Log.v(TAG,"# of data lists in room: " + hr_data.size());
            // Loop through datasets and create a TRIMP for each one
            for (ch.epfl.esl.commons.HeartRateData hr_data_value: hr_data) {
                ArrayList<Float> hrArray = hr_data_value.value;
                float[] hrArrayFloat = new float[hrArray.size()];
                for (int i = 0; i < hrArray.size(); i++) {
                    hrArrayFloat[i] = hrArray.get(i);
                }
                TRIMP trimp = new TRIMP(hrArrayFloat, contexts[0]);
            }

            return null;
        }

        //When that's done, delete the data
        @Override
        protected void onPostExecute(Void nothing) {
            Log.v(TAG,"Deleting data from Room");
            DeleteDataTask ddt = new DeleteDataTask();
            ddt.execute();
        }
    }

    //Save user ID in Room
    class StoreToDatabase extends AsyncTask<Integer, Void, Integer> {

        @Override
        protected Integer doInBackground(Integer... userIDs) {
            User user = new User();
            user.userID = userIDs[0];
            // adding the measurement into the database
            database.userDataInterface().insertSensorData(user);
            // Retrieving the last measurement
            Integer userID =
                    database.userDataInterface().getLastValue();
            return userID;
        }

        @Override
        protected void onPostExecute(Integer userID) {
            Log.v("Room","Saving userID to Room: " + userID);
        }
    }

    //Delete last user ID in Room
    private class DeleteLastUserID extends  AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            database.userDataInterface().deleteAll();
            return null;
        }
    }

    //Delete all HR data in room
    private class DeleteDataTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            database.sensorDataDao().deleteAll();
            return null;
        }
    }

    //Send the user's firebase token to be.care to get the FB notifications to work
    private void sendFirebaseToken() {

        String firebaseToken = FirebaseInstanceId.getInstance().getToken();
        Log.v("FB token","Firebase token: " + firebaseToken);

        String url = BeCare.baseURL + "user/firebaseToken";
        JSONObject loginCredentials = new JSONObject();
        try {
            loginCredentials.put("token",firebaseToken);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("FB token","Response: " + response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("FB token","Error: " + error.toString());


                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.v("Volley","Setting token to " + authorizationToken);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization",authorizationToken);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MyRequestQueue.add(jsObjRequest);

    }
}

//Class used to extract authorization token from be.care's headers
class MetaRequest extends JsonObjectRequest {

    public MetaRequest(int method, String url, JSONObject jsonRequest, Response.Listener
            <JSONObject> listener, Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
    }

    public MetaRequest(String url, JSONObject jsonRequest, Response.Listener<JSONObject>
            listener, Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            JSONObject jsonResponse = new JSONObject(jsonString);
            jsonResponse.put("headers", new JSONObject(response.headers));
            return Response.success(jsonResponse,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }
}
