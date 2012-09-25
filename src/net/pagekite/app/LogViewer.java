package net.pagekite.app;

import net.pagekite.lib.PageKiteAPI;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class LogViewer extends Activity {

	private IntentFilter mListenFilter;
	private BroadcastReceiver mListener;
	private boolean mListening = false;
	private TextView mLogView;
	private ScrollView mLogScrollView;
	private Handler mHandler;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logviewer);

        mLogScrollView = (ScrollView) findViewById(R.id.logScrollView);
        mLogView = (TextView) findViewById(R.id.logView);
		mLogView.setText("");
        
		mHandler = new Handler();
        mListener = new BroadcastReceiver() {
			@Override
			public void onReceive(Context ctx, Intent event) {
				updateText();
			}
        };
        mListenFilter = new IntentFilter(Service.STATUS_UPDATE_INTENT);
        mListening = false;
	}

	private void scrollDown(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {Thread.sleep(100);} catch (InterruptedException e) {}
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLogScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        }).start();
    }

	void updateText() {
		mLogView.setText(PageKiteAPI.getLog());
		scrollDown();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	return doMenuItem(item.getItemId());
    }

    public boolean doMenuItem(int itemId) {
    	String kiteURL;
    	Intent ntnt;
    	switch (itemId) {
    		case R.id.menu_send_log:
 				ntnt = new Intent(Intent.ACTION_SENDTO);
 				ntnt.setData(Uri.parse("mailto:"+PageKiteAPI.PAGEKITE_NET_EMAIL));
        		ntnt.putExtra(Intent.EXTRA_SUBJECT, "PageKite App Log");
        		ntnt.putExtra(Intent.EXTRA_TEXT, PageKiteAPI.getLog());
            	startActivity(Intent.createChooser(ntnt, getText(R.string.menu_send_log)));
    			return true;
        	default:
        		return false;
    	}
    }

	@Override
	protected void onResume() {
		super.onResume();
        updateText();
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
}
