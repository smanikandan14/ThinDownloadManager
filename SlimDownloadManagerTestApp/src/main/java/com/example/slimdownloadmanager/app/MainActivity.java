package com.example.slimdownloadmanager.app;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.slim.downloadmanager.DownloadRequest;
import com.slim.downloadmanager.DownloadStatusListener;
import com.slim.downloadmanager.SlimDownloadManager;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Uri downloadUri = Uri.parse("http://mobile-video-origin.offercdn.com/DEV/GlobalFiles/17272.mp4");
        //Uri destinationUri = Uri.parse(this.getFilesDir().toString()+"/test.mp4");
        Uri destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test.mp4");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setRoamingAllowed(true);
        SlimDownloadManager.getInstance().add(downloadRequest, new DownloadStatusListener() {
            @Override
            public void updateDownloadStatus(long id, int status, int progress) {
                System.out.println("###### UpdateDownloadStatus id status progress ######## "+id+" : "+status+" : "+progress);
            }
        });
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
