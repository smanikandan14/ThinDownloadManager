package com.thin.downloadmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRequestQueue {

	private static final String TAG = DownloadRequestQueue.class.getSimpleName();
	private final boolean DEBUG = false;
	/**
	 * The set of all requests currently being processed by this RequestQueue. A Request will be in this set if it is waiting in any queue or currently being processed by any dispatcher.
	 */
	private Set<DownloadRequest> mCurrentRequests = new HashSet<>();

	/** The queue of requests that are actually going out to the network. */
	private PriorityBlockingQueue<DownloadRequest> mDownloadQueue = new PriorityBlockingQueue<>();

	/** The download dispatchers */
	private DownloadDispatcher[] mDownloadDispatchers;

	/** Used for generating monotonically-increasing sequence numbers for requests. */
	private AtomicInteger mSequenceGenerator = new AtomicInteger();

	private final AtomicBoolean mWifiBroadcastIsRegistered = new AtomicBoolean(false);

	private CallBackDelivery mDelivery;

	private final WifiStateReceiver mWifiStateReceiver = new WifiStateReceiver();

	/**
	 * Delivery class to delivery the call back to call back registrar in main thread.
	 */
	class CallBackDelivery {

		/** Used for posting responses, typically to the main thread. */
		private final Executor mCallBackExecutor;

		/**
		 * Constructor taking a handler to main thread.
		 */
		public CallBackDelivery(final Handler handler) {
			// Make an Executor that just wraps the handler.
			mCallBackExecutor = new Executor() {
				@Override
				public void execute(Runnable command) {
					handler.post(command);
				}
			};
		}

		public void postDownloadComplete(final DownloadRequest request) {
			mCallBackExecutor.execute(new Runnable() {
				public void run() {
					if (request.getDownloadListener() != null) {
						request.getDownloadListener().onDownloadComplete(request.getDownloadId());
					}
					if (request.getStatusListener() != null) {
						request.getStatusListener().onDownloadComplete(request);
					}
				}
			});
		}

		public void postDownloadFailed(final DownloadRequest request, final int errorCode, final String errorMsg) {
			mCallBackExecutor.execute(new Runnable() {
				public void run() {
					if (request.getDownloadListener() != null) {
						request.getDownloadListener().onDownloadFailed(request.getDownloadId(), errorCode, errorMsg);
					}
					if (request.getStatusListener() != null) {
						request.getStatusListener().onDownloadFailed(request, errorCode, errorMsg);
					}
				}
			});
		}

		public void postProgressUpdate(final DownloadRequest request, final long totalBytes, final long downloadedBytes, final int progress) {
			mCallBackExecutor.execute(new Runnable() {
				public void run() {
					if (request.getDownloadListener() != null) {
						request.getDownloadListener().onProgress(request.getDownloadId(), totalBytes, downloadedBytes, progress);
					}
					if (request.getStatusListener() != null) {
						request.getStatusListener().onProgress(request, totalBytes, downloadedBytes, progress);
					}
				}
			});
		}
	}

	/**
	 * Default constructor.
	 */
	public DownloadRequestQueue() {
		initialize(new Handler(Looper.getMainLooper()));
	}

	/**
	 * Creates the download dispatchers workers pool.
	 *
	 * Deprecated:
	 */
	public DownloadRequestQueue(int threadPoolSize) {
		initialize(new Handler(Looper.getMainLooper()));
	}

	/**
	 * Construct with provided callback handler.
	 *
	 * @param callbackHandler
	 */
	public DownloadRequestQueue(Handler callbackHandler) throws InvalidParameterException {
		if (callbackHandler == null) {
			throw new InvalidParameterException("callbackHandler must not be null");
		}

		initialize(callbackHandler);
	}

	public void start() {
		stop(); // Make sure any currently running dispatchers are stopped.

		// Create download dispatchers (and corresponding threads) up to the pool size.
		for (int i = 0; i < mDownloadDispatchers.length; i++) {
			DownloadDispatcher downloadDispatcher = new DownloadDispatcher(mDownloadQueue, mDelivery);
			mDownloadDispatchers[i] = downloadDispatcher;
			downloadDispatcher.start();
		}
	}

	// Package-Private methods.
	/**
	 * Generates a download id for the request and adds the download request to the download request queue for the dispatchers pool to act on immediately.
	 *
	 * @param request
	 * @return downloadId
	 */
	int add(DownloadRequest request) {
		int downloadId = getDownloadId();
		// Tag the request as belonging to this queue and add it to the set of current requests.
		request.setDownloadRequestQueue(this);

		synchronized (mCurrentRequests) {
			if (mCurrentRequests.contains(request)) mCurrentRequests.remove(request); // HashSet doesn't replace "equal" objects
			mCurrentRequests.add(request);
		}

		// Process requests in the order they are added.
		request.setDownloadId(downloadId);

		startIfRequestedNetworkIsActive(request);

		return downloadId;
	}

	/**
	 * Returns the current download state for a download request.
	 *
	 * @param downloadId
	 * @return
	 */
	int query(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.getDownloadId() == downloadId) {
					return request.getDownloadState();
				}
			}
		}
		return DownloadManager.STATUS_NOT_FOUND;
	}

	/**
	 * Cancel all the dispatchers in work and also stops the dispatchers.
	 */
	void cancelAll() {

		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				request.cancel();
			}

			// Remove all the requests from the queue.
			mCurrentRequests.clear();
		}
	}

	/**
	 * Cancel a particular download in progress. Returns 1 if the download Id is found else returns 0.
	 *
	 * @param downloadId
	 * @return int
	 */
	int cancel(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (request.getDownloadId() == downloadId) {
					request.cancel();
					return 1;
				}
			}
		}

		return 0;
	}

	void finish(DownloadRequest request) {
		if (mCurrentRequests != null) {//if finish and release are called together it throws NPE
			// Remove from the queue.
			synchronized (mCurrentRequests) {
				mCurrentRequests.remove(request);
			}
		}
	}

	/**
	 * Cancels all the pending & running requests and releases all the dispatchers.
	 */
	void release() {
		if (mCurrentRequests != null) {
			synchronized (mCurrentRequests) {
				mCurrentRequests.clear();
				mCurrentRequests = null;
			}
		}

		if (mDownloadQueue != null) {
			mDownloadQueue = null;
		}

		if (mDownloadDispatchers != null) {
			stop();

			for (int i = 0; i < mDownloadDispatchers.length; i++) {
				mDownloadDispatchers[i] = null;
			}
			mDownloadDispatchers = null;
		}

	}

	// Private methods.

	/**
	 * Perform construction.
	 *
	 * @param callbackHandler
	 */
	private void initialize(Handler callbackHandler) {
		int processors = Runtime.getRuntime().availableProcessors();
		mDownloadDispatchers = new DownloadDispatcher[processors];
		mDelivery = new CallBackDelivery(callbackHandler);
	}

	/**
	 * Stops download dispatchers.
	 */
	private void stop() {
		for (int i = 0; i < mDownloadDispatchers.length; i++) {
			if (mDownloadDispatchers[i] != null) {
				mDownloadDispatchers[i].quit();
			}
		}
	}

	/**
	 * Gets a sequence number.
	 */
	private int getDownloadId() {
		return mSequenceGenerator.incrementAndGet();
	}

	/**
	 * Try to switch to wifi. Active downloads may interfere
	 * @return True for wifi is now preferred network
	 */
	private boolean attemptSwitchToWifi(Context context) {
		boolean wifiIsActive = false;

		NetworkHelper networkHelper = NetworkHelper.getInstance(context);
		// if wifi is the active network
		if (networkHelper.isActiveNetworkWifi()) {
			networkHelper.setPreferWifi();
			wifiIsActive = true;
			if (DEBUG) Log.d(TAG, "attemptSwitchToWifi() - active network already WiFi.");
		} else { // another network type is active
			if (areAllActiveDownloadsWifiOnly()) { // TODO determine if this is an issue
				if (networkHelper.isWifiAvailable()) {
					networkHelper.setPreferWifi();
					wifiIsActive = true;
					if (DEBUG) Log.d(TAG, "attemptSwitchToWifi() - setting prefer wifi because all wifi-only.");
				} else {
					if (DEBUG) Log.d(TAG, "attemptSwitchToWifi() - WiFi is disabled or another non-enabled status.");
					// WiFi is disabled or another non-enabled status.
					// It is the responsibility of the app to enable Wifi: We simply handle network change broadcasts.
				}
			} else {
				if (DEBUG) Log.d(TAG, "attemptSwitchToWifi() - Not all active downloads are wifi-only.");
			}
		}

		return wifiIsActive;
	}

	private boolean areAnyDownloadsActive() {
		boolean dldActive = false;

		for (DownloadRequest downloadRequest : mDownloadQueue) {
			if (downloadRequest.isStateActive()) {
				dldActive = true;
				break;
			}
		}
		return dldActive;
	}

	/**
	 * returns true if a download is active that doesn't explicitly request wifi-only
	 * @return
	 */
	private boolean areAllActiveDownloadsWifiOnly() {
		boolean allWifiOnly = true;

		for (DownloadRequest downloadRequest : mDownloadQueue) {
			if (!downloadRequest.isWifiOnly()) {
				if (downloadRequest.isStateActive()) {
					allWifiOnly = false;
					break;
				}
			}
		}

		return allWifiOnly;
	}

	private void registerForWifiConnectedBroadcast(Context context) {
		// check if already registered
		synchronized (mWifiBroadcastIsRegistered) {
			if (!mWifiBroadcastIsRegistered.get()) {
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

				try {
					context.registerReceiver(mWifiStateReceiver, intentFilter);
				} catch (IllegalArgumentException e) {
					// register attempted while already registered
				}

				mWifiBroadcastIsRegistered.set(true);
			}
		}
	}

	private void unregisterWifiConnectedBroadcast(Context context) {
		synchronized (mWifiBroadcastIsRegistered) {
			if (mWifiBroadcastIsRegistered.get()) {
				try {
					context.unregisterReceiver(mWifiStateReceiver);
				} catch (IllegalArgumentException e) {
					// receiver was not registered
				}

				mWifiBroadcastIsRegistered.set(false);
			}
		}
	}

	private void tryToStartWifiOnlyDownloads() {
		// if there are any pending WiFi downloads.
		if (atLeastOnePendingWifiOnlyDownload()) {
			// if all active downloads are WiFi-only
			if (areAllActiveDownloadsWifiOnly()) {
				// Request prefer wifi
				NetworkHelper netHelp = NetworkHelper.getInstance();
				if (netHelp != null) {
					netHelp.setPreferWifi();
				}

				if (DEBUG) Log.d(TAG, "tryToStartWifiOnlyDownloads() - all active downloads were wifi-only");

				startAllPendingDownloads();
			} else { // another network is active
				if (DEBUG) Log.d(TAG, "tryToStartWifiOnlyDownloads() - a non-wifi-only download was active");
			}
		} else {
			if (!atLeastOneActiveWifiOnlyDownload()) {
				// reset preferred network
				NetworkHelper netHelp = NetworkHelper.getInstance();
				if (netHelp != null) {
					netHelp.clearNetworkPreference();
				}
				if (DEBUG) Log.d(TAG, "tryToStartWifiOnlyDownloads() - there were no active wifi downloads");
			}
			if (DEBUG) Log.d(TAG, "tryToStartWifiOnlyDownloads() - there were no pending wifi downloads");
		}
	}

	private boolean atLeastOneActiveWifiOnlyDownload() {
		boolean exists = false;

		for (DownloadRequest request : mDownloadQueue) {
			if (request.isWifiOnly()) {
				exists = true;
				break;
			}
		}

		return exists;
	}

	private void startAllPendingDownloads() {
		for (DownloadRequest request : mCurrentRequests) {
			if (request.getDownloadState()==DownloadManager.STATUS_PENDING) {
				startIfRequestedNetworkIsActive(request);
			}
		}
	}

	private void startIfRequestedNetworkIsActive(DownloadRequest request) {
		boolean requestedNetworkIsActive = true;

		if (request.isWifiOnly()) {
			requestedNetworkIsActive = attemptSwitchToWifi(request.getAppContext());

			if (!requestedNetworkIsActive) registerForWifiConnectedBroadcast(request.getAppContext());
		}

		if (DEBUG) Log.d(TAG, "startIfRequestedNetworkIsActive() - requestedNetworkIsActive="+requestedNetworkIsActive);
		if (requestedNetworkIsActive) {
			cancelActiveDownloadWithSameDestinationUri(request);
			mDownloadQueue.add(request);
		}
	}

	private boolean atLeastOnePendingWifiOnlyDownload() {
		boolean exist = false;

		for (DownloadRequest downloadRequest : mCurrentRequests) {
			if (downloadRequest.getDownloadState()==DownloadManager.STATUS_PENDING
					&& downloadRequest.isWifiOnly()) {
				exist = true;
				break;
			}
		}

		return exist;
	}

	private boolean cancelActiveDownloadWithSameDestinationUri(DownloadRequest newRequest) {
		boolean canceledOldDownload = false;

		for (DownloadRequest downloadRequest : mCurrentRequests) {
			if (downloadRequest.getDownloadId()!=newRequest.getDownloadId() &&
					downloadRequest.getDestinationURI().getPath().equals(newRequest.getDestinationURI().getPath())) {
				downloadRequest.cancel();
				canceledOldDownload = true;
			}
		}

		return canceledOldDownload;
	}

	private class WifiStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			synchronized (mWifiBroadcastIsRegistered) {
				if (mWifiBroadcastIsRegistered.get()) {
					if (NetworkHelper.getInstance(context).isWifiAvailable()) {
						if (DEBUG) Log.d(TAG, "WifiStateReceiver.onReceive() Wifi is Available!");
						// if there are no non-wifi downloads active, tryToStart
						tryToStartWifiOnlyDownloads();

						unregisterWifiConnectedBroadcast(context);
					}
				} else {
					unregisterWifiConnectedBroadcast(context);
				}
			}
		}
	}
}
