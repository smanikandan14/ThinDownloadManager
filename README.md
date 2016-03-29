ThinDownloadManager
===================

Thin DownloadManager is an android library primary to download files and to avoid using *DOWNLOAD_WITHOUT_NOTIFICATION* permission when using Android provided DownloadManager in your application.


##Why ?
  There are few reasons why you might want to use this library.

  * There are situations where you wanted to download a file into application's sandboxed cache or files directory where no one else can access to. **DownloadManager** provided by android does not have facility to download directly to application's cache or files directory **(/data/data/<package>/cache/ or /data/data/<pacakge>/files/)**. It can only accept destination in external SDcard as download destination. And if you are not using application's external file directory as destination i.e *(setDestinationInExternalFilesDir())* you have to have *android.permission.WRITE_EXTERNAL_STORAGE*

  Most of the times we download using Android's DownloadManager to external files directory and upon successful completion move the downloaded file to the sandboxed application's cache/file directory to avoid writing a own download manager which is a bit tedious. This library is handy in such situations.

  * **No additional permissions required.** Any download initiated by your application using android DownloadManager would throw a progress notification on status bar letting user know that you are downloading a file. So you end up using *setVisibleInDownloadsUi(false)* & having this permission *android.permission.DOWNLOAD_WITHOUT_NOTIFICATION*. When users install your app, they would be shown this permission and it makes them scary not to install your app because you are downloading some files without user's notification. Why give a chance of user not installing your app for this permission. You definetly need this library in this case.
    -Additional permissions may be required if downloading to external storage or using the WiFi-only DownloadRequest feature.

  * **Volley** - Google recommended Networking library for android doesn't have options to download a file.


##Usuage
####**DownloadStatusListener (Deprecated)**
  * Provides call back option to know when the download is completed, failed and reason for failure, and to know the progress of the download.
``` java
    //Callback when download is successfully completed
    void onDownloadComplete (int id);

    //Callback if download is failed. Corresponding error code and
    //error messages are provided
    void onDownloadFailed (int id, int errorCode, String errorMessage);

    //Callback provides download progress
    void onProgress (int id, long totalBytes, long downlaodedBytes, int progress);

```
####**DownloadStatusListenerV1**
  * Provides call back option to know when the download is completed, failed and reason for failure, and to know the progress of the download. DownloadRequest is given back in the callback so that you can easily set some Object as context to download request and get the context object back from the request object.
``` java
    //Callback when download is successfully completed
    void onDownloadComplete(DownloadRequest downloadRequest);

    //Callback if download is failed. Corresponding error code and
    //error messages are provided
    void onDownloadFailed(DownloadRequest downloadRequest, int errorCode, String errorMessage);


    //Callback provides download progress
    void onProgress(DownloadRequest downloadRequest, long totalBytes, long downloadedBytes, int progress);

```

####**DownloadRequest**
  * Takes all the necessary information required for download.
  * Download URI, Destination URI.
  * Set Priority for request as HIGH or MEDIUM or LOW.
  * Takes Callback listener DownloadStatusListener
  * You can use custom Http Headers.
  * You can set a Retry Policy.
  * You can set to keep the destination file on download failure.
  * You can set to only download over WiFi: If WiFi is unavailable, WiFi-only downloads are queued. When Wifi connects, WiFi-only downloads are started. If WiFi is disabled, it is up to the consuming app or user to enable WiFi. Using this requires additional permissions: see permissions section below.

     ``` java
        Uri downloadUri = Uri.parse("http://tcrn.ch/Yu1Ooo1");
        Uri destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test.mp4");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .addCustomHeader("Auth-Token", "YourTokenApiKey")
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext(downloadContextObject)//Optional
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {

                    }

                    @Override
                    public void onDownloadFailed(int id, int errorCode, String errorMessage) {

                    }

                    @Override
                    public void onProgress(int id, long totalBytes, long downlaodedBytes, int progress)) {

                    }
                });

     ```

####**ThinDownloadManager**
  * The number of threads used to perform parallel download is determined by the available processors on the device. Uses `Runtime.getRuntime().availableProcessors()` api.
  * You can construct with a Handler to use for download status callbacks.
  
  	``` java
    private ThinDownloadManager downloadManager;
    .....
    
    downloadManager = new ThinDownloadManager();
    
    ....
```

  * To start a download use *add( DownloadRequest request)*
   	```java
   	int downloadId = downloadManager.add(downloadRequest);
   	```

  * To cancel a particular download use *cancel(int downloadId)* by passing download id.
  	- Returns 1 if successfull cancelled.
  	- Returns -1 if supplied download id is not found.

  	```java
  	int status = downloadManager.cancel(downloadId);
  	```

  * To cancel all running requests use *cancelAll()*
  	```java
  	downloadManager.cancelAll();
  	```

  * To query for a particular download use *query(int downloadId)*

    The possible status could be
  	- STATUS_PENDING
  	- STATUS_STARTED
  	- STATUS_RUNNING

  	```java
  	int status = downloadManager.query(downloadId);
  	```
  * To release all the resources used by download manager use *release()*.

  	```java
  	downloadManager.release();
  	```


##No Permissions Required
  * Unless if you specify download destination to be in external public SDCard location.You might need *android.permission.WRITE_EXTERNAL_STORAGE* permission.
  * Using WiFi-only downloads requires: ACCESS_NETWORK_STATE, CHANGE_NETWORK_STATE, ACCESS_WIFI_STATE, and WAKE_LOCK.

##Setup
Include below line your build.gradle:

```java
dependencies {
    compile 'com.mani:ThinDownloadManager:1.3.0'
}
```
Make sure you included jcenter() in your repositories section.

##Download
* The source code of sample app code is available for you to play around and the app itself is available for download from play store :

<a href="https://play.google.com/store/apps/details?id=com.mani.thindownloadmanager.app&utm_source=global_co&utm_medium=prtnr&utm_content=Mar2515&utm_campaign=PartBadge&pcampaignid=MKT-AC-global-none-all-co-pr-py-PartBadges-Oct1515-1"><img alt="Get it on Google Play" width="250" height="100" src="https://play.google.com/intl/en_us/badges/images/apps/en-play-badge.png" ></a>

* Sample app demonstrates with 4 thread pool size and download three different formats of files jpg, mp3, mp4.
* The files are downloaded to applications sandboxed files directory. */data/data/<package>/files.

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-ThinDownloadManager-green.svg?style=flat)](https://android-arsenal.com/details/1/2393)

##Credits
https://android.googlesource.com/platform/packages/providers/DownloadProvider/

NOTE: Android's DownloadManager has plenty of features which is not available in ThinDownloadManager. For ex. pause and continue download when network connectivity changes.So analyse your requirement thoroughly and decide which one to use.

https://www.virag.si/2015/01/publishing-gradle-android-library-to-jcenter/ 
Steps for uploading to bintray.

##License
```
 Copyright 2013 Mani Selvaraj

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
```



