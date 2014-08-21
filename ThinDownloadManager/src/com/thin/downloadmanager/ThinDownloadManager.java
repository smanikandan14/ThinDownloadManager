package com.thin.downloadmanager;

public class ThinDownloadManager implements DownloadManager {

    private DownloadRequestQueue mRequestQueue;
    private static ThinDownloadManager mInstance = null;

    /** private default constructor **/
    private ThinDownloadManager() {
    	mRequestQueue = new DownloadRequestQueue();
    	mRequestQueue.start();
	}

    /** Static method to access the singleton instance **/
    public static ThinDownloadManager getInstance() {
    	if (mInstance == null) {
    		mInstance = new ThinDownloadManager();
    	}
    	
    	return mInstance;
    }
    
	/**
     * Add a new download.  The download will start automatically once the download manager is
     * ready to execute it and connectivity is available.
     *
     * @param request the parameters specifying this download
     * @return an ID for the download, unique across the application.  This ID is used to make future
     * calls related to this download.
	 * @throws IllegalArgumentException
     */
	@Override
	public int add(DownloadRequest request) throws IllegalArgumentException {
		if(request == null) { 
			throw new IllegalArgumentException("DownloadRequest cannot be null");
		}

		return mRequestQueue.add(request);
	}


	@Override
	public int cancel(int downloadId) {
		return mRequestQueue.cancel(downloadId);
	}


	@Override
	public void cancelAll() {
        mRequestQueue.cancelAll();
	}


	@Override
	public int query(int downloadId) {
		return mRequestQueue.query(downloadId);
	}

	@Override
	public void release() {
		if(mRequestQueue != null) {
			mRequestQueue = null;
		}
		mInstance = null;
	}
}


