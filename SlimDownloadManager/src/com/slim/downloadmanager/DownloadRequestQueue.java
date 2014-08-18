package com.slim.downloadmanager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRequestQueue {

	/** Number of network request dispatcher threads to start. */
    private static final int DEFAULT_DOWNLOAD_THREAD_POOL_SIZE = 1;

    /**
     * The set of all requests currently being processed by this RequestQueue. A Request
     * will be in this set if it is waiting in any queue or currently being processed by
     * any dispatcher.
     */
    private final Set<DownloadRequest> mCurrentRequests = new HashSet<DownloadRequest>();

    /** The queue of requests that are actually going out to the network. */
    private final PriorityBlockingQueue<DownloadRequest> mDownloadQueue =
        new PriorityBlockingQueue<DownloadRequest>();


	// Thread pool executor
	DownloadDispatcher[] mDownloadDispatchers;

	/** Used for generating monotonically-increasing sequence numbers for requests. */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

	// Single ton instance. 
	public DownloadRequestQueue() {		
		mDownloadDispatchers = new DownloadDispatcher[DEFAULT_DOWNLOAD_THREAD_POOL_SIZE];
	}

	public int add(DownloadRequest request,
			DownloadStatusListener listener) {
		int downloadId = getDownloadId();
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setDownloadRequestQueue(this);
        // Set the download listener.
        request.setmDownloadListener(listener);
        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process requests in the order they are added.
        request.setDownloadId(downloadId);		
        mDownloadQueue.add(request);
        
		return downloadId;		
	}
	
	public void start() {
        stop();  // Make sure any currently running dispatchers are stopped.
    
        // Create network dispatchers (and corresponding threads) up to the pool size.
        for (int i = 0; i < mDownloadDispatchers.length; i++) {
            DownloadDispatcher networkDispatcher = new DownloadDispatcher(mDownloadQueue);
            mDownloadDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }

    /**
     * Stops the cache and network dispatchers.
     */
    public void stop() {
		for (int i = 0; i < mDownloadDispatchers.length; i++) {
            if (mDownloadDispatchers[i] != null) {
            	mDownloadDispatchers[i].quit();
            }
        }
    }
    
    /**
     * Gets a sequence number.
     */
    public int getDownloadId() {
        return mSequenceGenerator.incrementAndGet();
    }

    public void finish() {
    	
    }

}
