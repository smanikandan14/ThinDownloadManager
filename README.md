ThinDownloadManager
===================

Thin DownloadManager is an android library primary to download files and to avoid using *DOWNLOAD_WITHOUT_NOTIFICATION* permission if you are using Android provided DownloadManager in your application. 


##Why ?
  There are few reasons why you might want to use this library.
  * In your application development there are situations where you wanted to download the file into your application's sandboxed cache or files directory where no one else can access to. *DownloadManager* provided by android does not have facility to download directly to application's cache or files directory. It can only accept external SDcard destination. So you have to have *android.permission.WRITE_EXTERNAL_STORAGE*
  ** /data/data/<package>/cache/ or /data/data/<pacakge>/files/
  * No additional permissions required. Any download initiated by your application using android DownloadManager would throw a progress notification on status bar letting user know that you are downloading a file. So you end up having *android.permission.DOWNLOAD_WITHOUT_NOTIFICATION*. When users install your app, they would be shown this permission and it is scary for them not to install your app. Why give a chance of user not installing your app for this permission. You definetly need this library in this case. 
  * Volley - Google recommended Networking library for android doesn't have options to download a file. 
  

##Usuage
  * ThinDownloadManager is a Singleton class. 
  * 
  add( DownloadRequest request);

	cancel(int downloadId);

	cancelAll();

	int query(int downloadId);	
	
	void release();
  * Change thread pool size.
  * DownloadRequestQueue
	/** Number of network request dispatcher threads to start. */
    private static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 1;


##No Permissions unless if you specify download destination to be in external SDCard. You might need *android.permission.WRITE_EXTERNAL_STORAGE* permission.

##Download

##License



