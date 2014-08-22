package com.thin.downloadmanager;

import com.thin.downloadmanager.DownloadRequest;



import com.thin.downloadmanager.DownloadDispatcher;
import com.thin.downloadmanager.DownloadManager;
import com.thin.downloadmanager.DownloadRequest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRequestQueue {

	/** Specifies default number of download dispatcher threads. */
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


	/** The download dispatchers */
	private DownloadDispatcher[] mDownloadDispatchers;

	/** Used for generating monotonically-increasing sequence numbers for requests. */
    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    /**
     * Creates the download dispatchers workers pool.
     */
	public DownloadRequestQueue(int threadPoolSize) {
        if(threadPoolSize >0 && threadPoolSize <= 4) {
            mDownloadDispatchers = new DownloadDispatcher[threadPoolSize];
        } else {
            mDownloadDispatchers = new DownloadDispatcher[DEFAULT_DOWNLOAD_THREAD_POOL_SIZE];
        }
	}

    public void start() {
        stop();  // Make sure any currently running dispatchers are stopped.

        // Create download dispatchers (and corresponding threads) up to the pool size.
        for (int i = 0; i < mDownloadDispatchers.length; i++) {
            DownloadDispatcher networkDispatcher = new DownloadDispatcher(mDownloadQueue);
            mDownloadDispatchers[i] = networkDispatcher;
            networkDispatcher.start();
        }
    }


    // Package-Private methods.
    /**
     * Generates a download id for the request and adds the download request to the
     * download request queue for the dispatchers pool to act on immediately.
     * @param request
     * @param listener
     * @return downloadId
     */
	 int add(DownloadRequest request) {
		int downloadId = getDownloadId();
        // Tag the request as belonging to this queue and add it to the set of current requests.
        request.setDownloadRequestQueue(this);

        synchronized (mCurrentRequests) {
            mCurrentRequests.add(request);
        }

        // Process requests in the order they are added.
        request.setDownloadId(downloadId);		
        mDownloadQueue.add(request);
        
		return downloadId;		
	}

    /**
     * Returns the current download state for a download request.
     * @param downloadId
     * @return
     */
    int query(int downloadId) {
        synchronized (mCurrentRequests) {
            for(DownloadRequest request: mCurrentRequests) {
                if(request.getDownloadId() == downloadId) {
                    return request.getDownloadState();
                }
            }
        }

        return DownloadManager.STATUS_NOT_FOUND;
    }

    /**
     * Cancel all the dispatchers in work and also stops the dispatchers.
     */
    void cancelAll() {
        stop();
        //Remove from the queue.
        synchronized (mCurrentRequests) {
            mCurrentRequests.clear();
        }
    }

    /**
     * Cancel a particular download in progress.
     * Returns 1 if the download Id is found else returns 0.
     *
     * @param downloadId
     * @return int
     */
    int cancel(int downloadId) {
        synchronized (mCurrentRequests) {
            for(DownloadRequest request: mCurrentRequests) {
                if(request.getDownloadId() == downloadId) {
                    request.cancel();
                    return 1;
                }
            }
        }

        return 0;
    }

    void finish(DownloadRequest request) {
        //Remove from the queue.
        synchronized (mCurrentRequests) {
            mCurrentRequests.remove(request);
        }
    }

    /**
     * Cancels all the pending & running requests and releases all the dispatchers.
     */
    void release() {
        cancelAll();
        if(mDownloadDispatchers != null) {
            for (int i = 0; i < mDownloadDispatchers.length; i++) {
                mDownloadDispatchers[i] = null;
            }
            mDownloadDispatchers = null;
        }

    }
    // Private methods.

    /**
     * Stops download dispatchers.
     */
    private void stop() {
        for (int i = 0; i < mDownloadDispatchers.length; i++) {
            if (mDownloadDispatchers[i] != null) {
                mDownloadDispatchers[i].quit();
            }
        }
    }

    /**
     * Gets a sequence number.
     */
    private int getDownloadId() {
        return mSequenceGenerator.incrementAndGet();
    }
}
