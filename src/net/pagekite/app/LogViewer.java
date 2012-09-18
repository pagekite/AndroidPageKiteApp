package net.pagekite.app;

import net.pagekite.lib.PageKiteAPI;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;

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
