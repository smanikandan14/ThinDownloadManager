package com.thin.downloadmanager;

import android.os.Process;
import android.text.format.DateUtils;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

public class DownloadDispatcher extends Thread {

    /** The queue of download requests to service. */
    private final BlockingQueue<DownloadRequest> mQueue;

    /** Used to tell the dispatcher to die. */
    private volatile boolean mQuit = false;

    /** Current Download request that this dispatcher is working */
    private DownloadRequest mRequest;

    /** Connection & Socket timeout */
    private static final int DEFAULT_TIMEOUT = (int) (20 * DateUtils.SECOND_IN_MILLIS);

    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 4096;

    /** How many times redirects happened during a download request. */
    private int mRedirectionCount = 0;

    /** The maximum number of redirects. */
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.

    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;

    private long mContentLength;
    private static long mCurrentBytes;

    /** Tag used for debugging/logging */
    public static final String TAG = "ThinDownloadManager";

    /** Constructor take the dependency (DownloadRequest queue) that all the Dispatcher needs */
    public DownloadDispatcher(BlockingQueue<DownloadRequest> queue) {
        mQueue = queue;
    }
    
    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        
    	while(true) {
    		try {
                mRequest = mQueue.take();
                mRedirectionCount = 0;
                Log.v(TAG, "Download initiated for " + mRequest.getDownloadId());
                updateDownloadState(DownloadManager.STATUS_STARTED);
                executeDownload(mRequest.getUri().toString());
    		} catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
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
    boolean shouldContinue = true;
    private void executeDownload(String downloadUrl) {
        URL url = null;
        try {
            url = new URL(downloadUrl);
        } catch (MalformedURLException e) {
            updateDownloadFailed(DownloadManager.ERROR_MALFORMED_URI,"MalformedURLException: URI passed is malformed.");
            return;
        }

        HttpURLConnection conn = null;

        try {
            System.out.println("####### ExecuteDownload url ######## "+url.toString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(DEFAULT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);

            final int responseCode = conn.getResponseCode();
            Log.v(TAG, "Response code obtained for downloaded Id "+mRequest.getDownloadId()+" : httpResponse Code "+responseCode);
            switch (responseCode) {
                case HTTP_OK:
                    shouldContinue = false;
                    if (readResponseHeaders(conn) == 1) {
                        transferData(conn);
                    } else {

                        updateDownloadFailed(DownloadManager.ERROR_DOWNLOAD_SIZE_UNKNOWN, "Can't know size of download, giving up");
                    }
                    return;
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    // Take redirect url and call executeDownload recursively until
                    // MAX_REDIRECT is reached.
                    while (mRedirectionCount++ < MAX_REDIRECTS && shouldContinue) {
                        Log.v(TAG, "Redirect for downloaded Id "+mRequest.getDownloadId());
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
                    updateDownloadFailed(DownloadManager.ERROR_UNHANDLED_HTTP_CODE, "Unhandled HTTP response:" + responseCode +" message:" +conn.getResponseMessage());
                    break;
            }
        } catch(IOException e){
            updateDownloadFailed(DownloadManager.ERROR_HTTP_DATA_ERROR, "Trouble with low-level sockets");
        } finally{
            if (conn != null) {
                conn.disconnect();
            }
        }
	}
	
    private void transferData(HttpURLConnection conn) {
        InputStream in = null;
        OutputStream out = null;
        FileDescriptor outFd = null;
        try {
            try {
                in = conn.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

    		File destinationFile = new File(mRequest.getDestinationURI().getPath().toString());
    		
            try {
                out = new FileOutputStream(destinationFile, true);
                outFd = ((FileOutputStream) out).getFD();
            } catch (IOException e) {
            	e.printStackTrace();
                updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR, "Error in writing download contents to the destination file");
            }

            // Start streaming data
            transferData(in, out);

        } finally {
        	try {
        		in.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}

            try {
                if (out != null) out.flush();
                if (outFd != null) outFd.sync();
            } catch (IOException e) {
            } finally {
            	try {
            		out.close();
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
            }
        }
    }
    
    private void transferData(InputStream in, OutputStream out) {
        final byte data[] = new byte[BUFFER_SIZE];
        mCurrentBytes = 0;
        mRequest.setDownloadState(DownloadManager.STATUS_RUNNING);

        for (;;) {
            if (mRequest.isCanceled()) {
                Log.v(TAG, "Stopping the download as Download Request is cancelled for Downloaded Id "+mRequest.getDownloadId());
                return;
            }
            int bytesRead = readFromResponse( data, in);

            if (mContentLength != -1) {
                int progress = (int) ((mCurrentBytes * 100) / mContentLength);
                updateDownloadProgress(progress);
            }

            if (bytesRead == -1) { // success, end of stream already reached
                updateDownloadComplete();
                return;
            } else if (bytesRead == Integer.MIN_VALUE) {
                return;
            }

            writeDataToDestination(data, bytesRead, out);
            mCurrentBytes += bytesRead;
        }
    }

    private int readFromResponse( byte[] data, InputStream entityStream) {
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

    private void writeDataToDestination(byte[] data, int bytesRead, OutputStream out) {
        while (true) {
            try {
                out.write(data, 0, bytesRead);
                return;
            } catch (IOException ex) {
                updateDownloadFailed(DownloadManager.ERROR_FILE_ERROR, "IOException when writing download contents to the destination file");
            }
        }
    }

    private int readResponseHeaders( HttpURLConnection conn){
        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");

        if (transferEncoding == null) {
            mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            Log.v(TAG, "Ignoring Content-Length since Transfer-Encoding is also defined for Downloaded Id " + mRequest.getDownloadId());
            mContentLength = -1;
        }

        if( mContentLength == -1
                && (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked")) ) {
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

    public void updateDownloadState(int state) {
        mRequest.setDownloadState(state);
    }

    public void updateDownloadComplete() {
        mRequest.setDownloadState(DownloadManager.STATUS_SUCCESSFUL);
        if(mRequest.getDownloadListener() != null) {
            mRequest.getDownloadListener().onDownloadComplete(mRequest.getDownloadId());
            mRequest.finish();
        }
    }

    public void updateDownloadFailed(int errorCode, String errorMsg) {
        shouldContinue = false;
        mRequest.setDownloadState(DownloadManager.STATUS_FAILED);
        if(mRequest.getDownloadListener() != null) {
            mRequest.getDownloadListener().onDownloadFailed(
                    mRequest.getDownloadId(), errorCode, errorMsg);
            mRequest.finish();
        }
    }

    public void updateDownloadProgress(int progress) {
        if(mRequest.getDownloadListener() != null) {
            mRequest.getDownloadListener().onProgress(mRequest.getDownloadId(),
                    progress);
        }
    }
}
