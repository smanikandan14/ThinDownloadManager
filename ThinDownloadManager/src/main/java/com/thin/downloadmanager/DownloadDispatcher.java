package com.thin.downloadmanager;

import android.os.Process;

import com.thin.downloadmanager.util.Log;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import static android.content.ContentValues.TAG;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * This thread/class used to make {@link HttpURLConnection}, receives Response from server
 * and Dispatch the response to respective {@link DownloadRequest}
 *
 * @author Mani Selvaraj
 * @author Praveen Kumar
 */
class DownloadDispatcher extends Thread {

    /**
     * The queue of download requests to service.
     */
    private final BlockingQueue<DownloadRequest> mQueue;
    /**
     * The buffer size used to stream the data
     */
    private final int BUFFER_SIZE = 4096;
    /**
     * The maximum number of redirects.
     */
    private final int MAX_REDIRECTS = 5; // can't be more than 7.
    private final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private final int HTTP_TEMP_REDIRECT = 307;
    /**
     * Used to tell the dispatcher to die.
     */
    private volatile boolean mQuit = false;
    /**
     * To Delivery call back response on main thread
     */
    private DownloadRequestQueue.CallBackDelivery mDelivery;
    /**
     * How many times redirects happened during a download request.
     */
    private int mRedirectionCount = 0;
    private long mContentLength;
    private boolean shouldAllowRedirects = true;

    /**
     * This variable is part of resumable download feature.
     * It will load the downloaded file cache length, If It had been already in available Downloaded Requested output path.
     * Otherwise it would keep "0" always by default.
     */
    private long mDownloadedCacheSize = 0;

    private Timer mTimer;

