package com.livetgtv.embedtv;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private LinearLayout home;
    private GridLayout grid;
    private EditText search;
    private TextView status;
    private ProgressBar loading;
    private WebView player;
    private final Handler handler = new Handler();

    private final List<Channel> all = new ArrayList<>();
    private final List<Channel> visible = new ArrayList<>();
    private String mode = "v1";

    private static final String V1_LIST =
            "https://livetgtv.lovable.app/api/public/channels";
    private static final String V2_LIST =
            "https://livetgtv.lovable.app/api/public/v2/channels";

    static class Channel {
        String id, name, category, logo, embed;
        Channel(String id, String name, String category, String logo, String embed) {
            this.id=id; this.name=name; this.category=category; this.logo=logo; this.embed=embed;
        }
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        home=findViewById(R.id.home);
        grid=findViewById(R.id.grid);
        search=findViewById(R.id.search);
        status=findViewById(R.id.status);
        loading=findViewById(R.id.loading);
        player=findViewById(R.id.player);

        configurePlayer();

        findViewById(R.id.v1Button).setOnClickListener(v -> loadCatalogue("v1"));
        findViewById(R.id.v2Button).setOnClickListener(v -> loadCatalogue("v2"));
        findViewById(R.id.reloadButton).setOnClickListener(v -> loadCatalogue(mode));

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int b,int c){ filter(s.toString()); }
            public void afterTextChanged(Editable e){}
        });

        loadCatalogue("v1");
    }

    private void configurePlayer() {
        WebSettings s=player.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 9; TV) AppleWebKit/537.36 Chrome/120 Safari/537.36");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(player,true);

        player.setWebViewClient(new WebViewClient());
        player.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v,int p) {
                if(p>=90) loading.setVisibility(View.GONE);
            }
        });
        player.setBackgroundColor(Color.BLACK);
    }

    private void loadCatalogue(final String which) {
        mode=which;
        loading.setVisibility(View.VISIBLE);
        status.setText("Loading "+which.toUpperCase()+" channels...");
        grid.removeAllViews();
        all.clear();
        visible.clear();

        final String endpoint = which.equals("v2") ? V2_LIST : V1_LIST;

        new Thread(() -> {
            try {
                HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
                c.setConnectTimeout(12000);
                c.setReadTimeout(15000);
                c.setRequestMethod("GET");
                c.setRequestProperty("Accept","application/json");
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb=new StringBuilder(); String line;
                while((line=r.readLine())!=null) sb.append(line);
                r.close();
                JSONArray arr=new JSONObject(sb.toString()).optJSONArray("channels");
                if(arr==null) throw new Exception("No channels array");

                for(int i=0;i<arr.length();i++) {
                    JSONObject o=arr.getJSONObject(i);
                    String id=o.optString("id");
                    String name=o.optString("name","Channel "+id);
                    String cat=o.optString("category","");
                    String logo=o.optString("logo","");
                    String embed;
                    if(which.equals("v2")) {
                        embed="https://livetgtv.lovable.app/v2/embed/"+id;
                    } else {
                        embed="https://livetgtv.lovable.app/embed/"+id;
                    }
                    all.add(new Channel(id,name,cat,logo,embed));
                }

                handler.post(() -> {
                    loading.setVisibility(View.GONE);
                    status.setText(all.size()+" channels • "+mode.toUpperCase());
                    filter("");
                });
            } catch(Exception e) {
                handler.post(() -> {
                    loading.setVisibility(View.GONE);
                    status.setText("Could not load "+mode.toUpperCase()+" catalogue: "+e.getMessage());
                });
            }
        }).start();
    }

    private void filter(String q) {
        visible.clear();
        String x=q.toLowerCase(Locale.US).trim();
        for(Channel ch:all) {
            if(x.isEmpty() || ch.name.toLowerCase(Locale.US).contains(x)
                    || ch.category.toLowerCase(Locale.US).contains(x)) visible.add(ch);
        }
        render();
    }

    private void render() {
        grid.removeAllViews();
        for(final Channel ch:visible) {
            Button b=new Button(this);
            b.setText(ch.name + (ch.category.isEmpty() ? "" : "\n"+ch.category));
            b.setTextColor(Color.WHITE);
            b.setTextSize(13);
            b.setAllCaps(false);
            b.setFocusable(true);
            b.setMinHeight(82);
            b.setOnClickListener(v -> openChannel(ch));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
            lp.width=0;
            lp.height=90;
            lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            lp.setMargins(6,6,6,6);
            b.setLayoutParams(lp);
            grid.addView(b);
        }
    }

    private void openChannel(Channel ch) {
        home.setVisibility(View.GONE);
        player.setVisibility(View.VISIBLE);
        loading.setVisibility(View.VISIBLE);
        player.loadUrl(ch.embed);
    }

    private void closePlayer() {
        player.stopLoading();
        player.loadUrl("about:blank");
        player.setVisibility(View.GONE);
        home.setVisibility(View.VISIBLE);
        loading.setVisibility(View.GONE);
    }

    @Override public void onBackPressed() {
        if(player.getVisibility()==View.VISIBLE) {
            closePlayer();
        } else {
            super.onBackPressed();
        }
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if(e.getAction()==KeyEvent.ACTION_UP && e.getKeyCode()==KeyEvent.KEYCODE_BACK
                && player.getVisibility()==View.VISIBLE) {
            closePlayer();
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override protected void onDestroy() {
        if(player!=null) player.destroy();
        super.onDestroy();
    }
}
