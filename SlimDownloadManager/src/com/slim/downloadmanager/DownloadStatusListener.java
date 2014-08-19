package com.slim.downloadmanager;

public interface DownloadStatusListener {
	
	void updateDownloadStatus (int id, int status);
	void updateDownloadProgress (int id, int progress);
}
