package com.mani.thindownloadmanager.app;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.thin.downloadmanager.DownloadManager;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;


public class MainActivity extends ActionBarActivity {

  private ThinDownloadManager downloadManager;
  private static final int DOWNLOAD_THREAD_POOL_SIZE = 4;

  Button mDownload1;
  Button mDownload2;
  Button mDownload3;
  Button mDownload4;

  Button mStartAll;
  Button mCancelAll;
  Button mListFiles;

  ProgressBar mProgress1;
  ProgressBar mProgress2;
  ProgressBar mProgress3;
  ProgressBar mProgress4;

  TextView mProgress1Txt;
  TextView mProgress2Txt;
  TextView mProgress3Txt;
  TextView mProgress4Txt;

  private static final String FILE1 = "https://dl.dropboxusercontent.com/u/25887355/test_photo1.JPG";
  private static final String FILE2 = "https://dl.dropboxusercontent.com/u/25887355/test_photo2.jpg";
  private static final String FILE3 = "https://dl.dropboxusercontent.com/u/25887355/test_song.mp3";
  private static final String FILE4 = "https://dl.dropboxusercontent.com/u/25887355/test_video.mp4";

  MyDownloadStatusListener myDownloadStatusListener = new MyDownloadStatusListener();

  int downloadId1;
  int downloadId2;
  int downloadId3;
  int downloadId4;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mDownload1 = (Button) findViewById(R.id.button1);
    mDownload2 = (Button) findViewById(R.id.button2);
    mDownload3 = (Button) findViewById(R.id.button3);
    mDownload4 = (Button) findViewById(R.id.button4);

    mStartAll = (Button) findViewById(R.id.button5);
    mCancelAll = (Button) findViewById(R.id.button6);
    mListFiles = (Button) findViewById(R.id.button7);

    mProgress1Txt = (TextView) findViewById(R.id.progressTxt1);
    mProgress2Txt = (TextView) findViewById(R.id.progressTxt2);
    mProgress3Txt = (TextView) findViewById(R.id.progressTxt3);
    mProgress4Txt = (TextView) findViewById(R.id.progressTxt4);

    mProgress1 = (ProgressBar) findViewById(R.id.progress1);
    mProgress2 = (ProgressBar) findViewById(R.id.progress2);
    mProgress3 = (ProgressBar) findViewById(R.id.progress3);
    mProgress4 = (ProgressBar) findViewById(R.id.progress4);

    mProgress1.setMax(100);
    mProgress1.setProgress(0);

    mProgress2.setMax(100);
    mProgress2.setProgress(0);

    mProgress3.setMax(100);
    mProgress3.setProgress(0);

    mProgress4.setMax(100);
    mProgress4.setProgress(0);

    downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

