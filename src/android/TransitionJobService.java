package com.cowbell.cordova.geofence;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by jupe on 22-02-18.
 */

public class TransitionJobService extends JobService {

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        PersistableBundle params = jobParameters.getExtras();
        final String url = params.getString("url");
        final String sessionToken = params.getString("sessionToken");
		final String applicationId = params.getString("applicationId");
		final String javascriptId = params.getString("javascriptId");
        final String id = params.getString("id");
        final String transition = params.getString("transition");
        final String date = params.getString("date");

        Thread thread = new Thread(() -> {
            try {
                sendTransitionToServer(url, applicationId, javascriptId, sessionToken, id, transition, date);
                jobFinished(jobParameters, false);
            } catch (Exception exception) {
                // It is possible to have no network during transition from Cellular to Wifi
                Log.e(GeofencePlugin.TAG, "Error while sending geofence transition, rescheduling", exception);
                jobFinished(jobParameters, true);
            }
        });
        thread.start();

        return true; // Async
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void sendTransitionToServer(String urlString, String applicationId, String javascriptId, String sessionToken, String id, String transition, String date) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);

        if (applicationId != null) {
            conn.setRequestProperty("X-Parse-Application-Id", applicationId);
        }

		if (javascriptId != null) {
            conn.setRequestProperty("X-Parse-Javascript-Key", javascriptId);
        }

		if (sessionToken != null) {
            conn.setRequestProperty("X-Parse-Session-Token", sessionToken);
        }

        conn.setRequestProperty("Content-Type", "application/json");

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        String json = "{ \"geofenceId\": \"" + id + "\",  \"transition\": \"" + transition + "\", \"date\": \"" + date +"\" }";
        Log.i(GeofencePlugin.TAG, "Sending Geofence transition to server: " + json);
        writer.write(json);
        writer.flush();
        writer.close();
        os.close();

        conn.connect();
        int responseCode = conn.getResponseCode();
        Log.i(GeofencePlugin.TAG, "Send Geofence transition to server: " + responseCode);
    }
}
