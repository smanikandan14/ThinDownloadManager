package com.thin.downloadmanager;

import android.os.Handler;
import android.os.Looper;

import com.thin.downloadmanager.util.Log;

import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadRequestQueue {

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

	private CallBackDelivery mDelivery;

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
		initialize(new Handler(Looper.getMainLooper()), threadPoolSize);
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
			mCurrentRequests.add(request);
		}

		// Process requests in the order they are added.
		request.setDownloadId(downloadId);
		mDownloadQueue.add(request);

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

	/**
	 * Pause a particular download in progress.
	 *
	 * @param downloadId - selected download request Id
	 * @return It will return 1 if the download Id is found else returns 0.
	 */
	int pause(int downloadId) {
		checkResumableDownloadEnabled(downloadId);
		return this.cancel(downloadId);
	}

	/**
	 * Pause all the dispatchers in work and also cancel and stops the dispatchers.
	 */
	void pauseAll() {
		checkResumableDownloadEnabled(-1); // Error code -1 handle for cancelAll()
		this.cancelAll();
	}


	/**
	 * This is called by methods that want to throw an exception if the {@link DownloadRequest}
	 * hasn't enable isResumable feature.
	 */
	private void checkResumableDownloadEnabled(int downloadId) {
		synchronized (mCurrentRequests) {
			for (DownloadRequest request : mCurrentRequests) {
				if (downloadId == -1 && !request.isResumable()) {
					Log.e("ThinDownloadManager",
							String.format(Locale.getDefault(), "This request has not enabled resume feature hence request will be cancelled. Request Id: %d", request.getDownloadId()));
				} else if ((request.getDownloadId() == downloadId && !request.isResumable())) {
					throw new IllegalStateException("You cannot pause the download, unless you have enabled Resume feature in DownloadRequest.");
				} else {
					//ignored, It can not be a scenario to happen.
				}
			}
		}
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
	 * Perform construction with custom thread pool size.
        */
	private void initialize(Handler callbackHandler, int threadPoolSize) {
		mDownloadDispatchers = new DownloadDispatcher[threadPoolSize];
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
}
