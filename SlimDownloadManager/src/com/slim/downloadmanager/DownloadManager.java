package com.slim.downloadmanager;

public interface DownloadManager {

    /**
     * Status when the download is currently pending.
     */
    public final static int STATUS_PENDING = 1 << 0;

    /**
     * Status when the download is currently pending.
     */
    public final static int STATUS_STARTED = 1 << 1;

    /**
     * Status when the download is currently running.
     */
    public final static int STATUS_RUNNING = 1 << 2;

    /**
     * Status when the download has successfully completed.
     */
    public final static int STATUS_SUCCESSFUL = 1 << 3;

    /**
     * Status when the download has failed.
     */
    public final static int STATUS_FAILED = 1 << 4;

    /**
     * Status when the download has failed.
     */
    public final static int STATUS_NOT_FOUND = 1 << 5;

    public int add( DownloadRequest request,
			DownloadStatusListener listener);	

	public void cancel(int downloadId);

	public void cancelAll();

	public int query(int downloadId);	
	
	public void release();
}
