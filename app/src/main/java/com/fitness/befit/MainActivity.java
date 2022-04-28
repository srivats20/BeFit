package com.fitness.befit;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements OnDataPointListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1234;
    GoogleApiClient mFitnessClient;
    View main;
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;
    SessionInsertRequest insertRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        main = findViewById(R.id.main);
        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        String[] PERMISSIONS = {
                Manifest.permission.ACTIVITY_RECOGNITION
        };
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1234);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        buildFitnessClient();
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void buildFitnessClient() {
        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
                .build();

        Scope scopeLocation = new Scope(Scopes.FITNESS_LOCATION_READ_WRITE);
        Scope scopesActivity = new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE);


        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    scopesActivity, scopeLocation);
        } else {
            accessGoogleFit();
        }
    }

    private void accessGoogleFit() {
        Log.d(this.getLocalClassName(), "authorized");

        if (mFitnessClient == null) {
            mFitnessClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.SESSIONS_API)
                    .addApi(Fitness.HISTORY_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .enableAutoManage(this, 0, result -> {
                        Log.i(MainActivity.this.getLocalClassName(), "Google Play services connection failed. Cause: " +
                                result.toString());
                        Snackbar.make(
                                main,
                                "Exception while connecting to Google Play services: " +
                                        result.getErrorMessage(),
                                Snackbar.LENGTH_INDEFINITE).show();
                    })
                    .build();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!mFitnessClient.isConnecting() && !mFitnessClient.isConnected()) {
                    mFitnessClient.connect();
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.e("GoogleFit", "RESULT_CANCELED");
            }
        } else {
            Log.e("GoogleFit", "requestCode NOT request_oauth");
        }
    }

    private void addUserDataAndroidHealth() {
        final String SESSION_NAME = "LogRunActivity";
        long startTime=0L;
        long endTime=0L;
        float totalCoveredDistanceInMiles = 0;
        float totalCalories = 0;
            Session mSession = new Session.Builder()
                    .setName(SESSION_NAME)
                    .setIdentifier(getString(R.string.app_name) + " " + System.currentTimeMillis())
                    .setDescription("Running Session Details")
                    .setStartTime((long) startTime, TimeUnit.MILLISECONDS)
                    .setEndTime((long) endTime, TimeUnit.MILLISECONDS)
                    .setActivity(FitnessActivities.WALKING)
                    .build();
            DataSource distanceSegmentDataSource = new DataSource.Builder()
                    .setAppPackageName(this.getPackageName())
                    .setDataType(DataType.AGGREGATE_DISTANCE_DELTA)
                    .setName("Total Distance Covered")
                    .setType(DataSource.TYPE_RAW)
                    .build();
            DataSet distanceDataSet = DataSet.create(distanceSegmentDataSource);
            DataPoint firstRunSpeedDataPoint = distanceDataSet.createDataPoint()
                    .setTimeInterval((long) startTime, (long) endTime, TimeUnit.MILLISECONDS);
            firstRunSpeedDataPoint.getValue(Field.FIELD_DISTANCE).setFloat((float) totalCoveredDistanceInMiles);
            distanceDataSet.add(firstRunSpeedDataPoint);
            DataSource caloriesDataSource = new DataSource.Builder()
                    .setAppPackageName(this.getPackageName())
                    .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                    .setName("Total Calories Burned")
                    .setType(DataSource.TYPE_RAW)
                    .build();
            DataSet caloriesDataSet = DataSet.create(caloriesDataSource);
            DataPoint caloriesDataPoint = caloriesDataSet.createDataPoint()
                    .setTimeInterval((long) startTime, (long) endTime, TimeUnit.MILLISECONDS);
            caloriesDataPoint.getValue(Field.FIELD_CALORIES).setFloat((float) totalCalories);
            caloriesDataSet.add(caloriesDataPoint);
            insertRequest = new SessionInsertRequest.Builder()
                    .setSession(mSession)
                    .addDataSet(distanceDataSet)
                    .addDataSet(caloriesDataSet)
                    .build();
            new InsertIUserHistoryDataGoogleFit().execute("", "", "");

    }

    private class InsertIUserHistoryDataGoogleFit extends AsyncTask<String,Void,String> {
        protected String doInBackground(String... urls) {
            PendingResult pendingResult =
                    Fitness.SessionsApi.insertSession(mFitnessClient, insertRequest);

            pendingResult.setResultCallback(result -> {
                if (result.getStatus().isSuccess()) {           // -- USER RECORD INSERTED SUCCESSFULLY.
                } else {
                    Log.i(MainActivity.this.getLocalClassName(), "Failed to insert running session: " + result.getStatus().getStatusMessage());
                }
            });
            return "";
        }
        protected void onProgressUpdate(Integer... progress) {
        }
        protected void onPostExecute(String result) {
        }
    }




    @Override
    public void onDataPoint(DataPoint dataPoint) {
        for (final Field field : dataPoint.getDataType().getFields()) {
            final Value value = dataPoint.getValue(field);
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Field: " + field.getName() + " Value: " + value, Toast.LENGTH_SHORT).show());
        }
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {

        SensorRequest request = new SensorRequest.Builder()
                .setDataSource(dataSource)
                .setDataType(dataType)
                .setSamplingRate(3, TimeUnit.SECONDS)
                .build();

        Fitness.SensorsApi.add(mFitnessClient, request, this)
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Log.e("GoogleFit", "SensorApi successfully added");
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        addUserDataAndroidHealth();
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
            Log.i(MainActivity.this.getLocalClassName(), "Connection lost.  Cause: Network Lost.");
        } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
            Log.i(MainActivity.this.getLocalClassName(),
                    "Connection lost.  Reason: Service Disconnected");
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!authInProgress) {
            try {
                authInProgress = true;
                connectionResult.startResolutionForResult(MainActivity.this, GOOGLE_FIT_PERMISSIONS_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {

            }
        } else {
            Log.e("GoogleFit", "authInProgress");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Fitness.SensorsApi.remove( mFitnessClient, this )
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            mFitnessClient.disconnect();
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
}
