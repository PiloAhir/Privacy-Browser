package com.surakshit.browser;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.ByteArrayInputStream;
import java.util.*;

public class MainActivity extends Activity {
    private static final String PREF="private_browser", HIST="history", AUTO="auto";
    private SharedPreferences prefs;
    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private boolean autoClear=true, clearing=false;
    private final ArrayList<Visit> history=new ArrayList<>();
    static class Visit {
        long time; String url,title;
        Visit(long t,String u,String ti){time=t;url=u;title=ti;}
        JSONObject toJson() throws Exception { JSONObject o=new JSONObject(); o.put("time",time);o.put("url",url);o.put("title",title);return o; }
    }
    int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(12));return d;}
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);return b;}

    @Override public void onCreate(Bundle state){
        super.onCreate(state); prefs=getSharedPreferences(PREF,MODE_PRIVATE);autoClear=prefs.getBoolean(AUTO,true);loadHistory();makeUi();setupWeb();
        navigate(state!=null&&state.getString("url")!=null?state.getString("url"):"https://duckduckgo.com/");
    }
    void makeUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(246,249,252));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(10),dp(5),dp(8),dp(3));
        TextView logo=label("◈ સુરક્ષિત બ્રાઉઝર",18,Color.rgb(20,55,85));logo.setTypeface(null,Typeface.BOLD);top.addView(logo,new LinearLayout.LayoutParams(0,dp(44),1));
        Button settings=btn("⚙ સેટિંગ્સ");settings.setOnClickListener(v->settings());top.addView(settings);root.addView(top);
        LinearLayout row=new LinearLayout(this);row.setPadding(dp(8),dp(3),dp(8),dp(5));
        address=new EditText(this);address.setSingleLine(true);address.setTextSize(14);address.setHint("વેબસાઇટ અથવા શોધ");address.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);address.setImeOptions(EditorInfo.IME_ACTION_GO);address.setPadding(dp(10),0,dp(10),0);address.setBackground(bg(Color.WHITE));
        address.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_GO||a==EditorInfo.IME_ACTION_DONE){navigate(address.getText().toString());((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);return true;}return false;});
        row.addView(address,new LinearLayout.LayoutParams(0,dp(46),1));Button go=btn("જાઓ");go.setOnClickListener(v->navigate(address.getText().toString()));row.addView(go,new LinearLayout.LayoutParams(dp(62),dp(46)));root.addView(row);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=label("ટ્રેકર બ્લોકલિસ્ટ • સ્થાનિક ડેટા",11,Color.rgb(70,90,105));footer.setGravity(Gravity.CENTER);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(25)));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(dp(3),0,dp(3),dp(4));
        Button back=btn("‹ પાછા"),forward=btn("આગળ ›"),reload=btn("⟳"),clear=btn("🗑 ડેટા");
        back.setOnClickListener(v->{if(web.canGoBack())web.goBack();});forward.setOnClickListener(v->{if(web.canGoForward())web.goForward();});reload.setOnClickListener(v->web.reload());clear.setOnClickListener(v->clearDialog());
        nav.addView(back,new LinearLayout.LayoutParams(0,dp(43),1));nav.addView(forward,new LinearLayout.LayoutParams(0,dp(43),1));nav.addView(reload,new LinearLayout.LayoutParams(0,dp(43),.7f));nav.addView(clear,new LinearLayout.LayoutParams(0,dp(43),1));root.addView(nav);setContentView(root);
    }
    void setupWeb(){
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setDatabaseEnabled(false);s.setGeolocationEnabled(false);s.setSaveFormData(false);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setSupportMultipleWindows(false);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebChromeClient(new WebChromeClient(){@Override public void onProgressChanged(WebView v,int p){progress.setProgress(p);progress.setVisibility(p>=100?View.GONE:View.VISIBLE);}});
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(u.startsWith("https://")||u.startsWith("http://")){address.setText(u);return false;}return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){if(tracker(r.getUrl().getHost()))return new WebResourceResponse("text/plain","utf-8",new ByteArrayInputStream(new byte[0]));return super.shouldInterceptRequest(v,r);}
            @Override public void onPageStarted(WebView v,String u,android.graphics.Bitmap f){address.setText(u);}
            @Override public void onPageFinished(WebView v,String u){address.setText(u);addVisit(u,v.getTitle());}
        });
    }
    boolean tracker(String host){
        if(host==null)return false;String h=host.toLowerCase(Locale.ROOT);
        String[] domains={"google-analytics.com","googletagmanager.com","doubleclick.net","connect.facebook.net","hotjar.com","segment.io","segment.com","mixpanel.com","amplitude.com","clarity.ms","bat.bing.com","analytics.tiktok.com","scorecardresearch.com","adservice.google.com","adnxs.com","criteo.com","taboola.com","outbrain.com","quantserve.com"};
        for(String d:domains)if(h.equals(d)||h.endsWith("."+d))return true;return false;
    }
    void navigate(String raw){
        if(raw==null||raw.trim().isEmpty())return;String x=raw.trim(),u;
        if(x.matches("(?i)^https?://.*"))u=x;
        else if(x.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(/.*)?$")&&!x.contains(" "))u="https://"+x;
        else u="https://duckduckgo.com/?q="+android.net.Uri.encode(x);
        address.setText(u);web.loadUrl(u);
    }
    void addVisit(String u,String t){
        if(u==null||(!u.startsWith("http://")&&!u.startsWith("https://")))return;
        if(!history.isEmpty()){Visit z=history.get(history.size()-1);if(z.url.equals(u)&&System.currentTimeMillis()-z.time<2500)return;}
        history.add(new Visit(System.currentTimeMillis(),u,t==null?"":t));while(history.size()>2000)history.remove(0);saveHistory();
    }
    void loadHistory(){try{JSONArray a=new JSONArray(prefs.getString(HIST,"[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);history.add(new Visit(o.optLong("time"),o.optString("url"),o.optString("title")));}}catch(Exception ignored){}}
    void saveHistory(){JSONArray a=new JSONArray();try{for(Visit v:history)a.put(v.toJson());prefs.edit().putString(HIST,a.toString()).apply();}catch(Exception ignored){}}
    void clearDialog(){
        String[] choices={"છેલ્લા 24 કલાક","છેલ્લું 1 અઠવાડિયું","છેલ્લો 1 મહિનો","છેલ્લું 1 વર્ષ","સંપૂર્ણ ડેટા"};
        new AlertDialog.Builder(this).setTitle("બ્રાઉઝિંગ ડેટા ડિલીટ કરો")
        .setMessage("સમયગાળાની પસંદગીથી ફક્ત સ્થાનિક ઇતિહાસ તે સમયગાળા માટે કાઢી શકાય છે. કૂકીઝ, કૅશ અને વેબસાઇટ સ્ટોરેજ માટે સંપૂર્ણ ડેટા સાફ કરો.")
        .setItems(choices,(d,n)->new AlertDialog.Builder(this).setTitle("કાયમી ડિલીટની પુષ્ટિ")
        .setMessage(n==4?"ઇતિહાસ, કૂકીઝ, કૅશ અને વેબસાઇટ લોકલ સ્ટોરેજ ડિલીટ કરવું છે?":"પસંદ કરેલા સમયગાળાનો સ્થાનિક ઇતિહાસ કાયમી રીતે કાઢવો છે? કૂકીઝ/કૅશ તે સમયગાળા માટે અલગથી કાઢી શકાતાં નથી.")
        .setNegativeButton("રદ કરો",null).setPositiveButton("ડિલીટ", (dd,w)->{if(n==4)clearAll(true);else clearPeriod(n);}).show()).show();
    }
    void clearPeriod(int n){
        long span=n==0?86400000L:n==1?7L*86400000:n==2?30L*86400000:n==3?365L*86400000:Long.MAX_VALUE;
        if(n==4)history.clear();else{long cutoff=System.currentTimeMillis()-span;for(int i=history.size()-1;i>=0;i--)if(history.get(i).time>=cutoff)history.remove(i);}
        saveHistory();Toast.makeText(this,"પસંદ કરેલા સમયગાળાનો ઇતિહાસ ડિલીટ થયો",Toast.LENGTH_LONG).show();
    }
    void clearAll(boolean toast){
        if(clearing)return;clearing=true;
        try{
            if(web!=null){web.stopLoading();web.clearHistory();web.clearFormData();web.clearCache(true);web.clearSslPreferences();web.loadUrl("about:blank");}
            CookieManager cm=CookieManager.getInstance();cm.removeAllCookies(ok->{cm.flush();WebStorage.getInstance().deleteAllData();history.clear();prefs.edit().remove(HIST).apply();clearing=false;if(toast&&!isFinishing())runOnUiThread(()->Toast.makeText(this,"બ્રાઉઝિંગ ડેટા સાફ કર્યો",Toast.LENGTH_SHORT).show());});
        }catch(Exception e){history.clear();prefs.edit().remove(HIST).apply();clearing=false;}
    }
    void settings(){
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(18),dp(8),dp(18),dp(8));box.setOrientation(1);
        Switch sw=new Switch(this);sw.setText("એપ બેકગ્રાઉન્ડ/બંધ થાય ત્યારે ડેટા સાફ કરો");sw.setTextSize(15);sw.setChecked(autoClear);box.addView(sw);
        TextView a=label("ચાલુ હોય ત્યારે એપ બેકગ્રાઉન્ડમાં જાય ત્યારે પણ ડેટા સાફ થઈ શકે છે. Android force-stop માટે callbackની ગેરંટી આપતું નથી.",13,Color.DKGRAY);a.setGravity(Gravity.TOP);a.setPadding(0,dp(10),0,dp(12));box.addView(a);
        TextView b=label("ટ્રેકર બ્લોકિંગ જાણીતા ડોમેનની યાદી સુધી મર્યાદિત છે. દરેક ટ્રેકર રોકવાની ગેરંટી નથી.",13,Color.DKGRAY);b.setGravity(Gravity.TOP);box.addView(b);
        TextView c=label("સમય પ્રમાણે ઇતિહાસ કાઢી શકાય છે; WebView કૂકીઝ/કૅશ માટે ચોક્કસ સમય-મુદ્રાંક આપતું નથી. તે માટે સંપૂર્ણ ડેટા સાફ કરો.",13,Color.DKGRAY);c.setGravity(Gravity.TOP);c.setPadding(0,dp(10),0,0);box.addView(c);
        new AlertDialog.Builder(this).setTitle("પ્રાઇવસી સેટિંગ્સ").setView(box).setNegativeButton("રદ",null).setPositiveButton("સેવ કરો",(d,w)->{autoClear=sw.isChecked();prefs.edit().putBoolean(AUTO,autoClear).apply();Toast.makeText(this,autoClear?"ઓટો-ક્લિયર ચાલુ":"ઓટો-ક્લિયર બંધ",Toast.LENGTH_SHORT).show();}).show();
    }
    @Override protected void onSaveInstanceState(Bundle b){if(web!=null)b.putString("url",web.getUrl());super.onSaveInstanceState(b);}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setTitle("બ્રાઉઝર બંધ કરવું?").setMessage(autoClear?"બંધ/બેકગ્રાઉન્ડ થતાં ડેટા સાફ થશે.":"ઓટો-ક્લિયર બંધ છે; ડેટા જળવાશે.").setNegativeButton("રહો",null).setPositiveButton("બંધ કરો",(d,w)->finish()).show();}
    @Override protected void onStop(){if(autoClear)clearAll(false);super.onStop();}
    @Override protected void onDestroy(){if(autoClear)clearAll(false);if(web!=null){try{((android.view.ViewGroup)web.getParent()).removeView(web);web.destroy();}catch(Exception ignored){}}super.onDestroy();}
}