    /**
     * Constructor take the dependency (DownloadRequest queue) that all the Dispatcher needs
     */
    DownloadDispatcher(BlockingQueue<DownloadRequest> queue,
                       DownloadRequestQueue.CallBackDelivery delivery) {
        mQueue = queue;
        mDelivery = delivery;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTimer = new Timer();
        while (true) {
            DownloadRequest request = null;
            try {
                request = mQueue.take();
                mRedirectionCount = 0;
                shouldAllowRedirects = true;
                Log.v("Download initiated for " + request.getDownloadId());
                updateDownloadState(request, DownloadManager.STATUS_STARTED);
                executeDownload(request, request.getUri().toString());
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    if (request != null) {
                        request.finish();
                        // don't remove files that have been downloaded sucessfully.
                        if (request.getDownloadState() != DownloadManager.STATUS_SUCCESSFUL) {
                            updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                        }
                    }
                    mTimer.cancel();
                    return;
                }
            }
        }
    }

    void quit() {
        mQuit = true;
        interrupt();
    }


    private void executeDownload(DownloadRequest request, String downloadUrl) {
        URL url;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            updateDownloadFailed(request, DownloadManager.ERROR_MALFORMED_URI, "MalformedURLException: URI passed is malformed.");
            return;
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            File destinationFile = new File(request.getDestinationURI().getPath());
            if (destinationFile.exists()) {
                mDownloadedCacheSize = (int) destinationFile.length();
            }
            conn.setRequestProperty("Range", "bytes=" + mDownloadedCacheSize + "-");

            Log.d(TAG, "Existing file mDownloadedCacheSize: " + mDownloadedCacheSize);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(request.getRetryPolicy().getCurrentTimeout());
            conn.setReadTimeout(request.getRetryPolicy().getCurrentTimeout());

            HashMap<String, String> customHeaders = request.getCustomHeaders();
            if (customHeaders != null) {
                for (String headerName : customHeaders.keySet()) {
                    conn.addRequestProperty(headerName, customHeaders.get(headerName));
                }
            }

            // Status Connecting is set here before
            // urlConnection is trying to connect to destination.
            updateDownloadState(request, DownloadManager.STATUS_CONNECTING);

            final int responseCode = conn.getResponseCode();

            Log.v("Response code obtained for downloaded Id "
                    + request.getDownloadId()
                    + " : httpResponse Code "
                    + responseCode);

            switch (responseCode) {
                case HTTP_PARTIAL:
                case HTTP_OK:
                    shouldAllowRedirects = false;
                    if (readResponseHeaders(request, conn, responseCode) == 1) {
                        Log.d(TAG, "Existing mDownloadedCacheSize: " + mDownloadedCacheSize);
                        Log.d(TAG, "File mContentLength: " + mContentLength);
                        if (mDownloadedCacheSize == mContentLength) { // Mark as success, If end of stream already reached
                            updateDownloadComplete(request);
                            Log.d(TAG, "Download Completed");
                        } else {
                            transferData(request, conn);
                        }
                    } else {
                        updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_SIZE_UNKNOWN, "Transfer-Encoding not found as well as can't know size of download, giving up");
                    }
                    return;
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    // Take redirect url and call executeDownload recursively until
                    // MAX_REDIRECT is reached.
                    while (mRedirectionCount < MAX_REDIRECTS && shouldAllowRedirects) {
                        mRedirectionCount++;
                        Log.v(TAG, "Redirect for downloaded Id " + request.getDownloadId());
                        final String location = conn.getHeaderField("Location");
                        executeDownload(request, location);
                    }

                    if (mRedirectionCount > MAX_REDIRECTS && shouldAllowRedirects) {
                        updateDownloadFailed(request, DownloadManager.ERROR_TOO_MANY_REDIRECTS, "Too many redirects, giving up");
                        return;
                    }
                    break;
                case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    updateDownloadFailed(request, HTTP_REQUESTED_RANGE_NOT_SATISFIABLE, conn.getResponseMessage());
                    break;
                case HTTP_UNAVAILABLE:
                    updateDownloadFailed(request, HTTP_UNAVAILABLE, conn.getResponseMessage());
                    break;
                case HTTP_INTERNAL_ERROR:
                    updateDownloadFailed(request, HTTP_INTERNAL_ERROR, conn.getResponseMessage());
                    break;
                default:
                    updateDownloadFailed(request, DownloadManager.ERROR_UNHANDLED_HTTP_CODE, "Unhandled HTTP response:" + responseCode + " message:" + conn.getResponseMessage());
                    break;
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            // Retry.
            attemptRetryOnTimeOutException(request);
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
            attemptRetryOnTimeOutException(request);
        } catch (IOException e) {
            e.printStackTrace();
            updateDownloadFailed(request, DownloadManager.ERROR_HTTP_DATA_ERROR, "Trouble with low-level sockets");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void transferData(DownloadRequest request, HttpURLConnection conn) {
        BufferedInputStream in = null;
        RandomAccessFile accessFile = null;
        cleanupDestination(request, false);
        try {
            try {
                in = new BufferedInputStream(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            File destinationFile = new File(request.getDestinationURI().getPath());

            boolean errorCreatingDestinationFile = false;
            // Create destination file if it doesn't exists
            if (!destinationFile.exists()) {
                try {
                    // Check path
                    File parentPath = destinationFile.getParentFile();
                    if (parentPath != null && !parentPath.exists()) {
                        parentPath.mkdirs();
                    }
                    if (!destinationFile.createNewFile()) {
                        errorCreatingDestinationFile = true;
                        updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR,
                                "Error in creating destination file");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    errorCreatingDestinationFile = true;
                    updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR,
                            "Error in creating destination file");
                }
            } else {
                if (in != null) {
                    request.abortCancel();
                }
            }

            // If Destination file couldn't be created. Abort the data transfer.
            if (!errorCreatingDestinationFile) {
                try {
                    accessFile = new RandomAccessFile(destinationFile, "rw");
                    accessFile.seek(mDownloadedCacheSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (in == null) {
                    updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR,
                            "Error in creating input stream");
                } else if (accessFile == null) {

                    updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR,
                            "Error in writing download contents to the destination file");
                } else {
                    // Start streaming data
                    transferData(request, in, accessFile);
                }
            }

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (accessFile != null) {
                    accessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (accessFile != null) accessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void transferData(DownloadRequest request, InputStream in, RandomAccessFile out) {
        final byte data[] = new byte[BUFFER_SIZE];
        long mCurrentBytes = mDownloadedCacheSize;
        request.setDownloadState(DownloadManager.STATUS_RUNNING);
        Log.v("Content Length: " + mContentLength + " for Download Id " + request.getDownloadId());
        for (; ; ) {
            if (request.isCancelled()) {
                Log.v("Stopping the download as Download Request is cancelled for Downloaded Id " + request.getDownloadId());
                request.finish();
                updateDownloadFailed(request, DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                return;
            }
            int bytesRead = readFromResponse(request, data, in);

            if (mContentLength != -1 && mContentLength > 0) {
                int progress = (int) ((mCurrentBytes * 100) / mContentLength);
                updateDownloadProgress(request, progress, mCurrentBytes);
            }

            if (bytesRead == -1) { // success, end of stream already reached
                updateDownloadComplete(request);
                return;
            } else if (bytesRead == Integer.MIN_VALUE) {
                return;
            }

            if (writeDataToDestination(request, data, bytesRead, out)) {
                mCurrentBytes += bytesRead;
            } else {
                request.finish();
                updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR, "Failed writing file");
                return;
            }
        }
    }

    private int readFromResponse(DownloadRequest request, byte[] data, InputStream entityStream) {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }
            updateDownloadFailed(request, DownloadManager.ERROR_HTTP_DATA_ERROR, "IOException: Failed reading response");
            return Integer.MIN_VALUE;
        }
    }

    private boolean writeDataToDestination(DownloadRequest request, byte[] data, int bytesRead, RandomAccessFile out) {
        boolean successInWritingToDestination = true;
        try {
            out.write(data, 0, bytesRead);
        } catch (IOException ex) {
            updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR, "IOException when writing download contents to the destination file");
            successInWritingToDestination = false;
        } catch (Exception e) {
            updateDownloadFailed(request, DownloadManager.ERROR_FILE_ERROR, "Exception when writing download contents to the destination file");
            successInWritingToDestination = false;
        }

        return successInWritingToDestination;
    }

    private int readResponseHeaders(DownloadRequest request, HttpURLConnection conn, int responseCode) {
        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        mContentLength = -1;

        if (transferEncoding == null) {
            if (responseCode == HTTP_OK) {
                // If file download already completed, 200 HttpStatusCode will thrown by service.
                mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
            } else {
                // If file download already partially completed, 206 HttpStatusCode will thrown by service and we can resume remaining chunks downloads.
                mContentLength = getHeaderFieldLong(conn, "Content-Length", -1) + mDownloadedCacheSize;
            }
        } else {
            Log.v("Ignoring Content-Length since Transfer-Encoding is also defined for Downloaded Id " + request.getDownloadId());
        }

        if (mContentLength != -1) {
            return 1;
        } else if (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked")) {
            return -1;
        } else {
            return 1;
        }
    }

    private long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void attemptRetryOnTimeOutException(final DownloadRequest request) {
        updateDownloadState(request, DownloadManager.STATUS_RETRYING);
        final RetryPolicy retryPolicy = request.getRetryPolicy();
        try {
            retryPolicy.retry();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    executeDownload(request, request.getUri().toString());
                }
            }, retryPolicy.getCurrentTimeout());
        } catch (RetryError e) {
            // Update download failed.
            updateDownloadFailed(request, DownloadManager.ERROR_CONNECTION_TIMEOUT_AFTER_RETRIES,
                    "Connection time out after maximum retires attempted");
        }
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the downloaded file with mDownloadedCacheSize file.
     *
     * @param forceClean -  It will delete downloaded cache, Even streaming is enabled, If user intentionally cancelled.
     */
    private void cleanupDestination(DownloadRequest request, boolean forceClean) {
        if (!request.isResumable() || forceClean) {
            Log.d("cleanupDestination() deleting " + request.getDestinationURI().getPath());
            File destinationFile = new File(request.getDestinationURI().getPath());
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
        }
    }

    private void updateDownloadState(DownloadRequest request, int state) {
        request.setDownloadState(state);
    }

    private void updateDownloadComplete(DownloadRequest request) {
        mDownloadedCacheSize = 0; // reset into Zero.
        mDelivery.postDownloadComplete(request);
        request.setDownloadState(DownloadManager.STATUS_SUCCESSFUL);
        request.finish();
    }

    private void updateDownloadFailed(DownloadRequest request, int errorCode, String errorMsg) {
        mDownloadedCacheSize = 0; // reset into Zero.
        shouldAllowRedirects = false;
        request.setDownloadState(DownloadManager.STATUS_FAILED);
        if (request.getDeleteDestinationFileOnFailure()) {
            cleanupDestination(request, true);
        }
        mDelivery.postDownloadFailed(request, errorCode, errorMsg);
        request.finish();
    }

    private void updateDownloadProgress(DownloadRequest request, int progress, long downloadedBytes) {
        mDelivery.postProgressUpdate(request, mContentLength, downloadedBytes, progress);
    }
}
