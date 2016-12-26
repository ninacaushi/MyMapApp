package com.example.user.mymap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class JSONTask extends AsyncTask<String,String,String> {
    private ProgressDialog progressDialog;
    private Activity act;

    public JSONTask(Activity act) {
        this.act=act;
    }
    protected void onPreExecute(){
        super.onPreExecute();
        progressDialog = new ProgressDialog(act);
        progressDialog.setMessage("Downloading your data. Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection connection = null;
        BufferedReader bf = null;
        try{
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            bf = new BufferedReader(new InputStreamReader( connection.getInputStream()));

            StringBuffer buffer = new StringBuffer();
            String line ="";

            while((line = bf.readLine()) !=null){
                buffer.append(line+"\n");
                Log.d("Response: ","> "+line);
            }
            return buffer.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            if(connection != null){
                connection.disconnect();
            }
            try{
                if(bf !=null){
                    bf.close();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(progressDialog.isShowing()){
            progressDialog.dismiss();
        }
     }
}