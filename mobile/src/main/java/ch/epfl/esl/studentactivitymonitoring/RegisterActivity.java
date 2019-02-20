package ch.epfl.esl.studentactivitymonitoring;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import ch.epfl.esl.commons.BeCare;
import ch.epfl.esl.commons.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by orlandic on 27/02/2018.
 * Registers user through be.care platform
 * User selects system language
 */

public class RegisterActivity extends AppCompatActivity {
    private final static String TAG  = "RegisterActivity";

    //Strings to be extracted from textViews
    private static String mEmail;
    private static String mPassword;
    private static String mPasswordRepeat;
    private static String language;

    //Possible languages that the app supports
    static class Language {
        public static final String ENGLISH = "english";
        public static final String FRENCH = "french";
    }

    //UI elements
    private TextView mEmailView;
    private TextView mFirstName;
    private TextView mLastName;
    private TextView mPasswordView;
    private TextView mPasswordRepeatView;
    private EditText mUserBirthday;
    private EditText mUserHeight;
    private Button doneButton;
    private ProgressBar mProgressView;
    private View mRegisterForm;
    RadioButton englishButton;
    RadioButton frenchButton;
    RadioButton maleButton;
    RadioButton femaleButton;

    //App context
    private static Context context;

    //Backend code objects
    private UserRegisterTask mRegTask = null;
    private static RequestQueue MyRequestQueue;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
        context = getApplicationContext();

