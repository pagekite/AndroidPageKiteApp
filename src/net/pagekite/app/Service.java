package net.pagekite.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;
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

	private void handleCommand(Intent intent) {
		// TODO: Do real work.
		Notification notification = new Notification(R.drawable.notify_icon,
													 getText(R.string.ticker_text),
													 System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, Preferences.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
																notificationIntent, 0);
		notification.setLatestEventInfo(this, getText(R.string.notification_title),
		        						getText(R.string.pagekite_running),
		        						pendingIntent);
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		Toast.makeText(getBaseContext(), "Stopped PageKite.", Toast.LENGTH_LONG).show();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
