package net.pagekite.app;

import net.pagekite.lib.PageKiteAPI;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Service extends android.app.Service {

	private static final int NOTIFICATION_ID = 1;

	public Service() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    handleCommand(intent);
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

	private void handleCommand(Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		String problem = "";
		String kiteName = prefs.getString("kiteName", "kitename");
		String kiteSecret = prefs.getString("kiteSecret", "secret");
		int httpPortNumber = Integer.parseInt(prefs.getString("httpPortNumber", "0"));
		int websocketPortNumber = Integer.parseInt(prefs.getString("websocketPortNumber", "0"));
		int httpsPortNumber = Integer.parseInt(prefs.getString("httpsPortNumber", "0"));
		int sshPortNumber = Integer.parseInt(prefs.getString("sshPortNumber", "0"));
		String localip = "localhost";
		if (prefs.getBoolean("useWiFiIP", false)) {
			localip = getWiFiIP();
		}

		if ((kiteName == "kitename") || (kiteSecret == "secret")) {
			Toast.makeText(getBaseContext(),
				       "Please add a kite name and shared secret.",
				       Toast.LENGTH_LONG).show();
			stopSelf();
		}
		else if ((httpPortNumber + websocketPortNumber + httpsPortNumber) == 0) {
			Toast.makeText(getBaseContext(),
				       "Port numbers are all zero!",
				       Toast.LENGTH_LONG).show();
			stopSelf();
		}
		else {
			boolean ok = true;
			if (ok) {
				ok = PageKiteAPI.init(3, 3, 10);
				problem = "Init failed.";
			}
			if (ok && (httpPortNumber > 0)) {
				ok = PageKiteAPI.addKite("http", kiteName, 0, kiteSecret,
						             	 localip, httpPortNumber);
				problem = "add_kite(http, ...) failed.";
			}
			if (ok && (websocketPortNumber > 0)) {
				ok = PageKiteAPI.addKite("websocket", kiteName, 0, kiteSecret,
						             	 localip, websocketPortNumber);
				problem = "add_kite(websockets, ...) failed.";
			}
			if (ok && (httpsPortNumber > 0)) {
				ok = PageKiteAPI.addKite("https", kiteName, 0, kiteSecret,
					   	                 localip, httpsPortNumber);
				problem = "add_kite(https, ...) failed.";
			}
			if (ok && (sshPortNumber > 0)) {
				ok = PageKiteAPI.addKite("raw", kiteName, 22, kiteSecret,
					   	                 localip, sshPortNumber);
				problem = "add_kite(ssh, ...) failed.";
			}
			if (ok) {
				ok = PageKiteAPI.addFrontend(kiteName, 443, 1);
				problem = "add_frontend(...) failed.";
			}
			if (ok) {
				ok = PageKiteAPI.start();
				problem = "start failed.";
			}
		    if (ok)
			{
				if (prefs.getBoolean("showNotification", false)) {
					Notification nfy = new Notification(R.drawable.notify_icon,
							getText(R.string.ticker_text),
							System.currentTimeMillis());
					Intent nfInt = new Intent(this, Preferences.class);
					PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
							nfInt, 0);
					nfy.setLatestEventInfo(this,
							getText(R.string.notification_title),
							getText(R.string.pagekite_running),
							pendingIntent);
					startForeground(NOTIFICATION_ID, nfy);
				}
				else {
					Toast.makeText(getBaseContext(),
						       "Started PageKite Service",
						       Toast.LENGTH_LONG).show();					
				}
			}
			else {
				Toast.makeText(getBaseContext(),
						       "PageKite Failed, " + problem,
						       Toast.LENGTH_LONG).show();
				stopSelf();
			}
		}
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		if (PageKiteAPI.stop()) {
			Toast.makeText(getBaseContext(), "Stopped PageKite.",
				       Toast.LENGTH_LONG).show();
		}
		else {
			Toast.makeText(getBaseContext(), "Stopped PageKite failed!?",
				       Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
