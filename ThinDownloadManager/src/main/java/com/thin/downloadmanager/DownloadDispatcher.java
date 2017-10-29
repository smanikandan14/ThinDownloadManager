package com.thin.downloadmanager;

import android.os.Process;
import android.util.Log;

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

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * @author Mani Selvaraju
 * @author Praveen Kumar
 */
public class DownloadDispatcher extends Thread {

    /**
     * Tag used for debugging/logging
     */
    public static final String TAG = "ThinDownloadManager";
    /**
     * The buffer size used to stream the data
     */
    public final int BUFFER_SIZE = 4096;
    /**
     * The maximum number of redirects.
     */
    public final int MAX_REDIRECTS = 5; // can't be more than 7.
    /**
     * The queue of download requests to service.
     */
    private final BlockingQueue<DownloadRequest> mQueue;
    private final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private final int HTTP_TEMP_REDIRECT = 307;
    boolean shouldAllowRedirects = true;
    Timer mTimer;
    /**
     * Used to tell the dispatcher to die.
     */
    private volatile boolean mQuit = false;
    /**
     * Current Download request that this dispatcher is working
     */
    private DownloadRequest mRequest;
    /**
     * To Delivery call back response on main thread
     */
    private DownloadRequestQueue.CallBackDelivery mDelivery;
    /**
     * How many times redirects happened during a download request.
     */
    private int mRedirectionCount = 0;
    private long mContentLength;
    private long mCurrentBytes;
    private long mDownloadedCacheSize = 0;

    /**
     * Constructor take the dependency (DownloadRequest queue) that all the Dispatcher needs
     */
    public DownloadDispatcher(BlockingQueue<DownloadRequest> queue,
                              DownloadRequestQueue.CallBackDelivery delivery) {
        mQueue = queue;
        mDelivery = delivery;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTimer = new Timer();
        while (true) {
            try {
                mRequest = mQueue.take();
                mRedirectionCount = 0;
                Log.v(TAG, "Download initiated for " + mRequest.getDownloadId());
                updateDownloadState(DownloadManager.STATUS_STARTED);
                executeDownload(mRequest.getUri().toString());
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    if (mRequest != null) {
                        mRequest.finish();
                        updateDownloadFailed(DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                        mTimer.cancel();
                    }
                    return;
                }
                continue;
            }
        }
    }

    public void quit() {
        mQuit = true;
        interrupt();
    }


