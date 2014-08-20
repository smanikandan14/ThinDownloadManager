package com.slim.downloadmanager;

import android.os.Process;
import android.text.format.DateUtils;

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

    /** Current Download request that this dispatcher is working **/
    private DownloadRequest mRequest;

    private static final int HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_TEMP_REDIRECT = 307;

    /** Connection & Socket timeout **/
    private static final int DEFAULT_TIMEOUT = (int) (20 * DateUtils.SECOND_IN_MILLIS);

    /** The buffer size used to stream the data */
    public static final int BUFFER_SIZE = 4096;

    private int mRedirectionCount = 0;
    /**
     * The maximum number of redirects.
     */
    public static final int MAX_REDIRECTS = 5; // can't be more than 7.


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
                System.out.println("######## Request processed #######  "+mRequest.getDownloadId()+" : "+mRequest.getUri().toString());
                updateDownloadStatus(DownloadManager.STATUS_STARTED);
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

    private void executeDownload(String downloadUrl) {
        URL url = null;
        while (mRedirectionCount++ < MAX_REDIRECTS) {
            try {
                url = new URL(downloadUrl);
            } catch (MalformedURLException e) {
                updateDownloadStatus(DownloadManager.STATUS_FAILED);
                return;
            }
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);

                final int responseCode = conn.getResponseCode();
                System.out.println("######## Connection Response code ####### " + responseCode);
                switch (responseCode) {
                    case HTTP_OK:
                        System.out.println("######## HTTP_OK content location ####### ");
                        if (readResponseHeaders(conn) == 1) {
                            transferData(conn);
                        } else {
                            updateDownloadStatus(DownloadManager.STATUS_FAILED);
                        }
                        return;
                    case HTTP_MOVED_PERM:
                    case HTTP_MOVED_TEMP:
                    case HTTP_SEE_OTHER:
                    case HTTP_TEMP_REDIRECT:
                        final String location = conn.getHeaderField("Location");
                        downloadUrl = location;
                        continue;

                    case HTTP_REQUESTED_RANGE_NOT_SATISFIABLE:
                    case HTTP_UNAVAILABLE:
                    case HTTP_INTERNAL_ERROR:
                    default:
                        updateDownloadStatus(DownloadManager.STATUS_FAILED);
                        break;
                }
            } catch(IOException e){
                // Trouble with low-level sockets
                //throw new StopRequestException(STATUS_HTTP_DATA_ERROR, e);
            } finally{
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } // End of while

        updateDownloadStatus(DownloadManager.STATUS_FAILED);
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
                updateDownloadStatus(DownloadManager.STATUS_FAILED);
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
    
    static long mCurrentBytes;
    
    private void transferData(InputStream in, OutputStream out) {
        final byte data[] = new byte[BUFFER_SIZE];
        mCurrentBytes = 0;
        mRequest.setDownloadState(DownloadManager.STATUS_RUNNING);
        updateDownloadStatus(DownloadManager.STATUS_RUNNING);

        for (;;) {
            if (mRequest.isCanceled()) {
                System.out.println("######## Request is cancelled so stopping the download #######  ");
                return;
            }
            int bytesRead = readFromResponse( data, in);

            int progress = (int)((mCurrentBytes * 100)/mContentLength);
            updateDownloadProgress(progress);

            if (bytesRead == -1) { // success, end of stream already reached
                updateDownloadStatus(DownloadManager.STATUS_SUCCESSFUL);
                return;
            }

            writeDataToDestination(data, bytesRead, out);
            mCurrentBytes += bytesRead;

            //reportProgress(state);
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
        while (true) {
            try {
                out.write(data, 0, bytesRead);
                return;
            } catch (IOException ex) {
                //throw new StopRequestException(Downloads.Impl.STATUS_FILE_ERROR,
                       // "Failed to write data: " + ex);
                updateDownloadStatus(DownloadManager.STATUS_FAILED);
            }
        }
    }

    long mContentLength;
    private int readResponseHeaders( HttpURLConnection conn){
        final String transferEncoding = conn.getHeaderField("Transfer-Encoding");

        if (transferEncoding == null) {
            mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            System.out.println( "Ignoring Content-Length since Transfer-Encoding is also defined");
            mContentLength = -1;
        }

        System.out.println("######## Content-Length ######### "+mRequest.getDownloadId()+" : "+mContentLength);

        if( mContentLength == -1
                && (transferEncoding == null || !transferEncoding.equalsIgnoreCase("chunked")) ) {
            System.out.println( "Ignoring Content-Length since Transfer-Encoding is also defined");
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

    public void updateDownloadStatus(int downloadStatus) {
        mRequest.setDownloadState(downloadStatus);
        if(mRequest.getDownloadListener() != null) {
            mRequest.getDownloadListener().updateDownloadStatus(mRequest.getDownloadId(),
                    downloadStatus);
            if(downloadStatus == DownloadManager.STATUS_SUCCESSFUL ||
                    downloadStatus == DownloadManager.STATUS_FAILED ||
                    downloadStatus == DownloadManager.STATUS_NOT_FOUND) {
                mRequest.finish();
            }
        }
    }

    public void updateDownloadProgress(int progress) {
        if(mRequest.getDownloadListener() != null) {
            mRequest.getDownloadListener().updateDownloadProgress(mRequest.getDownloadId(),
                    progress);
        }
    }
}
