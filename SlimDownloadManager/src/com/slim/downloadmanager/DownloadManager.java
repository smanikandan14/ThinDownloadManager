package com.slim.downloadmanager;

import android.content.Context;

public interface DownloadManager {
	
	public int add( DownloadRequest request, 
			DownloadStatusListener listener);	

	public void cancel(int downloadId);

	public void cancelAll();

	public int query(int downloadId);	
	
	public void release();
}
