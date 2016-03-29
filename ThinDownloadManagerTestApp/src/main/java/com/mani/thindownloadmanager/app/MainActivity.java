package com.mani.thindownloadmanager.app;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadManager;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.RetryPolicy;
import com.thin.downloadmanager.ThinDownloadManager;
import java.io.File;

public class MainActivity extends AppCompatActivity {

    private ThinDownloadManager downloadManager;
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 4;

    Button mDownload1;
    Button mDownload2;
    Button mDownload3;
    Button mDownload4;
    Button mDownload5;

    Button mStartAll;
    Button mCancelAll;
    Button mListFiles;

    ProgressBar mProgress1;
    ProgressBar mProgress2;
    ProgressBar mProgress3;
    ProgressBar mProgress4;
    ProgressBar mProgress5;

    TextView mProgress1Txt;
    TextView mProgress2Txt;
    TextView mProgress3Txt;
    TextView mProgress4Txt;
    TextView mProgress5Txt;

    private static final String FILE1 = "https://dl.dropboxusercontent.com/u/25887355/test_photo1.JPG";
    private static final String FILE2 = "https://dl.dropboxusercontent.com/u/25887355/test_photo2.jpg";
    private static final String FILE3 = "https://dl.dropboxusercontent.com/u/25887355/test_song.mp3";
    private static final String FILE4 = "https://dl.dropboxusercontent.com/u/25887355/test_video.mp4";
    private static final String FILE5 = "http://httpbin.org/headers";
    private static final String FILE6 = "https://dl.dropboxusercontent.com/u/25887355/ThinDownloadManager.tar.gz";

    private RetryPolicy retryPolicy;
    private File filesDir;

    MyDownloadDownloadStatusListenerV1
        myDownloadStatusListener = new MyDownloadDownloadStatusListenerV1();

    int downloadId1;
    int downloadId2;
    int downloadId3;
    int downloadId4;
    int downloadId5;
    int downloadId6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDownload1 = (Button) findViewById(R.id.button1);
        mDownload2 = (Button) findViewById(R.id.button2);
        mDownload3 = (Button) findViewById(R.id.button3);
        mDownload4 = (Button) findViewById(R.id.button4);
        mDownload5 = (Button) findViewById(R.id.button_download_headers);

        mStartAll = (Button) findViewById(R.id.button5);
        mCancelAll = (Button) findViewById(R.id.button6);
        mListFiles = (Button) findViewById(R.id.button7);

        mProgress1Txt = (TextView) findViewById(R.id.progressTxt1);
        mProgress2Txt = (TextView) findViewById(R.id.progressTxt2);
        mProgress3Txt = (TextView) findViewById(R.id.progressTxt3);
        mProgress4Txt = (TextView) findViewById(R.id.progressTxt4);
        mProgress5Txt = (TextView) findViewById(R.id.progressTxt5);

        mProgress1 = (ProgressBar) findViewById(R.id.progress1);
        mProgress2 = (ProgressBar) findViewById(R.id.progress2);
        mProgress3 = (ProgressBar) findViewById(R.id.progress3);
        mProgress4 = (ProgressBar) findViewById(R.id.progress4);
        mProgress5 = (ProgressBar) findViewById(R.id.progress5);

        mProgress1.setMax(100);
        mProgress1.setProgress(0);

        mProgress2.setMax(100);
        mProgress2.setProgress(0);

        mProgress3.setMax(100);
        mProgress3.setProgress(0);

        mProgress4.setMax(100);
        mProgress4.setProgress(0);

        mProgress5.setMax(100);
        mProgress5.setProgress(0);

        retryPolicy = new DefaultRetryPolicy();
        filesDir = getExternalFilesDir("");

        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

