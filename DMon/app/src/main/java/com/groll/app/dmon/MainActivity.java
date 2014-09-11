package com.groll.app.dmon;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity {

    private static final String APPM_TAG = "AppMain";
    private List<String> units = Arrays.asList("usd", "eur", "btc");
    private int curUnitIndex = 0;
    private int updateInterval = 10; //mins
    private ScheduledExecutorService es;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadState();

        //Schedule and run update
        es = Executors.newSingleThreadScheduledExecutor();
        es.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                Log.d(APPM_TAG, "Scheduled update in progress");
                update();
            }
        }, 0, updateInterval, TimeUnit.MINUTES);

        //set handlers for update action
        View.OnClickListener handler = new UpdateClickHandler();
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.top_layout);
        layout.setOnClickListener(handler);
        TextView tv = (TextView) findViewById(R.id.cur_val_text);
        tv.setOnClickListener(new PriceClickHandler());
        tv = (TextView) findViewById(R.id.change_amt_text);
        tv.setOnClickListener(handler);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (es != null) {
            es.shutdown();
        }
        Log.d(APPM_TAG, "Application exiting");
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
    }

    private void loadState() {
        SharedPreferences prefs = getSharedPreferences("dmon", MODE_PRIVATE);
        curUnitIndex = prefs.getInt("unit_index", 0);
        if (curUnitIndex >= units.size()) {
            curUnitIndex = 0;
        }
        updateInterval = prefs.getInt("update_interval", 10);
    }

    private void saveState() {
        SharedPreferences.Editor ed = getSharedPreferences("dmon", MODE_PRIVATE).edit();
        ed.putInt("unit_index", curUnitIndex);
        ed.putInt("update_interval", updateInterval);
        ed.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.info_menu) {
            Intent infoIntent = new Intent(this, Info.class);
            startActivity(infoIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void update() {
        Log.d(APPM_TAG, "Running update");
        if (isPaused) {
            Log.d(APPM_TAG, "Skipping update because App is paused");
            return;
        }
        String url = "http://coinmarketcap-nexuist.rhcloud.com/api/doge";
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        new AsyncTask<URL, Void, CoinInfo>() {

            @Override
            protected CoinInfo doInBackground(URL... urls) {
                HttpURLConnection con = null;
                URL u = urls[0];
                CoinInfo info = new CoinInfo();
                try {
                    con = (HttpURLConnection) u.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("Content-type", "application/json");
                    final int response = con.getResponseCode();
                    if (response != 200) {
                        Log.i(APPM_TAG, "GET response=" + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Could not contact the server, error code " + response + ".\nPlease try again later.", Toast.LENGTH_LONG).show();
                            }
                        });
                        return null;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuffer resp = new StringBuffer(1024);
                    String line = br.readLine();
                    while (line != null) {
                        resp.append(line);
                        line = br.readLine();
                    }
                    br.close();
                    Log.d(APPM_TAG, resp.toString());

                    JSONObject js = new JSONObject(resp.toString());
                    String un = units.get(curUnitIndex);
                    info.curVal = js.getJSONObject("price").getString(un);
                    info.curVal = formatCurVal(info.curVal, un);
                    info.changeAmt = js.getString("change");
                    info.unitStr = un;
                    double time = js.getDouble("timestamp");
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis((long) (time * 1000));
                    info.lastUpdated = c.getTime();

                } catch (Exception e) {
                    Log.e(APPM_TAG, "Error during update: " + e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Could not contact the server.\nPlease try again later.", Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
                return info;
            }

            private String formatCurVal(String val, String unitType) {
                try {
                    double dval = Double.parseDouble(val);
                    String formatString;
                    if (unitType.equals("btc")) {
                        dval *= 100000000; //convert to satoshis
                        formatString = "%1.2f";
                    } else {
                        double tmp = dval;
                        int digits = 1;
                        while ((tmp *= 10) <= 1) digits++;
                        digits += 3; //show 4 sigfigs?
                        formatString = String.format("%%%d.%df", digits, digits);
                    }
                    return String.format(formatString, dval);
                } catch (NumberFormatException e) {
                    Log.e(APPM_TAG, "Error converting value to a number: " + e.toString());
                    return val;
                }
            }

            @Override
            protected void onPostExecute(CoinInfo result) {
                TextView updateView = (TextView) findViewById(R.id.updating_text);
                Animation in = new AlphaAnimation(0.0f, 1.0f);
                in.setDuration(250);
                updateView.startAnimation(in);
                updateView.setVisibility(View.VISIBLE);
                if (result != null) {

                    TextView valText = (TextView) findViewById(R.id.cur_val_text);
                    valText.setText(result.curVal);

                    TextView changeText = (TextView) findViewById(R.id.change_amt_text);
                    if (result.changeAmt.contains("-")) {
                        changeText.setText(result.changeAmt + "%");
                        changeText.setTextAppearance(getApplicationContext(), R.style.changeDownStyle);
                    } else {
                        changeText.setText("+" + result.changeAmt + "%");
                        changeText.setTextAppearance(getApplicationContext(), R.style.changeUpStyle);
                    }

                    TextView unitView = (TextView) findViewById(R.id.units);
                    String unitStr = result.unitStr;
                    if (unitStr.equals("btc")) unitStr = "satoshi";
                    unitStr += "/Ð";
                    unitView.setText(unitStr);

                    TextView lastUpdate = (TextView) findViewById(R.id.last_updated_text);
                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yy - K:mm:ss a z");
                    lastUpdate.setText("Last Update " + (result.lastUpdated == null ? "Unknown" : df.format(result.lastUpdated)));
                }
                Animation out = new AlphaAnimation(1.0f, 0.0f);
                out.setDuration(1500);
                updateView.startAnimation(out);
                updateView.setVisibility(View.INVISIBLE);

            }
        }.execute(u);


    }

    private static class CoinInfo {
        public String curVal = "0";
        public String unitStr = "usd";
        public String changeAmt = "0";
        public Date lastUpdated = null;
    }

    private class UpdateClickHandler implements View.OnClickListener {
        @Override
        public synchronized void onClick(View view) {
            Log.d(APPM_TAG, "Click handler triggered");
            update();
        }
    }

    private class PriceClickHandler implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Log.d(APPM_TAG, "Price click handler triggered");
            curUnitIndex = (curUnitIndex + 1) % units.size();
            update();
        }
    }
}
