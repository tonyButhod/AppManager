package buthod.tony.appManager.recipes;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;

import buthod.tony.appManager.R;
import buthod.tony.appManager.RootActivity;

public class WebRecipeActivity extends RootActivity {

    public static final String URL_EXTRA = "URL";

    private WebView mWebView = null;
    private EditText mUrlField = null;
    private ImageButton mGoToUrlButton = null;
    private String mCurrentUrl = "https://www.marmiton.org/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_recipe_activity);

        mWebView = (WebView) findViewById(R.id.web_view);
        mUrlField = (EditText) findViewById(R.id.url_field);
        mGoToUrlButton = (ImageButton) findViewById(R.id.go_to_url_button);

        // Finish the activity if the back button is pressed
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        // Load web page
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                mCurrentUrl = url;
                mUrlField.setText(mCurrentUrl);
                view.loadUrl(url);
                return true;
            }
        });
        mWebView.loadUrl(mCurrentUrl);
        findViewById(R.id.validate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra(URL_EXTRA, mCurrentUrl);
                finish(intent);
            }
        });
        // URL field
        mGoToUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebView.loadUrl(mUrlField.getText().toString());
            }
        });
    }

    /**
     * When the back key is pressed, go back in the web view if possible.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mWebView.canGoBack())
                        mWebView.goBack();
                    else
                        finish();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
