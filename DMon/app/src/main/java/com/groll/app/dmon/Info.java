package com.groll.app.dmon;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.groll.app.dmon.R;


public class Info extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        ActionBar ab = getActionBar();
        if(ab != null) {
           ab.setDisplayHomeAsUpEnabled(true);
        }
        TextView dogeAddr = (TextView) findViewById(R.id.donate_url);
        final String url = getString(R.string.donate_url);
        dogeAddr.setText(Html.fromHtml("<a href=\"" + url + "\"><u>" + url + "</u></a>"));
        dogeAddr.setMovementMethod(LinkMovementMethod.getInstance());
        dogeAddr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dogeIntent = new Intent(Intent.ACTION_VIEW);
                dogeIntent.setData(Uri.parse(url));
                try {
                    startActivity(dogeIntent);
                } catch (ActivityNotFoundException e) {
                    Log.e("InfoAct", "No activity available for dogecoin URL: " + e);
                    String addr = getString(R.string.donate_addr_only);
                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData cd = ClipData.newPlainText("dmon dogecoin address", addr);
                    cm.setPrimaryClip(cd);
                    String msg = "No dogecoin wallet installed!\nCopied address to clipboard: \"" + addr + "\"";
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //  getMenuInflater().inflate(R.menu.info, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == android.R.id.home) {
//            NavUtils.navigateUpFromSameTask(this);
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }
}