        //Instantiate UI objects
        mEmailView = findViewById(R.id.registerEmail);
        mPasswordView = findViewById(R.id.registerPassword);
        mPasswordRepeatView = findViewById(R.id.repeatPassword);
        mRegisterForm = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.registerProgress);
        mFirstName = findViewById(R.id.user_firstname);
        mLastName = findViewById(R.id.user_lastname);
        mUserHeight = findViewById(R.id.user_height);
        mUserBirthday = findViewById(R.id.user_age);
        maleButton = findViewById(R.id.male_radio_button);
        femaleButton = findViewById(R.id.female_radio_button);

        //shared preferences
        sharedPref = this.getSharedPreferences("strings", Context.MODE_PRIVATE);

        //Do not show loading view at first
        mProgressView.setVisibility(View.INVISIBLE);
        mProgressView.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

        //Get the previously entered registration values from the intent
        final Intent intent = getIntent();
        mEmailView.setText(intent.getStringExtra("email"));
        mPasswordView.setText(intent.getStringExtra("password"));
        mPasswordRepeatView.setText(intent.getStringExtra("password2"));
        language = intent.getStringExtra("language");
        mUserBirthday.setText(intent.getStringExtra("birthday"));
        mUserHeight.setText(intent.getStringExtra("height"));
        mFirstName.setText(intent.getStringExtra("firstName"));
        mLastName.setText(intent.getStringExtra("lastName"));


        //Set up queue for JSON objects to be sent to be.care
        MyRequestQueue = Volley.newRequestQueue(this);

        doneButton = findViewById(R.id.doneRegistering);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerNewUser();
            }
        });

        englishButton = findViewById(R.id.english_radio_button);
        frenchButton = findViewById(R.id.french_radio_button);

        //Switch language button activation based on previously selected language
        if (language != null) {
            switch (language) {
                case Language.ENGLISH:
                    englishButton.setChecked(true);
                    frenchButton.setChecked(false);
                    break;
                case Language.FRENCH:
                    englishButton.setChecked(false);
                    frenchButton.setChecked(true);
                    break;
                default:
                    englishButton.setChecked(false);
                    frenchButton.setChecked(false);
                    break;
            }
        }

        //Change system language when either button is clicked
        englishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                englishButton.setChecked(true);
                frenchButton.setChecked(false);
                changeSystemLanguage("en");

            }
        });
        frenchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                frenchButton.setChecked(true);
                englishButton.setChecked(false);
                changeSystemLanguage("fr");

            }
        });

        maleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                femaleButton.setChecked(false);
            }
        });
        femaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maleButton.setChecked(false);
            }
        });

        TextView alreadyRegisterd = findViewById(R.id.alreadyRegisteredTV);
        alreadyRegisterd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actuallyGoToLogin();
            }
        });

    }

    //language codes: "en" for english and "fr" for french
    private void changeSystemLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.locale = locale;
        res.updateConfiguration(config, res.getDisplayMetrics());
        Intent intent = getIntent();

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("languageCode", languageCode);
        editor.commit();

        intent.putExtra("email",mEmailView.getText().toString());
        intent.putExtra("password",mPasswordView.getText().toString());
        intent.putExtra("password2",mPasswordRepeatView.getText().toString());
        intent.putExtra("firstName", mFirstName.getText().toString());
        intent.putExtra("lastName",mLastName.getText().toString());
        intent.putExtra("height",mUserHeight.getText().toString());
        intent.putExtra("birthday",mUserBirthday.getText().toString());
        if (languageCode.equals("en")) {
            intent.putExtra("language",Language.ENGLISH);
        } else {
            intent.putExtra("language",Language.FRENCH);
        }
        finish();
        startActivity(intent);
    }

    // Methods used for the DatePicker Fragment (Date of Birth) and the TimePicke Fragment (Notification Time)
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), getString(R.string.date_picker));
    }


    public void processDatePickerResult(int year, int month, int day) {
        // The month integer returned by the date picker starts counting at 0
        // for January, so you need to add 1 to show months starting at 1.
        String month_string = Integer.toString(month + 1);
        String day_string = Integer.toString(day);
        String year_string = Integer.toString(year);
        if (month < 10) {
            month_string = "0" + month_string;
        }
        if (day < 10) {
            day_string = "0" + day_string;
        }
        // Assign the concatenated strings to dateMessage.
        String dateMessage = (year_string + "-" + month_string + "-" + day_string);
        mUserBirthday.setText(dateMessage);
        //Toast.makeText(this, getString(R.string.date) + dateMessage, Toast.LENGTH_SHORT).show();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.simple_help_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                String errorMesssage = getString(R.string.help_register);
                HelpFragment dialog = HelpFragment.newInstance(errorMesssage);
                dialog.show(getFragmentManager(), TAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void registerNewUser() {
        if (mRegTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String repeatPassword = mPasswordRepeatView.getText().toString();
        String birthday = mUserBirthday.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        } else if (!doPasswordsMatch(password, repeatPassword)) {
            mPasswordRepeatView.setError(getString(R.string.error_password_mismatch));
            focusView = mPasswordRepeatView;
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

        //Make sure user selected a gender
        if (!maleButton.isChecked() && !femaleButton.isChecked()) {
            cancel = true;
            focusView = femaleButton;
            femaleButton.setError(getString(R.string.error_field_required));
        } else if (TextUtils.isEmpty(birthday)) {
            cancel = true;
            focusView = mUserBirthday;
            mUserBirthday.setError(getString(R.string.error_field_required));
        }

        try {
            int height = Integer.valueOf(mUserHeight.getText().toString());
        } catch (NumberFormatException nfe) {
            cancel = true;
            focusView = mUserHeight;
            mUserHeight.setError(getString(R.string.input_must_be_integer));
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mRegTask = new UserRegisterTask(email, password);
            mRegTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4;
    }

    private boolean doPasswordsMatch(String pwd1, String pwd2) {
        return pwd1.equals(pwd2);
    }

    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterForm.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterForm.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterForm.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mRegisterForm.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserRegisterTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String url = BeCare.baseURL + "public/registration/new";
            JSONObject loginCredentials = new JSONObject();
            try {
                loginCredentials.put("email",mEmail);
                loginCredentials.put("password",mPassword);
                loginCredentials.put("clientName","EPFL");
                Log.v("Register","Trying to login with email " + mEmail + " and pwd " + mPassword);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    //If the email is sent, go to the e-mail confirmation screen
                    Log.v("Volley","Response: " + response.toString());
                    try {
                        int id = response.getInt("id");
                        JSONObject headers = response.getJSONObject("headers");
                        String bearer = headers.getString("Authorization");
                        String[] split = bearer.split(" ");
                        String token = split[1];
                        mRegTask = null;
                        showProgress(false);
                        goToLogin();
                        Log.v("Volley", "Token: " + token);
                        updateUser(token, id);
                    } catch (JSONException e) {
                        Log.v("Volley","Getting headers failed");
                        e.printStackTrace();
                    }


                }
            };

            Response.ErrorListener errorListener = new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    //be.care sends an error message if the email address is invalid
                    Log.v("Volley","Error: " + error.toString());
                    mRegTask = null;
                    showProgress(false);
                    mEmailView.setError(getString(R.string.error_invalid_email));
                    mEmailView.requestFocus();

                }
            };

            MetaRequest jsObjRequest = new MetaRequest
                    (Request.Method.POST, url, loginCredentials, listener, errorListener);
            jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            MyRequestQueue.add(jsObjRequest);

            return true;
        }

    }

    //TODO: put this somewhere after login
    public void updateUser(final String token, int id) {
        String firstName = mFirstName.getText().toString();
        String lastName = mLastName.getText().toString();
        String emailAddress = mEmailView.getText().toString();
        String gender;
        if (maleButton.isChecked()) {
            gender = Constants.Gender.M;
        } else {
            gender = Constants.Gender.F;
        }
        boolean confirmed = true;
        int height = Integer.valueOf(mUserHeight.getText().toString());
        String birthdate = mUserBirthday.getText().toString();

        String url = BeCare.baseURL + "user/update";
        JSONObject loginCredentials = new JSONObject();
        try {
            loginCredentials.put("emailAdress",emailAddress);
            loginCredentials.put("birthdate",birthdate);
            loginCredentials.put("confirmed",confirmed);
            loginCredentials.put("firstName",firstName);
            loginCredentials.put("gender",gender);
            loginCredentials.put("height",height);
            loginCredentials.put("id",id);
            loginCredentials.put("lastName",lastName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, url, loginCredentials, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //If the email is sent, go to the e-mail confirmation screen
                        Log.v("Update user","Response: " + response.toString());

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //be.care sends an error message if the email address is invalid
                        Log.v("Update user","Error: " + error.toString());


                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.v("Volley","Setting token to " + token);
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization",token);
                return headers;
            }
        };

        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyRequestQueue.add(jsObjRequest);

    }

    private void actuallyGoToLogin(){
        final Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void goToLogin() {
        final Intent intent = new Intent(this, EmailConfirmationActivity.class);
        intent.putExtra("email",mEmailView.getText().toString());
        startActivity(intent);

    }

}
