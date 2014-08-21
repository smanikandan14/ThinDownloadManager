ThinDownloadManager
===================

Thin DownloadManager is an android library to download files and to primary avoid using DOWNLOAD_WITHOUT_NOTIFICATION permission if you are using Android provided DownloadManager in your application. 


##Why ?
  There are two reasons why you might want to use this library.
  There are cases in your application development where you wanted to download the file into your application's sandboxed cache or files directory. DownloadManager provided by android doesnot have options to download directly to application's space. 
  * No permission. Any download initiated by your application would throw a progress bar on status bar and letting user know that you are downloading a file. So you end up having DOWNLOAD_WITHOUT_NOTIFICATION. You definetly need this library in this case. 
  * Volley - Networking library doesn't have options to download a file. 
  

##Usuage
  * Singleton ThinDownloadManager
  * 
  add( DownloadRequest request);

	cancel(int downloadId);

	cancelAll();

	int query(int downloadId);	
	
	void release();

##No Permissions

##Download

##License



