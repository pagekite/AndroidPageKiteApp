package net.pagekite.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpViewer extends Activity {

    public static final String INTENT_HELP_PAGE = "net.pagekite.app.helpPage";
    public static final String HELP_ABOUT = "about.html";

    private static final String SI_LAST_VIEW = "lastHelpView";
    private String mViewing;
    private WebView mWebView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.helpviewer);

        Intent i = getIntent();
        mViewing = i.getStringExtra(INTENT_HELP_PAGE);
        if (mViewing == null) mViewing = HELP_ABOUT;

        mWebView = (WebView) findViewById(R.id.helpWebView);
        mWebView.setSaveEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(false);

        if (savedInstanceState != null)
        	mViewing = savedInstanceState.getString(SI_LAST_VIEW);
            
        loadHelp(mViewing);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putString(SI_LAST_VIEW, mViewing);
    }
    
    private void loadHelp(String page) {
        if (page != null) mViewing = page;
        mWebView.loadUrl("file:///android_asset/help/"+mViewing);
    }
}