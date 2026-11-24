package com.jvchannel48.hotspotcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String API = "https://cine-ko-api.onrender.com/api/tmdb/3";
    private static final String IMG = "https://image.tmdb.org/t/p/w500";
    private static final int BG = Color.rgb(10, 12, 19), PANEL = Color.rgb(25, 29, 41), WHITE = Color.WHITE, MUTED = Color.rgb(166, 174, 191), ACCENT = Color.rgb(229, 31, 67);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private LinearLayout root, content, header;
    private String tab = "Home", currentType = "movie", query = "";
    private JSONArray currentEpisodes = new JSONArray();
    private int episodeIndex = -1;
    private JSONObject currentShow;
    private TextView status;

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void space(LinearLayout p, int h) { View v = new View(this); p.addView(v, new LinearLayout.LayoutParams(1, dp(h))); }
    private String apiKey() { return "server"; }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("cineko", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        showShell();
        loadHome();
    }

    private void showShell() {
        root = column(); root.setBackgroundColor(BG);
        header = column(); header.setPadding(dp(18), dp(16), dp(18), dp(8));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("CINE", 25, WHITE, true); TextView ko = label("-KO", 25, ACCENT, true);
        top.addView(brand); top.addView(ko); View spacer = new View(this); top.addView(spacer, new LinearLayout.LayoutParams(0,1,1));
        TextView settings = label("⚙", 24, WHITE, true); settings.setPadding(dp(12),0,dp(4),0); settings.setOnClickListener(v -> showApiKeyDialog()); top.addView(settings);
        header.addView(top);
        EditText search = new EditText(this); search.setSingleLine(true); search.setTextColor(WHITE); search.setHintTextColor(MUTED);
        search.setHint("Search movies and series"); search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search,0,0,0);
        search.setPadding(dp(14),0,dp(14),0); search.setBackground(bg(PANEL,14)); search.setTextSize(15);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,dp(48)); sp.topMargin=dp(14); header.addView(search,sp);
        search.setOnEditorActionListener((v, action, event) -> { String q=search.getText().toString().trim(); if(!q.isEmpty()){ query=q; tab="Search"; showTabContent(); searchCatalog(q); } return true; });
        root.addView(header);
        FrameLayout frame = new FrameLayout(this); content = column(); ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(content); frame.addView(scroll);
        root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setBackgroundColor(Color.rgb(17,19,28)); nav.setPadding(dp(4),dp(8),dp(4),dp(8));
        String[] tabs={"Home","Movies","Series","My List"};
        for(String name:tabs){ TextView b=label(name,12,name.equals(tab)?ACCENT:MUTED,true); b.setGravity(Gravity.CENTER); b.setPadding(dp(8),dp(8),dp(8),dp(8)); nav.addView(b,new LinearLayout.LayoutParams(0,dp(42),1)); b.setOnClickListener(v->{tab=name;showShellContent();}); }
        root.addView(nav); setContentView(root);
    }

    private void showShellContent(){ showTabContent(); if(tab.equals("Home"))loadHome(); else if(tab.equals("Movies"))loadList("movie","/movie/popular"); else if(tab.equals("Series"))loadList("tv","/tv/popular"); else loadSaved(); }
    private void showTabContent(){ content.removeAllViews(); content.setPadding(dp(16),dp(10),dp(16),dp(24)); }
    private void heading(String s){ TextView t=label(s,20,WHITE,true); t.setPadding(0,dp(14),0,dp(10)); content.addView(t); }
    private void message(String s){ TextView t=label(s,14,MUTED,false); t.setPadding(0,dp(12),0,dp(12)); content.addView(t); }
    private void loadHome(){ showTabContent(); heading("Welcome to CINE-KO"); message("Discover movies and series worth watching."); loadSection("Trending Movies","/trending/movie/week","movie"); loadSection("Popular Series","/tv/popular","tv"); }
    private void loadList(String type,String path){ showTabContent(); heading(type.equals("movie")?"Popular Movies":"Popular Series"); fetch(path, json -> renderGrid(json.optJSONArray("results"),type)); }
    private void loadSection(String title,String path,String type){
        heading(title); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false); hs.addView(row); content.addView(hs);
        fetch(path,json->{ JSONArray arr=json.optJSONArray("results"); if(arr==null)return; for(int i=0;i<Math.min(arr.length(),15);i++){ JSONObject item=arr.optJSONObject(i); if(item!=null) row.addView(poster(item,type,dp(124),dp(190))); }});
    }
    private View poster(JSONObject item,String type,int width,int height){
        LinearLayout card=column(); card.setPadding(dp(5),dp(4),dp(5),dp(8)); card.setLayoutParams(new LinearLayout.LayoutParams(width,-2));
        ImageView image=new ImageView(this); image.setBackground(bg(PANEL,10)); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String path=item.optString("poster_path",""); if(!path.equals("null")&&!path.isEmpty()) loadImage(IMG+path,image);
        card.addView(image,new LinearLayout.LayoutParams(-1,height));
        String title=item.optString("title",item.optString("name","Untitled")); TextView name=label(title,13,WHITE,true); name.setMaxLines(2); name.setPadding(0,dp(6),0,0); card.addView(name);
        card.setOnClickListener(v->openDetails(item,type)); return card;
    }
    private void renderGrid(JSONArray arr,String type){ if(arr==null){message("No results found.");return;} LinearLayout row=null; for(int i=0;i<arr.length();i++){ if(i%2==0){row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); content.addView(row);} JSONObject o=arr.optJSONObject(i); if(o!=null){row.addView(poster(o,type,dp(150),dp(218)),new LinearLayout.LayoutParams(0,-2,1));} } }
    private void searchCatalog(String q){ showTabContent(); heading("Search results"); fetch("/search/multi?query="+enc(q),json->{ JSONArray a=json.optJSONArray("results"); JSONArray filtered=new JSONArray(); if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i); if(o!=null){String type=o.optString("media_type"); if(type.equals("movie")||type.equals("tv"))filtered.put(o);}} renderMixed(filtered); }); }
    private void renderMixed(JSONArray arr){ if(arr==null||arr.length()==0){message("Walang nahanap. Subukan ang ibang title.");return;} for(int i=0;i<arr.length();i++){JSONObject o=arr.optJSONObject(i);if(o!=null)content.addView(poster(o,o.optString("media_type"),dp(145),dp(210)));} }
    private void loadSaved(){ showTabContent(); heading("My List"); JSONArray saved=new JSONArray(); try{saved=new JSONArray(prefs.getString("saved","[]"));}catch(Exception ignored){} renderMixed(saved); }
    private void openDetails(JSONObject item,String type){
        currentType=type; currentShow=item;
        String id=item.optString("id"); String path="/"+type+"/"+id+"?append_to_response=videos";
        fetch(path,detail->{ showTabContent(); String title=detail.optString("title",detail.optString("name","Details")); heading(title);
            TextView overview=label(detail.optString("overview","No description available."),14,MUTED,false); content.addView(overview);
            LinearLayout actions=new LinearLayout(this); Button play=button("▶ Play Trailer"); actions.addView(play,new LinearLayout.LayoutParams(0,dp(48),1)); play.setOnClickListener(v->playTrailer(detail));
            Button save=button("＋ My List"); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1); bp.leftMargin=dp(8); actions.addView(save,bp); save.setOnClickListener(v->saveItem(item));
            content.addView(actions); Button stream=button("Play authorized video URL"); LinearLayout.LayoutParams streamP=new LinearLayout.LayoutParams(-1,dp(46)); streamP.topMargin=dp(8); content.addView(stream,streamP); stream.setOnClickListener(v->promptVideoUrl(title)); space(content,12);
            if(type.equals("tv")){ heading("Seasons & Episodes"); JSONArray seasons=detail.optJSONArray("seasons"); if(seasons!=null)for(int i=0;i<seasons.length();i++){JSONObject s=seasons.optJSONObject(i);if(s!=null&&s.optInt("season_number")>0){Button sb=button("Season "+s.optInt("season_number")+"  ·  "+s.optInt("episode_count")+" episodes"); content.addView(sb); sb.setOnClickListener(v->loadSeason(id,s.optInt("season_number")));}}}
            else { JSONArray vids=detail.optJSONObject("videos")==null?null:detail.optJSONObject("videos").optJSONArray("results"); if(vids!=null&&vids.length()>0) message("Available official trailers can be played above."); }
        });
    }
    private void loadSeason(String showId,int season){ fetch("/tv/"+showId+"/season/"+season,json->{ currentEpisodes=json.optJSONArray("episodes"); episodeIndex=-1; showTabContent(); heading("Season "+season); if(currentEpisodes==null)return; for(int i=0;i<currentEpisodes.length();i++){final int index=i;JSONObject ep=currentEpisodes.optJSONObject(i);if(ep==null)continue;Button b=button("Episode "+ep.optInt("episode_number")+"  ·  "+ep.optString("name")); content.addView(b); b.setOnClickListener(v->{episodeIndex=index;new AlertDialog.Builder(this).setTitle("Episode "+ep.optInt("episode_number")).setItems(new String[]{"Official preview","Play authorized video URL"},(d,which)->{if(which==0)playEpisode(ep);else promptVideoUrl("S"+ep.optInt("season_number")+" E"+ep.optInt("episode_number")+" · "+ep.optString("name"));}).show();});} }); }
    private void playEpisode(JSONObject ep){ String title="S"+ep.optInt("season_number")+" E"+ep.optInt("episode_number")+" · "+ep.optString("name"); fetch("/tv/"+currentShow.optString("id")+"/season/"+ep.optInt("season_number")+"/episode/"+ep.optInt("episode_number")+"?append_to_response=videos",detail->{playVideo(detail,title);}); }
    private void playTrailer(JSONObject detail){ JSONObject videos=detail.optJSONObject("videos"); JSONArray results=videos==null?null:videos.optJSONArray("results"); if(results!=null)for(int i=0;i<results.length();i++){JSONObject v=results.optJSONObject(i);if(v!=null&&v.optString("site").equalsIgnoreCase("YouTube")&&(v.optString("type").equals("Trailer")||v.optString("type").equals("Teaser"))){playYoutube(v.optString("key"),detail.optString("title",detail.optString("name","Trailer")));return;}} toast("Walang official YouTube trailer na available."); }
    private void playVideo(JSONObject detail,String title){ JSONObject videos=detail.optJSONObject("videos");JSONArray a=videos==null?null:videos.optJSONArray("results");if(a!=null)for(int i=0;i<a.length();i++){JSONObject v=a.optJSONObject(i);if(v!=null&&v.optString("site").equalsIgnoreCase("YouTube")&&v.optString("type").equals("Trailer")){playYoutube(v.optString("key"),title);return;}} toast("Walang official episode preview/video source. Puwede ang next episode list."); }
    private void promptVideoUrl(String title){
        EditText input=new EditText(this); input.setSingleLine(true); input.setTextColor(WHITE); input.setHintTextColor(MUTED); input.setHint("https://…"); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        new AlertDialog.Builder(this).setTitle("Play video").setMessage("I-paste lamang ang video URL na pag-aari mo o may pahintulot kang i-stream.").setView(input).setPositiveButton("Play",(d,w)->{String url=input.getText().toString().trim();if(url.startsWith("https://")||url.startsWith("http://"))playDirectUrl(url,title);else toast("Maglagay ng valid http/https video URL.");}).setNegativeButton("Cancel",null).show();
    }
    private void playDirectUrl(String url,String title){
        showTabContent(); heading(title);
        VideoView video=new VideoView(this); video.setBackgroundColor(Color.BLACK);
        MediaController controls=new MediaController(this); controls.setAnchorView(video); video.setMediaController(controls);
        content.addView(video,new LinearLayout.LayoutParams(-1,dp(230)));
        video.setVideoURI(android.net.Uri.parse(url)); video.setOnPreparedListener(mp->{mp.setLooping(false);video.start();});
        video.setOnErrorListener((mp,what,extra)->{toast("Hindi mabuksan ang video. Check format, URL, at stream permissions.");return true;});
        if(episodeIndex>=0){LinearLayout row=new LinearLayout(this);Button prev=button("Previous");Button next=button("Next Episode");row.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(row);prev.setOnClickListener(v->playAdjacent(-1));next.setOnClickListener(v->playAdjacent(1));}
    }
    private void playYoutube(String key,String title){
        showTabContent(); heading(title); WebView web=new WebView(this); web.setBackgroundColor(Color.BLACK); web.getSettings().setJavaScriptEnabled(true); web.getSettings().setMediaPlaybackRequiresUserGesture(false); web.setWebChromeClient(new WebChromeClient()); web.setWebViewClient(new WebViewClient());
        String html="<!doctype html><html><body style='margin:0;background:#000'><iframe width='100%' height='100%' src='https://www.youtube-nocookie.com/embed/"+key+"?autoplay=1&playsinline=1' frameborder='0' allow='autoplay; encrypted-media; picture-in-picture' allowfullscreen></iframe></body></html>";
        content.addView(web,new LinearLayout.LayoutParams(-1,dp(230))); web.loadDataWithBaseURL("https://www.youtube-nocookie.com",html,"text/html","UTF-8",null);
        if(episodeIndex>=0){LinearLayout controls=new LinearLayout(this);Button prev=button("Previous");Button next=button("Next Episode");controls.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1));controls.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(controls);prev.setOnClickListener(v->playAdjacent(-1));next.setOnClickListener(v->playAdjacent(1));}
        message("Official trailer/preview playback. Full movies and episodes require a licensed streaming source.");
    }
    private void playAdjacent(int delta){int n=episodeIndex+delta;if(n<0||n>=currentEpisodes.length()){toast("Wala nang episode sa direksiyong iyon.");return;}episodeIndex=n;JSONObject ep=currentEpisodes.optJSONObject(n);if(ep!=null)playEpisode(ep);}
    private void saveItem(JSONObject item){try{JSONArray a=new JSONArray(prefs.getString("saved","[]"));boolean exists=false;for(int i=0;i<a.length();i++)if(a.optJSONObject(i)!=null&&a.optJSONObject(i).optString("id").equals(item.optString("id"))){exists=true;break;}if(!exists)a.put(item);prefs.edit().putString("saved",a.toString()).apply();toast(exists?"Nasa My List na ito.":"Added to My List.");}catch(Exception e){toast("Hindi na-save.");}}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setTextColor(WHITE);b.setAllCaps(false);b.setTextSize(13);b.setBackground(bg(ACCENT,12));return b;}
    private void showApiKeyDialog(){new AlertDialog.Builder(this).setTitle("CINE-KO Server").setMessage("Connected API backend: https://cine-ko-api.onrender.com\n\nThe TMDB token is stored privately in the Render server environment. If the catalog is unavailable, check that TMDB_API_TOKEN is set in Render → Environment.").setPositiveButton("OK",null).show();}
    private interface JsonCallback{void done(JSONObject json);}
    private void fetch(String path,JsonCallback callback){ worker.execute(()->{JSONObject result=null;try{URL url=new URL(API+path);HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod("GET");c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("accept","application/json");InputStream in=(c.getResponseCode()<400?c.getInputStream():c.getErrorStream());ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);String body=out.toString("UTF-8");if(c.getResponseCode()<400)result=new JSONObject(body);else{String err=new JSONObject(body).optString("status_message","API request failed");final String msg=err;runOnUiThread(()->toast(msg));}in.close();c.disconnect();}catch(Exception e){runOnUiThread(()->toast("Network/API error. Check CINE-KO server and TMDB setup."));}final JSONObject data=result;if(data!=null)runOnUiThread(()->callback.done(data));});}
    private void loadImage(String url,ImageView view){worker.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(10000);android.graphics.Bitmap bitmap=android.graphics.BitmapFactory.decodeStream(c.getInputStream());c.disconnect();if(bitmap!=null)runOnUiThread(()->view.setImageBitmap(bitmap));}catch(Exception ignored){}});}
    private String enc(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){return s;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
