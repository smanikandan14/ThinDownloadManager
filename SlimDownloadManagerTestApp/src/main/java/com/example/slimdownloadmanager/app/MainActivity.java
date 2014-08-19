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
                .setRoamingAllowed(true)
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void updateDownloadStatus(int id, int status) {
                        System.out.println("###### updateDownloadStatus ######## "+id+" : "+status);
                    }

                    @Override
                    public void updateDownloadProgress(int id, int progress) {
                        System.out.println("######  updateDownloadProgress ######## "+id+" : "+progress);
                    }
                });

        int id1 = SlimDownloadManager.getInstance().add(downloadRequest);
        System.out.println("###### ID 1 ######## "+id1);

        downloadUri = Uri.parse("http://mobile-video-origin.offercdn.com/DEV1/GlobalFiles/17272.mp4");
        destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test1.mp4");
        downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.LOW)
                .setRoamingAllowed(true);

        int id2 = SlimDownloadManager.getInstance().add(downloadRequest);
        System.out.println("###### ID 2 ######## "+id2);
        downloadUri = Uri.parse("http://mobile-video-origin.offercdn.com/DEV2/GlobalFiles/17272.mp4");
        destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test2.mp4");
        downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.NORMAL)
                .setRoamingAllowed(true);

        int id3 = SlimDownloadManager.getInstance().add(downloadRequest);
        System.out.println("###### ID 3 ######## "+id3);
        downloadUri = Uri.parse("http://mobile-video-origin.offercdn.com/DEV/GlobalFiles/17272.mp4");
        destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test3.mp4");
        downloadRequest = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setRoamingAllowed(true);

        int id4 = SlimDownloadManager.getInstance().add(downloadRequest);
        System.out.println("###### ID 4 ######## "+id4);
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
