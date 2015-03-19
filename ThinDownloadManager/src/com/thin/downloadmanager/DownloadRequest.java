package com.thin.downloadmanager;

import android.net.Uri;
import android.text.format.DateUtils;

public class DownloadRequest implements Comparable<DownloadRequest> {

    /** If download fails, then how many retry attempts should be made **/
    private final int DEFAULT_RETRY_ATTEMPTS = 2;

    private final long DEFAULT_RETRY_WAIT =  (5 * DateUtils.SECOND_IN_MILLIS);


    /** Tells the current download state of this request */
	private int mDownloadState;

    /** Download Id assigned to this request */
    private int mDownloadId;

    /** The URI resource that this request is to download */
    private Uri mUri;

    /** The destination path on the device where the downloaded files needs to be put
     * It can be either External Directory ( SDcard ) or
     * internal app cache or files directory.
     * For using external SDCard access, application should have
     * this permission android.permission.WRITE_EXTERNAL_STORAGE declared.
     */
    private Uri mDestinationURI;

    /** Whether or not this request has been canceled. */
    private boolean mCanceled = false;

    private DownloadRequestQueue mRequestQueue;

    private DownloadStatusListener mDownloadListener;

    private int mRetryAttempts;
    private long mRetryWaitMilli;

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
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
        }
        mDownloadState = DownloadManager.STATUS_PENDING;
        mUri = uri;
        mRetryAttempts = DEFAULT_RETRY_ATTEMPTS;
        mRetryWaitMilli = DEFAULT_RETRY_WAIT;
    }

    /**
     * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return mPriority;
    }

    /**
     * Set the {@link Priority}  of this request;
     * @param priority
     * @return request
     */
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
     * Sets the download Id of this request.  Used by {@link DownloadRequestQueue}.
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

	public DownloadRequest setDownloadListener(DownloadStatusListener downloadListener) {
		this.mDownloadListener = downloadListener;
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

	public DownloadRequest setDestinationURI(Uri destinationURI) {
		this.mDestinationURI = destinationURI;
        return this;
	}

    /**
     *
     * @param retry number of retry attempts
     * @return
     */
    public DownloadRequest setRetryAttempts(int retry){
        this.mRetryAttempts = retry;
        return this;
    }

    public int getRetryAttempts(){
        return this.mRetryAttempts;
    }
    /**
     *
     * @param retry number of retry attempts
     * @param waitTime duration of time to wait between retry attempts
     * @return
     */
    public DownloadRequest setRetryAttempts(int retry, long waitTime){
        this.mRetryAttempts = retry;
        return this;
    }

    /**
     *
     * @param milli duration to wait between retry attempts
     * @return
     */
    public DownloadRequest setRetryWaitInterval(long milli){
        this.mRetryWaitMilli = milli;
        return this;
    }

    public long getRetryWaitInterval(){
        return this.mRetryWaitMilli;
    }


    //Package-private methods.

    /**
     * Mark this request as canceled.  No callback will be delivered.
     */
    public void cancel() {
        mCanceled = true;
    }

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isCanceled() {
        return mCanceled;
    }

    void finish() {
    	mRequestQueue.finish(this);
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
