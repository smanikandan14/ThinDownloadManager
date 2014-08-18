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

    public DownloadRequest(Uri uri) {
        if ( uri == null) {
            throw new NullPointerException();
        }

        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("http") == false ) {
            throw new IllegalArgumentException("Can download only http URIs: "+uri);
        }

        mUri = uri;
    }

    /**
     * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return mPriority;
    }

    public DownloadRequest setPriority(Priority priority) {
    	mPriority = priority;
        return this;
    }

     /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     */
    void setDownloadRequestQueue(DownloadRequestQueue downloadQueue) {
    	mRequestQueue = downloadQueue;
    }

    /**
     * Sets the sequence number of this request.  Used by {@link RequestQueue}.
     */
    final void setDownloadId(int downloadId) {
        mDownloadId = downloadId;
    }

    final int getDownloadId() {
        return mDownloadId;
    }
    
    int getDownloadState() {
		return mDownloadState;
	}

	void setDownloadState(int mDownloadState) {
		this.mDownloadState = mDownloadState;
	}

	DownloadStatusListener getDownloadListener() {
		return mDownloadListener;
	}

	DownloadRequest setDownloadListener(DownloadStatusListener mDownloadListener) {
		this.mDownloadListener = mDownloadListener;
        return this;
	}

	public Uri getUri() {
		return mUri;
	}

	public DownloadRequest setUri(Uri mUri) {
		this.mUri = mUri;
        return this;
	}

	public Uri getDestinationURI() {
		return mDestinationURI;
	}

	public DownloadRequest setDestinationURI(Uri mDestinationURI) {
		this.mDestinationURI = mDestinationURI;
        return this;
	}

	public boolean isRoamingAllowed() {
		return mRoamingAllowed;
	}

	public DownloadRequest setRoamingAllowed(boolean mRoamingAllowed) {
		this.mRoamingAllowed = mRoamingAllowed;
        return this;
	}

	public int getAllowedNetworkTypes() {
		return mAllowedNetworkTypes;
	}

	public DownloadRequest setAllowedNetworkTypes(int mAllowedNetworkTypes) {
		this.mAllowedNetworkTypes = mAllowedNetworkTypes;
        return this;
	}

    //Package-private methods.

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
