package com.slim.downloadmanager;

import android.content.Context;
import android.os.Process;

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

    /** The queue of requests to service. */
    private final BlockingQueue<DownloadRequest> mQueue;

    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

	public static URL mUrl;
	private static Context mContext;
    private DownloadRequest mRequest;

    public DownloadDispatcher(BlockingQueue<DownloadRequest> queue) {
        mQueue = queue;
    }
    
    public void quit() {
    	mQuit = true;
    	interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        
    	while(true) {
    		try {
                mRequest = mQueue.take();
                updateDownloadStatus(DownloadManager.STATUS_STARTED, 0);
    			initiateDownload();
    		} catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
                continue;    			
    		}
    	}
    }

    public void updateDownloadStatus(int downloadStatus,int progress) {
        mRequest.getDownloadListener().updateDownloadStatus(mRequest.getDownloadId(),
                downloadStatus,progress);
    }

	public void initiateDownload() {
        /*PowerManager.WakeLock wakeLock = null;
        final PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        try {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NativeXDownloadManager");
            //wakeLock.setWorkSource(new WorkSource());
            wakeLock.acquire();
            mUrl = new URL(request.getUri().toString());
            executeDownload();
        } catch (Exception e) {
        	e.printStackTrace();
        }*/
        executeDownload();
	}
	
	private static int DEFAULT_TIMEOUT = 20000;
	
	private void executeDownload() {
        try {
            mUrl = new URL(mRequest.getUri().toString());
        } catch(MalformedURLException e) {
            updateDownloadStatus(DownloadManager.STATUS_FAILED, 0);
            return;
        }
        HttpURLConnection conn = null;
        try {
            //checkConnectivity();
            conn = (HttpURLConnection) mUrl.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(DEFAULT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);

            //addRequestHeaders(state, conn);

            final int responseCode = conn.getResponseCode();
            switch (responseCode) {
                case HTTP_OK:
                    //if (state.mContinuingDownload) {
                    //}
                	transferData(conn);
                	break;
                case HTTP_MOVED_PERM:
                case HTTP_MOVED_TEMP:
                case HTTP_SEE_OTHER:
/*                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    state.mUrl = new URL(state.mUrl, location);
                    if (responseCode == HTTP_MOVED_PERM) {
                        // Push updated URL back to database
                        state.mRequestUri = state.mUrl.toString();
                    }
                    continue;

                case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    throw new StopRequestException(
                            STATUS_CANNOT_RESUME, "Requested range not satisfiable"); */

                case HTTP_UNAVAILABLE:
//                    parseRetryAfterHeaders(state, conn);
//                    throw new StopRequestException(
//                            HTTP_UNAVAILABLE, conn.getResponseMessage());

                case HTTP_INTERNAL_ERROR:
//                    throw new StopRequestException(
//                            HTTP_INTERNAL_ERROR, conn.getResponseMessage());

                default:
//                    StopRequestException.throwUnhandledHttpError(
//                            responseCode, conn.getResponseMessage());
                    updateDownloadStatus(DownloadManager.STATUS_FAILED, 0);
                    mRequest.finish();
                    break;
            }
        } catch (IOException e) {
            // Trouble with low-level sockets
//            throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);

        } finally {
            if (conn != null) conn.disconnect();
        }

  //  throw new StopRequestException(STATUS_TOO_MANY_REDIRECTS, "Too many redirects");
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
                //throw new StopRequestException(STATUS_FILE_ERROR, e);
            	e.printStackTrace();
            }

            // Start streaming data, periodically watch for pause/cancel
            // commands and checking disk space as needed.
            transferData(in, out);

        } finally {

            //IoUtils.closeQuietly(in);
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
                //IoUtils.closeQuietly(out);
            	try {
            		out.close();
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
            }
        }
    }
    
    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 4096;
    static long mCurrentBytes; 
    
    private void transferData(InputStream in, OutputStream out) {
        final byte data[] = new byte[BUFFER_SIZE];
        mCurrentBytes = 0;
        for (;;) {
            int bytesRead = readFromResponse( data, in);
            updateDownloadStatus(DownloadManager.STATUS_RUNNING, (int)mCurrentBytes);
            System.out.println("######## Loop continues bytesRead #######  ");
            if (bytesRead == -1) { // success, end of stream already reached
                //handleEndOfStream(state);
            	System.out.println("######## end of stream #######  ");
                updateDownloadStatus(DownloadManager.STATUS_SUCCESSFUL, 0);
                return;
            }

            //state.mGotData = true;
            writeDataToDestination(data, bytesRead, out);
            mCurrentBytes += bytesRead;
            //reportProgress(state);

            System.out.println("######## downloaded " + mCurrentBytes + " for "
                      + mUrl);

            //checkPausedOrCanceled(state);
        }
    }

    private int readFromResponse( byte[] data, InputStream entityStream) {
        try {
            return entityStream.read(data);
        } catch (IOException ex) {
            // TODO: handle stream errors the same as other retries
            if ("unexpected end of stream".equals(ex.getMessage())) {
                return -1;
            }
            return -1;
        }
    }

    private void writeDataToDestination(byte[] data, int bytesRead, OutputStream out) {
    	
    	System.out.println("######### writeDataToDestination bytesRead ######## "+bytesRead);
        boolean forceVerified = false;
        int loop = 1;
        while (true) {
            try {
            	System.out.println("######### writing to Destination in Loop ######## "+loop);
            	loop++;
                out.write(data, 0, bytesRead);
                return;
            } catch (IOException ex) {
                // TODO: better differentiate between DRM and disk failures
                if (!forceVerified) {
                    // couldn't write to file. are we out of space? check.
                    //mStorageManager.verifySpace(mInfo.mDestination, state.mFilename, bytesRead);
                    forceVerified = true;
                } else {
                    //throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                           // "Failed to write data: " + ex);
                }
            }
        }
    }

    private void readResponseHeaders( HttpURLConnection conn){
        String mContentDisposition = conn.getHeaderField("Content-Disposition");
        String mContentLocation = conn.getHeaderField("Content-Location");
        //String mMimeType = Intent.normalizeMimeType(conn.getContentType());

        String mHeaderETag = conn.getHeaderField("ETag");

        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");
        long mContentLength;
        if (transferEncoding == null) {
            mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            System.out.println( "Ignoring Content-Length since Transfer-Encoding is also defined");
            mContentLength = -1;
        }

//        state.mTotalBytes = state.mContentLength;
//        mInfo.mTotalBytes = state.mContentLength;
//
//        final boolean noSizeInfo = state.mContentLength == -1
//                && (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked"));
//        if (!mInfo.mNoIntegrity && noSizeInfo) {
//            throw new StopRequestException(STATUS_CANNOT_RESUME,
//                    "can't know size of download, giving up");
//        }
    }

    public long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
