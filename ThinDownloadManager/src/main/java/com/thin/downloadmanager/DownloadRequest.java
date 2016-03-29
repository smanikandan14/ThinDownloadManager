package com.thin.downloadmanager;

import android.content.Context;
import android.net.Uri;

import java.security.InvalidParameterException;
import java.util.HashMap;

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

    private boolean mWifiOnly = false;

    private DownloadRequestQueue mRequestQueue;

    private DownloadStatusListener mDownloadListener;

    private DownloadStatusListenerV1 mDownloadStatusListenerV1;

    private Context mContext;

    private Object mDownloadContext;

    private HashMap<String, String> mCustomHeader;
    private Priority mPriority = Priority.NORMAL;

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

    public boolean isStateActive() {
        boolean isActive;

        switch (mDownloadState) {
            case DownloadManager.STATUS_STARTED:
            case DownloadManager.STATUS_CONNECTING:
            case DownloadManager.STATUS_RETRYING:
            case DownloadManager.STATUS_RUNNING:
                isActive = true;
                break;
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_NOT_FOUND:
            case DownloadManager.STATUS_SUCCESSFUL:
            case DownloadManager.STATUS_FAILED:
                isActive = false;
                break;
            default:
                isActive = false;
        }

        return isActive;
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
     * Set if destination file should be deleted on download failure.
     * Use is optional: default is to delete.
     */
    public DownloadRequest setDeleteDestinationFileOnFailure(boolean deleteOnFailure) {
        this.mDeleteDestinationFileOnFailure = deleteOnFailure;
        return this;
    }

    public boolean isWifiOnly() {
        return mWifiOnly;
    }

    /**
     * Set if file should only be downloaded on WiFi. Use is optional: default is don't care.
     * @param wifiOnly Allows easily passing settings. Setting false is the same as not using this method
     * @param context Used to get network service.
     */
    public DownloadRequest setWifiOnly(boolean wifiOnly, Context context) throws InvalidParameterException {
        if(wifiOnly && context==null) throw new InvalidParameterException("Context cannot be null");
        mContext = context.getApplicationContext();
        mWifiOnly = wifiOnly;
        return this;
    }

    public Context getAppContext() {
        return mContext;
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
