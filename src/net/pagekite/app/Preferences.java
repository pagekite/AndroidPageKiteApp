package net.pagekite.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

	private SharedPreferences mPrefs;
	private IntentFilter mListenFilter;
	private Integer mStatusCounter = 0;

	private Handler mHandler;
	private BroadcastReceiver mListener;
	private boolean mListening = false;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		// Fix the checkbox if it is wrong.
		if (mPrefs.getBoolean("enablePageKite", Service.isRunning) != Service.isRunning) {
			Service.setPrefActive(mPrefs, Service.isRunning);
		} 

        addPreferencesFromResource(R.xml.preferences);

        updateStatus(false).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference pref, Object obj) {
				if (mPrefs.getBoolean("enablePageKite", false)) {
					CheckBoxPreference enabled = (CheckBoxPreference) findPreference("enablePageKite");
					enabled.setEnabled(false);
					if (!stopService(new Intent(getBaseContext(), Service.class))) {
						enabled.setChecked(false);
						enabled.setEnabled(true);
						updateStatus(false);
					};
				}
				else {
					if (kitesAreConfigured()) {
						findPreference("enablePageKite").setEnabled(false);
				        findPreference("prefsAccount").setEnabled(false);
				        findPreference("prefsLocalhost").setEnabled(false);
				        findPreference("showNotification").setEnabled(false);
				        findPreference("startOnBoot").setEnabled(false);
						startService(new Intent(getBaseContext(), Service.class));
					}
					else {
						Toast.makeText(getBaseContext(),
									getText(R.string.need_account_details),
									Toast.LENGTH_LONG).show();
					}
				}
				return false;
			}
        });

        mHandler = new Handler();
        OnPreferenceChangeListener opcl = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
		        new Thread(new Runnable() {
		            @Override
		            public void run() {
		                try {Thread.sleep(100);} catch (InterruptedException e) {}
		                mHandler.post(new Runnable() {
		                    @Override
		                    public void run() { updateSummaries(); }
		                });
		            }
		        }).start();
				return true;
			}
        };
        findPreference("kiteName").setOnPreferenceChangeListener(opcl);
        findPreference("httpPortNumber").setOnPreferenceChangeListener(opcl);
        findPreference("httpsPortNumber").setOnPreferenceChangeListener(opcl);
        findPreference("websocketPortNumber").setOnPreferenceChangeListener(opcl);
        findPreference("sshPortNumber").setOnPreferenceChangeListener(opcl);
		updateSummaries();

        mListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context arg0, Intent event) {
				CheckBoxPreference enabled = (CheckBoxPreference) findPreference("enablePageKite");
				enabled.setChecked(mPrefs.getBoolean("enablePageKite", false));
		        findPreference("enablePageKite").setEnabled(true);
		        int counter = event.getIntExtra(Service.STATUS_COUNT, -1);
		        if (counter >= mStatusCounter) {
					updateStatus(false);
		        	mStatusCounter = counter;
		        }
			}
        };
        mListenFilter = new IntentFilter(Service.STATUS_UPDATE_INTENT);
        mListening = false;
    }

	@Override
	protected void onResume() {
		super.onResume();
		updateStatus(false);
        if (!mListening) {
        	registerReceiver(mListener, mListenFilter);
            mListening = true;
        }
	}

	@Override
	protected void onPause() {
		super.onPause();
        if (mListening) {
            unregisterReceiver(mListener);
            mListening = false;
        }
	}

	void setPageKiteEnabled(boolean enabled) {
		updateStatus(false);
	}

	boolean kitesAreConfigured() {
		String kiteName = ((EditTextPreference) findPreference("kiteName")).getText();
		String kiteSecret = ((EditTextPreference) findPreference("kiteSecret")).getText();
		return ((kiteName != null) && (kiteSecret != null) &&
                (kiteName.length() > 0) && (kiteSecret.length() > 0) &&
                (kiteName.toLowerCase() != getText(R.string.pagekite_default_kitename).toString().toLowerCase()));
	}
	
	protected void updateSummaries() {
		updateStringPreference("kiteName", R.string.pagekite_summary_kitename,
                R.string.pagekite_default_kitename,
                R.string.pagekite_explain_kitename);
		updateIntegerPreference("httpPortNumber", R.string.pagekite_summary_httpport);
		updateIntegerPreference("httpsPortNumber", R.string.pagekite_summary_httpsport);
		updateIntegerPreference("websocketPortNumber", R.string.pagekite_summary_wsport);
		updateIntegerPreference("sshPortNumber", R.string.pagekite_summary_sshport);
	}
	
	protected CheckBoxPreference updateStatus(boolean changing) {
		boolean status = false;
		CheckBoxPreference enabled = (CheckBoxPreference) findPreference("enablePageKite");
		status = enabled.isChecked() ^ changing;

		if (status) {
			if (Service.mStatusTextMore != null) {
				enabled.setSummary(Service.mStatusText + "\n" + Service.mStatusTextMore);
			}
			else {
				enabled.setSummary(Service.mStatusText);
			}
		}
		else {
			enabled.setSummary(getText(R.string.pagekite_stopped));
		}
		
        enabled.setChecked(status);
        findPreference("prefsAccount").setEnabled(!status);
        findPreference("prefsLocalhost").setEnabled(!status);
        findPreference("showNotification").setEnabled(!status);
        findPreference("startOnBoot").setEnabled(!status);
		return enabled;
	}

	public void updateStringPreference(String pname, int
			                           textid, int defid, int hintid) {
		EditTextPreference p = (EditTextPreference) findPreference(pname);
		String text = p.getText();
		if ((text != null) && (text.length() > 0) && (text != getText(defid))) {
			p.setSummary(getText(textid) + "\n" + text);
		}
		else {
			p.setSummary(getText(textid) + "\n" + getText(hintid));
		}
	}
	

	public void updateIntegerPreference(String pname, int textid) {
		EditTextPreference p = (EditTextPreference) findPreference(pname);
		String text = p.getText();
		int value = (text != null) ? Integer.parseInt(text) : 0;
		if (value != 0) {
			p.setSummary(getText(textid) + " = " + value + ".");
		}
		else {
			p.setSummary(getText(textid) + ".");
		}
	}
	
    public static final int MENU_ABOUT = Menu.FIRST;
    public static final int MENU_VIEW_LOG = Menu.FIRST + 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.prefmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        	case R.id.help:
        		Intent hi = new Intent(this, HelpViewer.class);
        		hi.putExtra(HelpViewer.INTENT_HELP_PAGE, HelpViewer.HELP_ABOUT);
        		startActivity(hi);
        		return true;
        	case R.id.view_log:
        		Intent li = new Intent(this, LogViewer.class);
        		startActivity(li);
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
    	}
    }

}