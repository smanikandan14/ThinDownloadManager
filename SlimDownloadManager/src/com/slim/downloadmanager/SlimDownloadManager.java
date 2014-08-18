package com.slim.downloadmanager;

public class SlimDownloadManager implements DownloadManager {

    private DownloadRequestQueue mRequestQueue;
    private static SlimDownloadManager mInstance = null;
    
    private SlimDownloadManager() {    	
    	mRequestQueue = new DownloadRequestQueue();
    	mRequestQueue.start();
	}
    
    public static SlimDownloadManager getInstance() {
    	if (mInstance == null) {
    		mInstance = new SlimDownloadManager();
    	}
    	
    	return mInstance;
    }
    
	/**
     * Add a new download.  The download will start automatically once the download manager is
     * ready to execute it and connectivity is available.
     *
     * @param request the parameters specifying this download
     * @return an ID for the download, unique across the system.  This ID is used to make future
     * calls related to this download.
	 * @throws IllegalAccessException 
     */
	@Override
	public int add(DownloadRequest request,
			DownloadStatusListener listener) throws IllegalArgumentException {
		if(request == null) { 
			throw new IllegalArgumentException("DownloadRequest cannot be null");
		}

		if(listener == null) { 
			throw new IllegalArgumentException("DownloadStatusListener cannot be null");
		}

		return mRequestQueue.add(request,listener);
	}


	@Override
	public void cancel(int downloadId) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void cancelAll() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public int query(int downloadId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void release() {
		if(mRequestQueue != null) {
			//mRequestQueue.
			mRequestQueue = null;
		}
		
		mInstance = null;
	}
}


