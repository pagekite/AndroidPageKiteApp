package net.pagekite.app;

import java.net.URI;
import java.util.HashMap;

import net.pagekite.lib.PageKiteAPI;

import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

	private final static int DIALOG_SIGNUP_YESNO = 1;
	private final static int DIALOG_SIGNUP_WARNING = 2;
	private final static int DIALOG_SIGNUP_DETAILS = 3;
	private final static int DIALOG_SIGNUP_WORKING = 4;
	private final static int DIALOG_SIGNUP_RESULT = 5;

	private final static String TAG = "PageKite.Preferences";

	private SharedPreferences mPrefs;
	private IntentFilter mListenFilter;
	private Integer mStatusCounter = 0;
	private String mSignupError = "";

	private Dialog mSignupDialog;
	private Dialog mProgressDialog;
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

		if (!kitesAreConfigured()) {
			showDialog(DIALOG_SIGNUP_YESNO);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateStatus(false);
    	if (mProgressDialog != null) mProgressDialog.dismiss();
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
                (!kiteName.toLowerCase().equals(getText(R.string.pagekite_default_kitename).toString().toLowerCase())));
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
    	return doMenuItem(item.getItemId());
    }

    public String getKiteURL() {
		String kiteName = mPrefs.getString("kiteName", null);
		String kiteURL = null;
		if (kiteName != null && Service.isRunning) {
			if (Integer.parseInt(mPrefs.getString("httpsPortNumber", "0")) > 0) {
				kiteURL = "https://" + kiteName;
			}
			else if (Integer.parseInt(mPrefs.getString("httpPortNumber", "0")) > 0) {
				if (kiteName.toLowerCase().endsWith(".pagekite.me")) {
					kiteURL = "https://" + kiteName;
				} else {
					kiteURL = "http://" + kiteName;
				}
			}
		}
    	return kiteURL;
    }
    
    public boolean doMenuItem(int itemId) {
    	String kiteURL;
    	Intent ntnt;
    	switch (itemId) {
    		case R.id.open_url:
    			kiteURL = getKiteURL();
    			if (kiteURL != null) {
    				startActivity(new Intent(Intent.ACTION_DEFAULT, Uri.parse(kiteURL)));
    			}
    			else {
					Toast.makeText(getBaseContext(),
							getText(R.string.need_flying_httpkite),
							Toast.LENGTH_LONG).show();
    			}
				return true;
    		case R.id.share_url:
    			kiteURL = getKiteURL();
    			if (kiteURL != null) {
     				ntnt = new Intent(Intent.ACTION_SEND);
     				ntnt.setType("text/plain");
            		ntnt.putExtra(Intent.EXTRA_SUBJECT, "My PageKite URL");
            		ntnt.putExtra(Intent.EXTRA_TEXT, kiteURL);
                	startActivity(Intent.createChooser(ntnt, getText(R.string.menu_share)));
    			}
    			else {
					Toast.makeText(getBaseContext(),
							getText(R.string.need_flying_httpkite),
							Toast.LENGTH_LONG).show();
    			}
    			return true;
        	case R.id.help:
        		ntnt = new Intent(this, HelpViewer.class);
        		ntnt.putExtra(HelpViewer.INTENT_HELP_PAGE, HelpViewer.HELP_ABOUT);
        		startActivity(ntnt);
        		return true;
        	case R.id.view_log:
        		startActivity(new Intent(this, LogViewer.class));
        		return true;
        	case R.id.signup:
        		if (kitesAreConfigured()) {
            		showDialog(DIALOG_SIGNUP_WARNING);        			
        		}
        		else {
            		showDialog(DIALOG_SIGNUP_DETAILS);        			
        		}
        		return true;
        	default:
        		return false;
    	}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_SIGNUP_YESNO:
            return new AlertDialog.Builder(this)
            	.setTitle("Welcome to PageKite!")
            	.setMessage("Before you can make your local servers visible to " +
            	            "the world, you need an account on a frontend relay " +
            			    "server.\n\n" +
            			    "Do you want to use pagekite.net as your relay?")
            	.setNegativeButton("Not now", null)
            	.setNeutralButton("Help?", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	doMenuItem(R.id.help);
                    }
            	})
            	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	showDialog(DIALOG_SIGNUP_DETAILS);
                    }
            	})
            	.create();
        case DIALOG_SIGNUP_WARNING:
            return new AlertDialog.Builder(this)
            	.setTitle("Warning")
            	.setMessage("This will replace your current account details.")
            	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
            		public void onClick(DialogInterface dialog, int whichButton) {
            			showDialog(DIALOG_SIGNUP_DETAILS);
            		}
            	})
            	.setNegativeButton("Cancel", null)
            	.create();
        case DIALOG_SIGNUP_DETAILS:
        	LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        	final View layout = inflater.inflate(R.layout.signupdialog,
        	                         (ViewGroup) findViewById(R.id.layout_signup));
        	mSignupDialog = new AlertDialog.Builder(this)
        		.setView(layout)
        		.setTitle("Sign up ...")
            	.setNegativeButton("Cancel", null)
            	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
            		public void onClick(DialogInterface dialog, int whichButton) {
						EditText e = (EditText) mSignupDialog.findViewById(R.id.signup_email);
						EditText k = (EditText) mSignupDialog.findViewById(R.id.signup_kitename);
						EditText p = (EditText) mSignupDialog.findViewById(R.id.signup_password);
						doSignup(e.getText().toString(),
								 k.getText().toString(),
								 p.getText().toString());
            		}
            	})
            	.create();
        	return mSignupDialog;
        case DIALOG_SIGNUP_WORKING:
        	mProgressDialog = ProgressDialog.show(this, "",
                    "Chatting with pagekite.net ...", true);
        	return mProgressDialog;
        case DIALOG_SIGNUP_RESULT:
            return new AlertDialog.Builder(this)
            	.setTitle("--")
            	.setMessage("--")
            	.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
            		public void onClick(DialogInterface dialog, int whichButton) {
						if (mSignupError != null) showDialog(DIALOG_SIGNUP_DETAILS);
            		}
            	})
            	.create();
        }
        return null;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	switch (id) {
    	case DIALOG_SIGNUP_RESULT:
        	String message = mSignupError;
        	if (message == null) {
        		String kiteName = ((EditTextPreference) findPreference("kiteName")).getText();
        		message = "We are ready to fly:  \n" + kiteName;
        	}
        	dialog.setTitle((mSignupError == null) ? "Success!" : "Failed :-(");
        	((AlertDialog) dialog).setMessage(message);
    	}
    }

    private String mSignupEmail = null;
    private String mSignupKiteName = null;
    private String mSignupPassword = null;
    private String mSignupSecret = null;
    public void doSignup(String email, String kiteName, String password) {
		Log.d(TAG, "STUPID: "+email+" "+kiteName);

    	if (email != null && kiteName != null &&
    	    email.length() > 0 && kiteName.length() > 0)
    	{
    		if (!kiteName.toLowerCase().endsWith(".pagekite.me")) {
    			kiteName += ".pagekite.me";
    		}
    		mSignupEmail = email;
    		mSignupKiteName = kiteName;
    		mSignupPassword = password;
	        new Thread(new Runnable() {
	            @Override
	            public void run() {
	            	mSignupError = null;

	            	URI uri = URI.create(PageKiteAPI.PAGEKITE_NET_XMLRPC);
	          	    XMLRPCClient client = new XMLRPCClient(uri);
          	    	Object[] result = null;
	          	    try {
	          	    	if (mSignupPassword != null && mSignupPassword.length() > 0) {
	          	    		result = (Object []) client.call("add_kite",
          	    				mSignupEmail, mSignupPassword, mSignupKiteName,
          	    				false); // Disable CNAMEs for now
	          	    	}
	          	    	else {
	          	    		result = (Object []) client.call("create_account",
          	    				"", "",  // Null credentials
          	    				mSignupEmail, mSignupKiteName,
          	    				true,    // Accept terms
          	    			 	true,    // Send e-mail
          	    				false);  // Activate
	          	    	}
					} catch (XMLRPCException e1) {
						e1.printStackTrace();
					}
	          	    if (result == null) {
		            	mSignupError = "Failed to communicate with server, try again later?";
	          	    }
	          	    else {
	          	    	String rcode = result[0].toString();
	          	    	if (rcode.equals("ok")) {
	          	    		@SuppressWarnings("unchecked")
							HashMap<String, String> rmap = (HashMap<String, String>) result[1];
	          	    		mSignupSecret = rmap.get("secret").toString();
	                		Editor editor = mPrefs.edit();
	                		editor.putString("kiteName", mSignupKiteName);
	                		editor.putString("kiteSecret", mSignupSecret);
	                		editor.commit();
	                		Log.d(TAG, "Got secret: " + mSignupSecret);
	          	    	}
	          	    	else {
		          	    	mSignupError = result[1].toString() + ". ("+rcode+")";
	          	    	}
	          	    }

	                mHandler.post(new Runnable() {
	                    @Override
	                    public void run() { 
	                    	mProgressDialog.dismiss();
	                    	if (mSignupError == null && mSignupSecret != null) {
		                		EditTextPreference p;
		                		p = (EditTextPreference) findPreference("kiteName");
		                		p.setText(mSignupKiteName);
		                		p = (EditTextPreference) findPreference("kiteSecret");
		                		p.setText(mSignupSecret);
		                		updateSummaries();
	                    	}
	                    	showDialog(DIALOG_SIGNUP_RESULT);
	                    }
	                });
	            }
	        }).start();
        	showDialog(DIALOG_SIGNUP_WORKING);
    	}
    	else {
    		mSignupError = "Please provide both an e-mail address and a kite name.";
        	showDialog(DIALOG_SIGNUP_RESULT);
    	}
    }
}