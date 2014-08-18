package com.slim.downloadmanager;

import android.net.Uri;

public class DownloadRequest implements Comparable<DownloadRequest> {

	private int mDownloadState;
	private int mDownloadId;
	private DownloadRequestQueue mRequestQueue;
	private DownloadStatusListener mDownloadListener;
	private Uri mUri;
	private Uri mDestinationURI;
	private boolean mRoamingAllowed = true;
    private int mAllowedNetworkTypes = ~0; // default to all network types allowed

    /**
     * Priority values.  Requests will be processed from higher priorities to
     * lower priorities, in FIFO order.
     */
    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }
    
    private Priority mPriority = Priority.NORMAL;
    
    /**
     * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return mPriority;
    }

    public void setPriority(Priority priority) {
    	mPriority = priority;
    }

     /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     */
    public void setDownloadRequestQueue(DownloadRequestQueue downloadQueue) {
    	mRequestQueue = downloadQueue;
    }

    /**
     * Sets the sequence number of this request.  Used by {@link RequestQueue}.
     */
    public final void setDownloadId(int downloadId) {
        mDownloadId = downloadId;
    }

    
    public int getmDownloadState() {
		return mDownloadState;
	}

	public void setmDownloadState(int mDownloadState) {
		this.mDownloadState = mDownloadState;
	}

	public DownloadStatusListener getmDownloadListener() {
		return mDownloadListener;
	}

	public void setmDownloadListener(DownloadStatusListener mDownloadListener) {
		this.mDownloadListener = mDownloadListener;
	}

	public Uri getUri() {
		return mUri;
	}

	public void setUri(Uri mUri) {
		this.mUri = mUri;
	}

	public Uri getDestinationURI() {
		return mDestinationURI;
	}

	public void setDestinationURI(Uri mDestinationURI) {
		this.mDestinationURI = mDestinationURI;
	}

	public boolean isRoamingAllowed() {
		return mRoamingAllowed;
	}

	public void setRoamingAllowed(boolean mRoamingAllowed) {
		this.mRoamingAllowed = mRoamingAllowed;
	}

	public int getAllowedNetworkTypes() {
		return mAllowedNetworkTypes;
	}

	public void setAllowedNetworkTypes(int mAllowedNetworkTypes) {
		this.mAllowedNetworkTypes = mAllowedNetworkTypes;
	}

	void finish() {
    	mRequestQueue.finish();
    }

	@Override
	public int compareTo(DownloadRequest other) {
        Priority left = this.getPriority();
        Priority right = other.getPriority();

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return left == right ?
                this.mDownloadId - other.mDownloadId :
                right.ordinal() - left.ordinal();		
	}
}