    Uri downloadUri = Uri.parse(FILE1);
    Uri destinationUri = Uri.parse(this.getFilesDir().toString() + "/test_photo1.JPG");
    final DownloadRequest downloadRequest1 = new DownloadRequest(downloadUri)
        .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.LOW)
        .setDownloadListener(myDownloadStatusListener);

    downloadUri = Uri.parse(FILE2);
    destinationUri = Uri.parse(this.getFilesDir().toString() + "/test_photo2.jpg");
    final DownloadRequest downloadRequest2 = new DownloadRequest(downloadUri)
        .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.LOW)
        .setDownloadListener(myDownloadStatusListener);

    downloadUri = Uri.parse(FILE3);
    destinationUri = Uri.parse(this.getFilesDir().toString() + "/test_song.mp3");
    final DownloadRequest downloadRequest3 = new DownloadRequest(downloadUri)
        .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
        .setDownloadListener(myDownloadStatusListener);

    downloadUri = Uri.parse(FILE4);
    destinationUri = Uri.parse(this.getFilesDir().toString() + "/test_video.mp4");
    final DownloadRequest downloadRequest4 = new DownloadRequest(downloadUri)
        .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
        .setDownloadListener(myDownloadStatusListener);

    mDownload1.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (downloadManager.query(downloadId1) == DownloadManager.STATUS_NOT_FOUND) {
          downloadId1 = downloadManager.add(downloadRequest1);
        }
      }
    });

    mDownload2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (downloadManager.query(downloadId2) == DownloadManager.STATUS_NOT_FOUND) {
          downloadId2 = downloadManager.add(downloadRequest2);
        }
      }
    });

    mDownload3.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (downloadManager.query(downloadId3) == DownloadManager.STATUS_NOT_FOUND) {
          downloadId3 = downloadManager.add(downloadRequest3);
        }
      }
    });

    mDownload4.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (downloadManager.query(downloadId4) == DownloadManager.STATUS_NOT_FOUND) {
          downloadId4 = downloadManager.add(downloadRequest4);
        }
      }
    });

    mStartAll.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        downloadManager.cancelAll();
        downloadId1 = downloadManager.add(downloadRequest1);
        downloadId2 = downloadManager.add(downloadRequest2);
        downloadId3 = downloadManager.add(downloadRequest3);
        downloadId4 = downloadManager.add(downloadRequest4);
      }
    });

    mCancelAll.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        downloadManager.cancelAll();
      }
    });

    mListFiles.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showInternalFilesDir();
      }
    });

    mProgress1Txt.setText("Download1");
    mProgress2Txt.setText("Download2");
    mProgress3Txt.setText("Download3");
    mProgress4Txt.setText("Download4");
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    System.out.println("######## onDestroy ######## ");
    downloadManager.release();
  }

  private void showInternalFilesDir() {
    File internalFile = new File(getFilesDir().getPath());
    File files[] = internalFile.listFiles();
    String contentText = "";
    if (files.length == 0) {
      contentText = "No Files Found";
    }

    for (File file : files) {
      contentText += file.getName() + " " + file.length() + " \n\n ";
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

  class MyDownloadStatusListener implements DownloadStatusListener {

    @Override
    public void onDownloadComplete(int id) {
      System.out.println("###### onDownloadComplete ######## " + id);

      if (id == downloadId1) {
        mProgress1Txt.setText("Download1 id: " + id + " Completed");

      } else if (id == downloadId2) {
        mProgress2Txt.setText("Download2 id: " + id + " Completed");

      } else if (id == downloadId3) {
        mProgress3Txt.setText("Download3 id: " + id + " Completed");

      } else if (id == downloadId4) {
        mProgress4Txt.setText("Download4 id: " + id + " Completed");
      }
    }

    @Override
    public void onDownloadFailed(int id, int errorCode, String errorMessage) {
      System.out.println("###### onDownloadFailed ######## " + id + " : " + errorCode + " : " + errorMessage);
      if (id == downloadId1) {
        mProgress1Txt.setText("Download1 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
        mProgress1.setProgress(0);
      } else if (id == downloadId2) {
        mProgress2Txt.setText("Download2 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
        mProgress2.setProgress(0);

      } else if (id == downloadId3) {
        mProgress3Txt.setText("Download3 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
        mProgress3.setProgress(0);

      } else if (id == downloadId4) {
        mProgress4Txt.setText("Download4 id: " + id + " Failed: ErrorCode " + errorCode + ", " + errorMessage);
        mProgress4.setProgress(0);
      }
    }

    @Override
    public void onProgress(int id, long totalBytes, long downloadedBytes, int progress) {

      if (id == downloadId1) {
        mProgress1Txt.setText("Download1 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
        mProgress1.setProgress(progress);

      } else if (id == downloadId2) {
        mProgress2Txt.setText("Download2 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
        mProgress2.setProgress(progress);

      } else if (id == downloadId3) {
        mProgress3Txt.setText("Download3 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
        mProgress3.setProgress(progress);

      } else if (id == downloadId4) {
        mProgress4Txt.setText("Download4 id: " + id + ", " + progress + "%" + "  " + getBytesDownloaded(progress, totalBytes));
        mProgress4.setProgress(progress);
      }
    }
  }

  private String getBytesDownloaded(int progress, long totalBytes) {
    //Greater than 1 MB
    long bytesCompleted = (progress * totalBytes) / 100;
    if (totalBytes >= 1000000) {
      return ("" + (String.format("%.1f", (float) bytesCompleted / 1000000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000000)) + "MB");
    }
    if (totalBytes >= 1000) {
      return ("" + (String.format("%.1f", (float) bytesCompleted / 1000)) + "/" + (String.format("%.1f", (float) totalBytes / 1000)) + "Kb");

    } else {
      return ("" + bytesCompleted + "/" + totalBytes);
    }
  }
}