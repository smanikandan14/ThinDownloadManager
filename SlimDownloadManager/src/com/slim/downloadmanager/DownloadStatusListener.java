package com.slim.downloadmanager;

public interface DownloadStatusListener {
	
	void updateDownloadStatus( long id, int status, int progress);
	
}
