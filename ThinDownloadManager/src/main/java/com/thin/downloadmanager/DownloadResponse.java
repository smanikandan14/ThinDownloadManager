package com.thin.downloadmanager;

import com.thin.downloadmanager.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author locke.peng on 1/19/18.
 */

public class DownloadResponse {

    private int mResponseCode;
    private String mResponseMessage;
    private String mErrorMessage;
    private String mTransferEncoding;
    private long mContentLength;
    private long mRangeLength;
    private String mLocation;

    public DownloadResponse(HttpURLConnection conn) throws IOException {
        this.mResponseCode = conn.getResponseCode();
        this.mResponseMessage = conn.getResponseMessage();
        readResponseHeaders(conn);
        readErrorMessage(conn);
    }

    private void readErrorMessage(HttpURLConnection conn) {
        InputStream errorInputStream = conn.getErrorStream();
        if (errorInputStream == null) {
            return;
        }

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        try {
            int len;
            byte[] buffer = new byte[1024];
            while ((len = errorInputStream.read(buffer)) > 0) {
                arrayOutputStream.write(buffer, 0, len);
            }
            mErrorMessage = new String(arrayOutputStream.toByteArray(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                errorInputStream.close();
                arrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readResponseHeaders(HttpURLConnection conn) {
        mTransferEncoding = conn.getHeaderField("Transfer-Encoding");
        if (mTransferEncoding == null) {
            mContentLength = getHeaderFieldLong(conn, "Content-Length", -1);
        } else {
            mContentLength = -1;
            Log.v("Ignoring Content-Length since Transfer-Encoding is also defined");
        }

        if (mResponseCode == HttpURLConnection.HTTP_PARTIAL) {
            readContentRange(conn);
        }

        mLocation = conn.getHeaderField("Location");
    }

    private void readContentRange(HttpURLConnection conn) {
        try {
            String contentRang = conn.getHeaderField("Content-Range");
            String regEx = "bytes ([0-9]+)-([0-9]+)/([0-9]+)";
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(contentRang);
            if (matcher.find()) {
                mRangeLength = Long.parseLong(matcher.group(1));
                // here is real total content length
                mContentLength = Long.parseLong(matcher.group(3));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getHeaderFieldLong(URLConnection conn, String field, long defaultValue) {
        try {
            return Long.parseLong(conn.getHeaderField(field));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean isValidContentLength() {
        return mContentLength > -1 || "chunked".equalsIgnoreCase(mTransferEncoding);
    }

    public int getResponseCode() {
        return mResponseCode;
    }

    public String getResponseMessage() {
        return mResponseMessage;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public String getTransferEncoding() {
        return mTransferEncoding;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public long getRangeLength() {
        return mRangeLength;
    }

    public String getLocation() {
        return mLocation;
    }
}
