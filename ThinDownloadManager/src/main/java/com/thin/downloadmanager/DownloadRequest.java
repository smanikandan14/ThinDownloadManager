package com.thin.downloadmanager;

import android.net.Uri;

import java.util.HashMap;

/**
 * This class used to handle user requests and provides priorities to the request even there may be 'n' number of request raised.
 * Basically this is {@link Comparable} class and It compares the {@link DownloadRequest}'s {@link Priority} levels and react accordingly.
 *
 * @author Mani Selvaraj
 * @author Praveen Kumar
 */
public class DownloadRequest implements Comparable<DownloadRequest> {


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

    /**
     * Tells the current download state of this request
     */
    private int mDownloadState;

    /**
     * Download Id assigned to this request
     */
    private int mDownloadId;

    /**
     * The URI resource that this request is to download
     */
    private Uri mUri;

    /**
     * The destination path on the device where the downloaded files needs to be put
     * It can be either External Directory ( SDcard ) or
     * internal app cache or files directory.
     * For using external SDCard access, application should have
     * this permission android.permission.WRITE_EXTERNAL_STORAGE declared.
     */
    private Uri mDestinationURI;

    private RetryPolicy mRetryPolicy;

    /**
     * Whether or not this request has been canceled.
     */
    private boolean mCancelled = false;

    private boolean mDeleteDestinationFileOnFailure = true;

    private DownloadRequestQueue mRequestQueue;

    private DownloadStatusListener mDownloadListener;

    private DownloadStatusListenerV1 mDownloadStatusListenerV1;

    private Object mDownloadContext;

    private HashMap<String, String> mCustomHeader;
    private Priority mPriority = Priority.NORMAL;

    private boolean isDownloadResumable = false;

    public DownloadRequest(Uri uri) {
        if (uri == null) {
            throw new NullPointerException();
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new IllegalArgumentException("Can only download HTTP/HTTPS URIs: " + uri);
        }
        mCustomHeader = new HashMap<>();
        mDownloadState = DownloadManager.STATUS_PENDING;
        mUri = uri;
    }

    /**
     * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
     */
    public Priority getPriority() {
        return mPriority;
    }

    /**
     * Set the {@link Priority}  of this request;
     *
     * @param priority
     * @return request
     */
    public DownloadRequest setPriority(Priority priority) {
        mPriority = priority;
        return this;
    }

    /**
     * Adds custom header to request
     *
     * @param key
     * @param value
     */
    public DownloadRequest addCustomHeader(String key, String value) {
        mCustomHeader.put(key, value);
        return this;
    }

    /**
     * Associates this request with the given queue. The request queue will be notified when this
     * request has finished.
     */
    void setDownloadRequestQueue(DownloadRequestQueue downloadQueue) {
        mRequestQueue = downloadQueue;
    }

    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy == null ? new DefaultRetryPolicy() : mRetryPolicy;
    }

    public DownloadRequest setRetryPolicy(RetryPolicy mRetryPolicy) {
        this.mRetryPolicy = mRetryPolicy;
        return this;
    }

    /**
     * Gets the download id.
     *
     * @return the download id
     */
    public final int getDownloadId() {
        return mDownloadId;
    }

    /**
     * Sets the download Id of this request.  Used by {@link DownloadRequestQueue}.
     */
    final void setDownloadId(int downloadId) {
        mDownloadId = downloadId;
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

    /**
     * Sets the download listener for this download request. Use setStatusListener instead.
     *
     * @deprecated use {@link #setStatusListener} instead.
     */
    @Deprecated
    public DownloadRequest setDownloadListener(DownloadStatusListener downloadListener) {
        this.mDownloadListener = downloadListener;
        return this;
    }

    /**
     * Gets the status listener. For internal use.
     *
     * @return  the status listener
     */
    DownloadStatusListenerV1 getStatusListener() {
        return mDownloadStatusListenerV1;
    }

    /**
     * Sets the status listener for this download request. Download manager sends progress,
     * failure and completion updates to this listener for this download request.
     *
     * @param downloadStatusListenerV1 the status listener for this download
     */
    public DownloadRequest setStatusListener(DownloadStatusListenerV1 downloadStatusListenerV1) {
        mDownloadStatusListenerV1 = downloadStatusListenerV1;
        return this;
    }

    public Object getDownloadContext() {
        return mDownloadContext;
    }

    public DownloadRequest setDownloadContext(Object downloadContext) {
        mDownloadContext = downloadContext;
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

    public boolean getDeleteDestinationFileOnFailure() {
        return mDeleteDestinationFileOnFailure;
    }

    /**
     * It marks the request with resumable feature and It is an optional feature
     * @param isDownloadResumable - It enables resumable feature for this request
     * @return - current {@link DownloadRequest}
     */
    public DownloadRequest setDownloadResumable(boolean isDownloadResumable) {
        this.isDownloadResumable = isDownloadResumable;
        setDeleteDestinationFileOnFailure(false); // If resumable feature enabled, downloaded file should not be deleted.
        return this;
    }

    public boolean isResumable() {
        return isDownloadResumable;
    }
    /**
     * Set if destination file should be deleted on download failure.
     * Use is optional: default is to delete.
     */
    public DownloadRequest setDeleteDestinationFileOnFailure(boolean deleteOnFailure) {
        this.mDeleteDestinationFileOnFailure = deleteOnFailure;
        return this;
    }

    /**
     * Mark this request as canceled.  No callback will be delivered.
     */
    public void cancel() {
        mCancelled = true;
    }

    //Package-private methods.

    /**
     * Returns true if this request has been canceled.
     */
    public boolean isCancelled() {
        return mCancelled;
    }



    /**
     * Marked the request as canceled is aborted.
     */
    public void abortCancel() {
        mCancelled = false;
    }

    /**
     * Returns all custom headers set by user
     *
     * @return
     */
    HashMap<String, String> getCustomHeaders() {
        return mCustomHeader;
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
