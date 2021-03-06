package net.pagekite.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.pagekite.lib.PageKiteAPI;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class Service extends android.app.Service {

	private static final String TAG = "PageKite.Service";
	
	public static final String TICK_INTENT = "net.pagekite.lib.TICK";
	public static final String STATUS_UPDATE_INTENT = "net.pagekite.lib.STATUS_UPDATE";
	public static final String STATUS_SERVICE = "svc";
	public static final String STATUS_PAGEKITE = "pagekite";
	public static final String STATUS_COUNT = "count";

	public static final int STATUS_SERVICE_STARTED = 1;
	public static final int STATUS_SERVICE_STOPPED = 2;

	private static final int NOTIFICATION_ID = 1;

	private Notification mNotification = null;
	private static int mStatusCounter = 0;
	public static String mStatusText;
	public static int mStatusIconId;
	public static String mStatusTextMore;
	public static boolean isRunning = false;

	private boolean mKeepRunning = true;
	private String mKiteName = null;
	private Handler mHandler = null;
	private NotificationManager mNotificationManager = null;
	private BroadcastReceiver mConnChangeReceiver = null;
	private BroadcastReceiver mTimerReceiver = null;

	public Service() {
		mHandler = new Handler();
		mConnChangeReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				boolean nonet = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				Log.d(TAG, "Connectivity state: " + nonet);
				PageKiteAPI.setHaveNetwork(!nonet);
				if (!nonet) PageKiteAPI.tick();
				setInexactTimer(!nonet);
			}
		};
        mTimerReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent event) {
				Log.d(TAG, "Inexact timer fired, sending tick.");
				PageKiteAPI.tick();
			}
        };
    }

	/***** Legacy Android 1.6 support begins ***************************************/
	@Override
	public void onStart(Intent intent, int startId) {
	    doStartup();
	}

    @SuppressWarnings("rawtypes")
	private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    @SuppressWarnings("rawtypes")
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    
    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
	@SuppressLint("NewApi")
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke startForeground", e);
            }
            return;
        }
        
        // Fall back on the old API.
        setForeground(true);
        mNM.notify(id, notification);
    }
    
    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
	@SuppressLint("NewApi")
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("MyApp", "Unable to invoke stopForeground", e);
            }
            return;
        }
        
        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        setForeground(false);
    }
	
	/***** Legacy Android 1.6 support ends *****************************************/

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    doStartup();
	    return START_STICKY;
	}

	public String getWiFiIP() {
	   WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
	   WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	   int ip = wifiInfo.getIpAddress();

	   return String.format("%d.%d.%d.%d",
			    			(ip & 0xff),       (ip >> 8 & 0xff),
	                        (ip >> 16 & 0xff), (ip >> 24 & 0xff));
	}

	private void doStartup() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		String problem = "";
		mKiteName = prefs.getString("kiteName", "kitename");
		String kiteSecret = prefs.getString("kiteSecret", "secret");
		int httpPortNumber = Preferences.parseInt(prefs.getString("httpPortNumber", "0"));
		int websocketPortNumber = Preferences.parseInt(prefs.getString("websocketPortNumber", "0"));
		int httpsPortNumber = Preferences.parseInt(prefs.getString("httpsPortNumber", "0"));
		int sshPortNumber = Preferences.parseInt(prefs.getString("sshPortNumber", "0"));

		String localip = "localhost";
		if (prefs.getBoolean("useWiFiIP", false)) {
			localip = getWiFiIP();
		}

		if ((mKiteName == "kitename") || (kiteSecret == "secret")) {
			Toast.makeText(getBaseContext(),
				       getText(R.string.need_account_details),
				       Toast.LENGTH_LONG).show();
			stopSelf();
		}
		else if ((httpPortNumber + websocketPortNumber + httpsPortNumber + sshPortNumber) == 0) {
			Toast.makeText(getBaseContext(),
				       getText(R.string.need_service_ports),
				       Toast.LENGTH_LONG).show();
			stopSelf();
		}
		else {
			statusUpdateLoop();
			boolean ok = true;
			if (ok) {
				boolean debug = prefs.getBoolean("enableDebugging", false);
				String id_s = getText(R.string.app_id_short).toString();
				String id_l = getText(R.string.app_id_long).toString();
				if (prefs.getBoolean("usePageKiteNet", false)) {
					ok = PageKiteAPI.initPagekiteNet(5, 30, id_s, id_l, debug);
				}
				else {
					ok = (PageKiteAPI.init(5, 2, 30, null, id_s, id_l, debug) &&
					      PageKiteAPI.addFrontend(mKiteName, 443));
				}
				problem = "Init failed.";
			}
			if (ok && (httpPortNumber > 0)) {
				ok = PageKiteAPI.addKite("http", mKiteName, 0, kiteSecret,
						             	 localip, httpPortNumber);
				problem = "add_kite(http, ...) failed.";
			}
			if (ok && (websocketPortNumber > 0)) {
				ok = PageKiteAPI.addKite("websocket", mKiteName, 0, kiteSecret,
						             	 localip, websocketPortNumber);
				problem = "add_kite(websockets, ...) failed.";
			}
			if (ok && (httpsPortNumber > 0)) {
				ok = PageKiteAPI.addKite("https", mKiteName, 0, kiteSecret,
					   	                 localip, httpsPortNumber);
				problem = "add_kite(https, ...) failed.";
			}
			if (ok && (sshPortNumber > 0)) {
				ok = PageKiteAPI.addKite("raw", mKiteName, 22, kiteSecret,
					   	                 localip, sshPortNumber);
				problem = "add_kite(ssh, ...) failed.";
			}
			if (ok) {
				ok = PageKiteAPI.start();
				problem = "start failed.";
			}
		    if (ok)
			{
				if (prefs.getBoolean("showNotification", false)) {
					mNotification = new Notification(R.drawable.notify_icon,
							getText(R.string.ticker_text),
							System.currentTimeMillis());
					updateStatusText();
					updateNotification(false);
					startReceivers();
					startForegroundCompat(NOTIFICATION_ID, mNotification);
				}
				else {
					Toast.makeText(getBaseContext(),
						       "Started PageKite Service",
						       Toast.LENGTH_LONG).show();					
				}
				setPrefActive(prefs, true);
				announce(STATUS_SERVICE_STARTED, 0);
			}
			else {
				Toast.makeText(getBaseContext(),
						       "PageKite Error: " + problem,
						       Toast.LENGTH_LONG).show();
				setPrefActive(prefs, false);
				stopSelf();
				announce(STATUS_SERVICE_STOPPED, 0);
			}
		}
	}
	
	void startReceivers() {
		registerReceiver(mConnChangeReceiver,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(mTimerReceiver,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	void stopReceivers() {
		try {
			setInexactTimer(false);
			unregisterReceiver(mConnChangeReceiver);
			unregisterReceiver(mTimerReceiver);
		} catch (IllegalArgumentException e) {
			// Ignore
		}
	}

	void setInexactTimer(boolean on) {
		Intent i = new Intent(TICK_INTENT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.cancel(pi); // cancel any existing alarms
		if (on) {
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				    SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				    AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
		}
	}

	static void setPrefActive(SharedPreferences prefs, boolean active) {
		Editor editor = prefs.edit();
		editor.putBoolean("enablePageKite", active);
		editor.commit();
		isRunning = active;
	}
	
	void announce(Integer serviceStatus, Integer pagekiteStatus) {
		Intent croak = new Intent(STATUS_UPDATE_INTENT);
		croak.putExtra(STATUS_SERVICE, serviceStatus);
		croak.putExtra(STATUS_PAGEKITE, pagekiteStatus);
		croak.putExtra(STATUS_COUNT, mStatusCounter++);
		sendBroadcast(croak);
	}

	void updateNotification(boolean notify) {
		if (mNotification != null) {
			Intent nfInt = new Intent(this, Preferences.class);
			nfInt.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			mNotification.icon = mStatusIconId;
			mNotification.setLatestEventInfo(this,
					mKiteName,
					(mStatusTextMore == null) ? mStatusText : mStatusTextMore,
					PendingIntent.getActivity(this, 0, nfInt, 0));
			if (notify) {
				if (mNotificationManager == null) {
					String ns = Context.NOTIFICATION_SERVICE;
					mNotificationManager = (NotificationManager) getSystemService(ns);					
				}
				if (mNotificationManager != null) {
					mNotificationManager.notify(NOTIFICATION_ID, mNotification);
				}
			}
		}
	}

	boolean updateStatusText() {
		String oldStatusText = mStatusText + mStatusTextMore;
		mStatusTextMore = null;
		switch (PageKiteAPI.getStatus()) {
		case PageKiteAPI.PK_STATUS_STARTUP:
			mStatusIconId = R.drawable.notify_icon;
			mStatusText = getText(R.string.status_startup).toString();
			break;
		case PageKiteAPI.PK_STATUS_CONNECT:
			mStatusIconId = R.drawable.notify_icon_updn;
			mStatusText = getText(R.string.status_connect).toString();
			break;
		case PageKiteAPI.PK_STATUS_DYNDNS:
			mStatusIconId = R.drawable.notify_icon_updn;
			mStatusText = getText(R.string.status_dyndns).toString();
			break;
		case PageKiteAPI.PK_STATUS_PROBLEMS:
			mStatusIconId = R.drawable.notify_icon_dark;
			mStatusText = getText(R.string.status_problems).toString();
			break;
		case PageKiteAPI.PK_STATUS_REJECTED:
			mStatusIconId = R.drawable.notify_icon_dark;
			mStatusText = getText(R.string.status_rejected).toString();
			break;
		case PageKiteAPI.PK_STATUS_NO_NETWORK:
			mStatusIconId = R.drawable.notify_icon_dark;
			mStatusText = getText(R.string.status_no_network).toString();
			break;
		case PageKiteAPI.PK_STATUS_FLYING:
			int liveClients = PageKiteAPI.getLiveStreams();
			mStatusIconId = (liveClients > 0) ? R.drawable.notify_icon_updn :
				                                R.drawable.notify_icon;
			mStatusText = getText(R.string.status_flying).toString();
			mStatusTextMore = 
					getText(R.string.status_frontends) + ": " + PageKiteAPI.getLiveFrontends() + 
					"  " +
					getText(R.string.status_streams) + ": " + liveClients;
			break;
		default:
			mStatusIconId = R.drawable.notify_icon_dark;
			mStatusText = getText(R.string.status_unknown).toString();
		}
		return (oldStatusText != (mStatusText + mStatusTextMore));
	}

	void statusUpdateLoop() {
		new Thread(new Runnable() {
	        public void run() {
	        	mKeepRunning = true;
	    		while (mKeepRunning) {
	    			PageKiteAPI.poll(3600);
	    			try { Thread.sleep(250); } catch (InterruptedException e) { }
	    			mHandler.post(new Runnable() {
	    				public void run() {
	    					if (updateStatusText()) {
	    						announce(0, PageKiteAPI.getStatus());
	    						updateNotification(true);
	    					}
	    				}
	    			});
	    		}
	        }
	    }).start();
	}

	@Override
	public void onDestroy() {
		mKeepRunning = false;
		mNotification = null;
		stopReceivers();
		PageKiteAPI.stop();
		stopForegroundCompat(NOTIFICATION_ID);
		setPrefActive(PreferenceManager.getDefaultSharedPreferences(getBaseContext()),
				      false);
		announce(STATUS_SERVICE_STOPPED, 0);
	}

	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
}