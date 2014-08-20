package com.slim.downloadmanager;

public interface DownloadStatusListener {
	void onDownloadComplete (int id);
    void onDownloadFailed (int id, int errorCode, String errorMessage);
	void onProgress (int id, int progress);
}