    private void executeDownload(String downloadUrl) {
        URL url;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            updateDownloadFailed(DownloadManager.ERROR_MALFORMED_URI, "MalformedURLException: URI passed is malformed.");
            return;
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            File destinationFile = new File(mRequest.getDestinationURI().getPath());
            if (destinationFile.exists()) {
                mDownloadedCacheSize = (int) destinationFile.length();
            }
            conn.setRequestProperty("Range", "bytes=" + mDownloadedCacheSize + "-");

            Log.d(TAG, "Existing file mDownloadedCacheSize: " + mDownloadedCacheSize);
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(mRequest.getRetryPolicy().getCurrentTimeout());
            conn.setReadTimeout(mRequest.getRetryPolicy().getCurrentTimeout());

            HashMap<String, String> customHeaders = mRequest.getCustomHeaders();
            if (customHeaders != null) {
                for (String headerName : customHeaders.keySet()) {
                    conn.addRequestProperty(headerName, customHeaders.get(headerName));
                }
            }

            // Status Connecting is set here before
            // urlConnection is trying to connect to destination.
            updateDownloadState(DownloadManager.STATUS_CONNECTING);

            final int responseCode = conn.getResponseCode();

            Log.v(TAG, "Response code obtained for mDownloadedCacheSize Id "
                    + mRequest.getDownloadId()
                    + " : httpResponse Code "
                    + responseCode);

            switch (responseCode) {
                case HTTP_OK:
                case HTTP_PARTIAL:
                    shouldAllowRedirects = false;
                    if (readResponseHeaders(conn, responseCode) == 1) {
                        Log.d(TAG, "Existing mDownloadedCacheSize: " + mDownloadedCacheSize);
                        Log.d(TAG, "File mContentLength: " + mContentLength);
                        if (mDownloadedCacheSize == mContentLength) { // Mark as success, If end of stream already reached
                            updateDownloadComplete();
                            Log.d(TAG, "Download Completed");
                        } else {
                            transferData(conn);
                        }
                    } else {
                        updateDownloadFailed(DownloadManager.ERROR_DOWNLOAD_SIZE_UNKNOWN, "Transfer-Encoding not found as well as can't know size of download, giving up");
                    }
                    return;
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    // Take redirect url and call executeDownload recursively until
                    // MAX_REDIRECT is reached.
                    while (mRedirectionCount++ < MAX_REDIRECTS && shouldAllowRedirects) {
                        Log.v(TAG, "Redirect for mDownloadedCacheSize Id " + mRequest.getDownloadId());
                        final String location = conn.getHeaderField("Location");
                        executeDownload(location);
                        continue;
                    }

                    if (mRedirectionCount > MAX_REDIRECTS) {
                        updateDownloadFailed(DownloadManager.ERROR_TOO_MANY_REDIRECTS, "Too many redirects, giving up");
                        return;
                    }
                    break;
                case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    updateDownloadFailed(HTTP_REQUESTED_RANGE_NOT_SATISFIABLE, conn.getResponseMessage());
                    break;
                case HTTP_UNAVAILABLE:
                    updateDownloadFailed(HTTP_UNAVAILABLE, conn.getResponseMessage());
                    break;
                case HTTP_INTERNAL_ERROR:
                    updateDownloadFailed(HTTP_INTERNAL_ERROR, conn.getResponseMessage());
                    break;
                default:
                    updateDownloadFailed(DownloadManager.ERROR_UNHANDLED_HTTP_CODE, "Unhandled HTTP response:" + responseCode + " message:" + conn.getResponseMessage());
                    break;
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            // Retry.
            attemptRetryOnTimeOutException();
        } catch (ConnectTimeoutException e) {
            e.printStackTrace();
            attemptRetryOnTimeOutException();
        } catch (IOException e) {
            e.printStackTrace();
            updateDownloadFailed(DownloadManager.ERROR_HTTP_DATA_ERROR, "Trouble with low-level sockets");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void transferData(HttpURLConnection conn) {
        BufferedInputStream in = null;
        RandomAccessFile accessFile = null;
        cleanupDestination(false);
        try {
            try {
                in = new BufferedInputStream(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            File destinationFile = new File(mRequest.getDestinationURI().getPath());

            boolean errorOccurred = false;
            // Create destination file if it doesn't exists
            if (!destinationFile.exists()) {
                try {
                    destinationFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    errorOccurred = true;
                    updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR,
                            "Error in creating destination file");
                }
            } else {
                if (in != null) {
                    mRequest.abortCancel();
                }
            }

            try {
                accessFile = new RandomAccessFile(destinationFile, "rw");
                accessFile.seek(mDownloadedCacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (in == null && !errorOccurred) {
                updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR, "Error in creating input stream");

            } else if (accessFile == null && !errorOccurred) {

                updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR, "Error in writing download contents to the destination file");

            } else {
                // Start streaming data
                transferData(in, accessFile);
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

    private void transferData(InputStream in, RandomAccessFile out) {
        final byte data[] = new byte[BUFFER_SIZE];
        mCurrentBytes = mDownloadedCacheSize;
        mRequest.setDownloadState(DownloadManager.STATUS_RUNNING);
        Log.v(TAG, "Content Length: " + mContentLength + " for Download Id " + mRequest.getDownloadId());
        for (; ; ) {
            if (mRequest.isCanceled()) {
                Log.v(TAG, "Stopping the download as Download Request is cancelled for Downloaded Id " + mRequest.getDownloadId());
                mRequest.finish();
                updateDownloadFailed(DownloadManager.ERROR_DOWNLOAD_CANCELLED, "Download cancelled");
                return;
            }
            int bytesRead = readFromResponse(data, in);

            if (mContentLength != -1 && mContentLength > 0) {

                int progress = (int) ((mCurrentBytes * 100) / mContentLength);
                updateDownloadProgress(progress, mCurrentBytes);
            }

            if (bytesRead == -1) { // success, end of stream already reached
                updateDownloadComplete();
                Log.e("######", "Download Completed");
                return;
            } else if (bytesRead == Integer.MIN_VALUE) {
                return;
            }

            writeDataToDestination(data, bytesRead, out);
            mCurrentBytes += bytesRead;
        }
    }

    private int readFromResponse(byte[] data, InputStream entityStream) {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }
            updateDownloadFailed(DownloadManager.ERROR_HTTP_DATA_ERROR, "IOException: Failed reading response");
            return Integer.MIN_VALUE;
        }
    }

    private void writeDataToDestination(byte[] data, int bytesRead, RandomAccessFile out) {
        while (true) {
            try {
                out.write(data, 0, bytesRead);
//                mDownloadedCacheSize += bytesRead;
                return;
            } catch (IOException ex) {
                updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR, "IOException when writing download contents to the destination file");
            }
        }
    }

    private int readResponseHeaders(HttpURLConnection conn, int responseCode) {
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
            Log.v(TAG, "Ignoring Content-Length since Transfer-Encoding is also defined for Downloaded Id " + mRequest.getDownloadId());
        }

        if (mContentLength != -1) {
            return 1;
        } else if (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked")) {
            return -1;
        } else {
            return 1;
        }
    }

    public long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void attemptRetryOnTimeOutException() {
        updateDownloadState(DownloadManager.STATUS_RETRYING);
        final RetryPolicy retryPolicy = mRequest.getRetryPolicy();
        try {
            retryPolicy.retry();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    executeDownload(mRequest.getUri().toString());
                }
            }, retryPolicy.getCurrentTimeout());
        } catch (RetryError e) {
            // Update download failed.
            updateDownloadFailed(DownloadManager.ERROR_CONNECTION_TIMEOUT_AFTER_RETRIES,
                    "Connection time out after maximum retires attempted");
        }
    }

    /**
     * Called just before the thread finishes, regardless of status, to take any necessary action on
     * the mDownloadedCacheSize file.
     *
     * @param forceClean -  It will delete downloaded cache, Even streaming is enabled, If user intentionally cancelled.
     */
    public void cleanupDestination(boolean forceClean) {
        if (!mRequest.isStreamingDownloadEnabled() || forceClean) {
            Log.d(TAG, "cleanupDestination() deleting " + mRequest.getDestinationURI().getPath());
            File destinationFile = new File(mRequest.getDestinationURI().getPath());
            if (destinationFile.exists()) {
                deleteRecursive(destinationFile);
            }
        }
    }

    /**
     * For a directory, recursively deletes the contents inside the incoming directory and
     * deletes it afterwards.
     *
     * @param fileOrDirectory
     */
    private void deleteRecursive(File fileOrDirectory) {
        if (null == fileOrDirectory)
            return;

        // Recursively delete files inside directory
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        Log.d(TAG, "Deleting: " + fileOrDirectory);

        boolean isDeleted = fileOrDirectory.delete();
        ;
        Log.e("######", "" + fileOrDirectory + " " + (isDeleted ? "DELETED" : "NOT DELETED"));
    }

    public void updateDownloadState(int state) {
        mRequest.setDownloadState(state);
    }

    public void updateDownloadComplete() {
        mDownloadedCacheSize = 0; // reset into Zero.
        mDelivery.postDownloadComplete(mRequest);
        mRequest.setDownloadState(DownloadManager.STATUS_SUCCESSFUL);
        mRequest.finish();
    }

    public void updateDownloadFailed(int errorCode, String errorMsg) {
        mDownloadedCacheSize = 0; // reset into Zero.
        shouldAllowRedirects = false;
        mRequest.setDownloadState(DownloadManager.STATUS_FAILED);
        boolean isUserCancelled = errorCode == DownloadManager.ERROR_DOWNLOAD_CANCELLED;
        cleanupDestination(isUserCancelled); // User cancelled intentionally.
        mDelivery.postDownloadFailed(mRequest, errorCode, errorMsg);
        mRequest.finish();
    }

    public void updateDownloadProgress(int progress, long downloadedBytes) {
        mDelivery.postProgressUpdate(mRequest, mContentLength, downloadedBytes, progress);
    }
}
