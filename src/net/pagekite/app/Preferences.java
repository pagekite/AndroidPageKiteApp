package net.pagekite.app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {
	// TODO: Update checkbox status periodically?

	@Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            Preference enabled = updateStatus(false);
            enabled.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference pref, Object obj) {
					CheckBoxPreference enabled = updateStatus(true);
					if (enabled.isChecked()) {
						Intent startup = new Intent(getBaseContext(), Service.class);
						startService(startup);
					}
					else {
						Intent stop = new Intent(getBaseContext(), Service.class);
						stopService(stop);
					}
					return true;
				}
            });
    }
	
	protected CheckBoxPreference updateStatus(boolean changing) {
		boolean status = false;
		CheckBoxPreference enabled = (CheckBoxPreference) findPreference("enablePageKite");
		enabled.setSummary("PageKite might be running! You never know!");
		
        // TODO: Actually check status, update summary and checkbox.
		status = enabled.isChecked() ^ changing;
		
        enabled.setChecked(status);
        this.findPreference("kiteName").setEnabled(!status);
        this.findPreference("kiteSecret").setEnabled(!status);
        this.findPreference("httpPortNumber").setEnabled(!status);
        this.findPreference("websocketPortNumber").setEnabled(!status);
        this.findPreference("httpsPortNumber").setEnabled(!status);
		return enabled;
	}
}