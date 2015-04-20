ThinDownloadManager
===================

Thin DownloadManager is an android library primary to download files and to avoid using *DOWNLOAD_WITHOUT_NOTIFICATION* permission when using Android provided DownloadManager in your application.


##Why ?
  There are few reasons why you might want to use this library.

  * There are situations where you wanted to download a file into application's sandboxed cache or files directory where no one else can access to. **DownloadManager** provided by android does not have facility to download directly to application's cache or files directory **(/data/data/<package>/cache/ or /data/data/<pacakge>/files/)**. It can only accept destination in external SDcard as download destination. And if you are not using application's external file directory as destination i.e *(setDestinationInExternalFilesDir())* you have to have *android.permission.WRITE_EXTERNAL_STORAGE*

  Most of the times we download using Android's DownloadManager to external files directory and upon successful completion move the downloaded file to the sandboxed application's cache/file directory to avoid writing a own download manager which is a bit tedious. This library is handy in such situations.

  * **No additional permissions required.** Any download initiated by your application using android DownloadManager would throw a progress notification on status bar letting user know that you are downloading a file. So you end up using *setVisibleInDownloadsUi(false)* & having this permission *android.permission.DOWNLOAD_WITHOUT_NOTIFICATION*. When users install your app, they would be shown this permission and it makes them scary not to install your app because you are downloading some files without user's notification. Why give a chance of user not installing your app for this permission. You definetly need this library in this case.

  * **Volley** - Google recommended Networking library for android doesn't have options to download a file.


##Usuage
####**DownloadStatusListener**
  * Provides call back option to know when the download is completed, when download failed and reaso for failure, and to know the progress of the download.
``` java
    //Callback when download is successfully completed
    void onDownloadComplete (int id);

    //Callback if download is failed. Corresponding error code and
    //error messages are provided
    void onDownloadFailed (int id, int errorCode, String errorMessage);

    //Callback provides download progress
    void onProgress (int id, long totalBytes, long downlaodedBytes, int progress);

```

####**DownloadRequest**
  * Takes all the necessary information required for download.
  * Download URI, Destination URI.
  * Set Priority for request as HIGH or MEDIUM or LOW.
  * Takes Callback listener DownloadStatusListener
  * Use custom Http Headers for a download request
  * You can set a Retry Policy

     ``` java
        Uri downloadUri = Uri.parse("http://tcrn.ch/Yu1Ooo1");
        Uri destinationUri = Uri.parse(this.getExternalCacheDir().toString()+"/test.mp4");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .addCustomHeader("Auth-Token", "YourTokenApiKey")
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
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
  * You can pass the thread pool size as an argument in the constructor. Size should be in range 1 - 4. If no argument is passed the thread pool size is 1.
  	``` java
    private ThinDownloadManager downloadManager;
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;

    .....

    downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);

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

##Setup
Download and include the [JAR](https://github.com/smanikandan14/ThinDownloadManager/raw/master/library-jar/thindownloadmanager-1.0.0.jar) in your project, or add the dependency in your build.gradle:

```java
dependencies {
    compile 'com.mani:thindownloadmanager:1.0.0'
}
```

##Download
* The source code of sample app code is available for you to play around and the app itself is available for download from play store :


<a href="https://play.google.com/store/apps/details?id=com.mani.thindownloadmanager.app">
  <img alt="Get it on Google Play"
       src="https://developer.android.com/images/brand/en_app_rgb_wo_60.png" />
</a>

* Sample app demonstrates with 4 thread pool size and download three different formats of files jpg, mp3, mp4.
* The files are downloaded to applications sandboxed files directory. */data/data/<package>/files.

##Credits
https://android.googlesource.com/platform/packages/providers/DownloadProvider/

NOTE: Android's DownloadManager has plenty of features which is not available in ThinDownloadManager. So analyse your requirement thoroughly and decide which one to use.

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