        mDownload1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (downloadManager.query(downloadId1) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId1 = downloadManager.add(getRequest1());
                //}
            }
        });

        mDownload2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (downloadManager.query(downloadId2) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId2 = downloadManager.add(getRequest2());
                //}
            }
        });

        mDownload3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (downloadManager.query(downloadId3) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId3 = downloadManager.add(getRequest3());
                //}
            }
        });

        mDownload4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if (downloadManager.query(downloadId4) == DownloadManager.STATUS_NOT_FOUND) {
                    downloadId4 = downloadManager.add(getRequest4());
                //}
            }
        });

        mDownload5.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
                //if (downloadManager.query(downloadId5) == DownloadManager.STATUS_NOT_FOUND) {
                //    downloadId5 = downloadManager.add(downloadRequest5);
                //}

              //if (downloadManager.query(downloadId6) == DownloadManager.STATUS_NOT_FOUND) {
                  downloadId6 = downloadManager.add(getRequest6());
              //}

          }
        });

        mStartAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.cancelAll();
                downloadId1 = downloadManager.add(getRequest1());
                downloadId2 = downloadManager.add(getRequest2());
                downloadId3 = downloadManager.add(getRequest3());
                downloadId4 = downloadManager.add(getRequest4());
                downloadId5 = downloadManager.add(getRequest5());
            }
        });

        mCancelAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.cancelAll();
            }
        });

        mListFiles.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showInternalFilesDir();
            }
        });

        mProgress1Txt.setText("Download1 - WiFi-Only");
        mProgress2Txt.setText("Download2");
        mProgress3Txt.setText("Download3");
        mProgress4Txt.setText("Download4");
        mProgress5Txt.setText("Download5");
    }

    private DownloadRequest getRequest1() {
        return new DownloadRequest(Uri.parse(FILE1))
                .setDestinationURI(Uri.parse(filesDir+"/test_photo1.JPG")).setPriority(DownloadRequest.Priority.LOW)
                .setRetryPolicy(retryPolicy)
                .setDownloadContext("Download1")
                .setWifiOnly(true, MainActivity.this.getApplicationContext())
                .setStatusListener(myDownloadStatusListener);
    }

    private DownloadRequest getRequest2() {
        return new DownloadRequest(Uri.parse(FILE2))
                .setDestinationURI(Uri.parse(filesDir+"/test_photo2.jpg")).setPriority(DownloadRequest.Priority.LOW)
                .setDownloadContext("Download2")
                .setWifiOnly(false, this.getApplicationContext())
                .setStatusListener(myDownloadStatusListener);
    }

    private DownloadRequest getRequest3() {
        return new DownloadRequest(Uri.parse(FILE3))
                .setDestinationURI(Uri.parse(filesDir+"/test_song.mp3")).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download3")
                .setStatusListener(myDownloadStatusListener);
    }

    private DownloadRequest getRequest4() {
        return new DownloadRequest(Uri.parse(FILE4))
                .setRetryPolicy(new DefaultRetryPolicy(5000, 3, 2f))
                .setDestinationURI(Uri.parse(filesDir+"/test_video.mp4")).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download4")
                .setStatusListener(myDownloadStatusListener);
    }

    private DownloadRequest getRequest5() {
        return new DownloadRequest(Uri.parse(FILE5))
                .addCustomHeader("Auth-Token", "myTokenKey")
                .addCustomHeader("User-Agent", "Thin/Android")
                .setDestinationURI(Uri.parse(filesDir+"/headers.json")).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download5")
                .setStatusListener(myDownloadStatusListener);
    }

    private DownloadRequest getRequest6() {
        return new DownloadRequest(Uri.parse(FILE6))
                .setDestinationURI(Uri.parse(filesDir+"/wtfappengine.zip")).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext("Download6")
                .setStatusListener(myDownloadStatusListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("######## onDestroy ######## ");
        downloadManager.release();
    }

    private void showInternalFilesDir() {
        File internalFile = new File(getExternalFilesDir("").getPath());
        File files[] = internalFile.listFiles();
        String contentText = "";
        if( files.length == 0 ) {
            contentText = "No Files Found";
        }

        for (File file : files) {
            contentText += file.getName()+" "+file.length()+" \n\n ";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog internalCacheDialog = builder.create();
        LayoutInflater inflater = internalCacheDialog.getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.layout_files, null);
        TextView content = (TextView) dialogLayout.findViewById(R.id.filesList);
        content.setText(contentText);

        builder.setView(dialogLayout);
        builder.show();

    }

    class MyDownloadDownloadStatusListenerV1 implements DownloadStatusListenerV1 {

        @Override
        public void onDownloadComplete(DownloadRequest request) {
            final int id = request.getDownloadId();
            String wifiOnlyString = request.isWifiOnly() ? "WiFi-Only" : "";

            System.out.println("######## onDownloadComplete ###### id: "+id);
            if (id == downloadId1) {
                mProgress1Txt.setText(request.getDownloadContext() + " "+wifiOnlyString+" id: "+id+" Completed");

            } else if (id == downloadId2) {
                mProgress2Txt.setText(request.getDownloadContext() + " "+wifiOnlyString+" id: "+id+" Completed");

            } else if (id == downloadId3) {
                mProgress3Txt.setText(request.getDownloadContext() + " "+wifiOnlyString+" id: "+id+" Completed");

            } else if (id == downloadId4) {
                mProgress4Txt.setText(request.getDownloadContext() + " "+wifiOnlyString+" id: "+id+" Completed");
            } else if (id == downloadId5) {
              mProgress5Txt.setText(request.getDownloadContext() + " "+wifiOnlyString+" id: "+id+" Completed");
            }
        }

        @Override
        public void onDownloadFailed(DownloadRequest request, int errorCode, String errorMessage) {
            final int id = request.getDownloadId();
            String wifiOnlyString = request.isWifiOnly() ? "WiFi-Only" : "";

            System.out.println("######## onDownloadFailed ###### "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
            if (id == downloadId1) {
                mProgress1Txt.setText("Download1 "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
                mProgress1.setProgress(0);
            } else if (id == downloadId2) {
                mProgress2Txt.setText("Download2 "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
                mProgress2.setProgress(0);

            } else if (id == downloadId3) {
                mProgress3Txt.setText("Download3 "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
                mProgress3.setProgress(0);

            } else if (id == downloadId4) {
                mProgress4Txt.setText("Download4 "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
                mProgress4.setProgress(0);
            } else if (id == downloadId5) {
              mProgress5Txt.setText("Download5 "+wifiOnlyString+" id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
              mProgress5.setProgress(0);
            }
        }

        @Override
        public void onProgress(DownloadRequest request, long totalBytes, long downloadedBytes, int progress) {
            int id = request.getDownloadId();
            String wifiOnlyString = request.isWifiOnly() ? "WiFi-Only" : "";

            System.out.println("######## onProgress ###### "+id+" : "+totalBytes+" : "+downloadedBytes+" : "+progress);
            if (id == downloadId1) {
                mProgress1Txt.setText("Download1 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
                mProgress1.setProgress(progress);

            } else if (id == downloadId2) {
                mProgress2Txt.setText("Download2 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
                mProgress2.setProgress(progress);

            } else if (id == downloadId3) {
                mProgress3Txt.setText("Download3 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
                mProgress3.setProgress(progress);

            } else if (id == downloadId4) {
                mProgress4Txt.setText("Download4 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
                mProgress4.setProgress(progress);
            } else if (id == downloadId5) {
              mProgress5Txt.setText("Download5 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
              mProgress5.setProgress(progress);
            } else if (id == downloadId6) {
                mProgress5Txt.setText("Download6 "+wifiOnlyString+" id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
                mProgress5.setProgress(progress);
            }
        }
    }

    private String getBytesDownloaded(int progress, long totalBytes) {
        //Greater than 1 MB
        long bytesCompleted = (progress * totalBytes)/100;
        if (totalBytes >= 1000000) {
            return (""+(String.format("%.1f", (float)bytesCompleted/1000000))+ "/"+ ( String.format("%.1f", (float)totalBytes/1000000)) + "MB");
        } if (totalBytes >= 1000) {
            return (""+(String.format("%.1f", (float)bytesCompleted/1000))+ "/"+ ( String.format("%.1f", (float)totalBytes/1000)) + "Kb");

        } else {
            return ( ""+bytesCompleted+"/"+totalBytes );
        }
    }

}