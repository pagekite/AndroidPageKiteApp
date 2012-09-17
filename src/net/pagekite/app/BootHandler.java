package net.pagekite.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootHandler extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent ntnt) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(ctx);
		if (p.getBoolean("startOnBoot", false) && 
			p.getBoolean("enablePageKite", false)) {
			ctx.startService(new Intent(ctx, Service.class));
		}
	}
}
