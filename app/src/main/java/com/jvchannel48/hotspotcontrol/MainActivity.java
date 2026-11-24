package com.jvchannel48.hotspotcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.content.Intent;
import android.net.Uri;
import java.util.*;

public class MainActivity extends Activity {
  private final int BG=Color.rgb(9,12,18), PANEL=Color.rgb(22,29,40), CYAN=Color.rgb(40,210,220), WHITE=Color.WHITE, MUTED=0xffaab6c5;
  private LinearLayout root, list;
  private EditText search;
  private WebView web;
  private TextView status;
  static class CameraSource {
    String name, area, kind, url, note;
    CameraSource(String n,String a,String k,String u,String d){name=n;area=a;kind=k;url=u;note=d;}
  }
  private final ArrayList<CameraSource> cameras=new ArrayList<>();
  private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  private GradientDrawable bg(int c,int r,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));if(stroke>0)d.setStroke(dp(1),CYAN);return d;}
  private TextView txt(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(BG);b.setTypeface(null,Typeface.BOLD);b.setBackground(bg(CYAN,14,0));return b;}
  @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);seed();home();}
  private void seed(){
    cameras.add(new CameraSource("Philippines Public Camera Map","Pilipinas","PUBLIC CAMERA DIRECTORY","https://www.argosatlas.com/en/cameras/philippines/","Browse public camera points. Some show still frames, not continuous video."));
    cameras.add(new CameraSource("WorldCam Philippines","Pilipinas","WEBCAM DIRECTORY","https://worldcam.eu/","Publicly listed webcams. Choose a camera and check its live status on the provider page."));
    cameras.add(new CameraSource("Davao Area Street Cameras","Davao City","CAMERA INDEX","https://www.argosatlas.com/en/cameras/philippines/","Open the public Philippines camera map and browse Davao entries."));
    cameras.add(new CameraSource("El Nido Public Webcam","Palawan","SCENIC WEBCAM","https://www.argosatlas.com/en/cameras/philippines/","Look for El Nido public webcam entries; availability is controlled by the camera publisher."));
    cameras.add(new CameraSource("Philippines Traffic & Road Views","Pilipinas","ROAD CAMERA DIRECTORY","https://lagayngdaan.com/fil/mga-kamera/","External public camera and road-source directory. Camera status must be checked at the original site."));
    cameras.add(new CameraSource("MMDA Traffic Information","Metro Manila","OFFICIAL TRAFFIC UPDATES","https://mmda.gov.ph/","Official MMDA information and advisories. Direct MMDA CCTV streams are not offered here."));
    cameras.add(new CameraSource("Pasig City DRRMO","Pasig City","OFFICIAL CITY CCTV INFO","https://pasigcity.gov.ph/services","City CCTV is monitored by DRRMO; public access is by request for incidents, not an open live stream."));
    cameras.add(new CameraSource("Muntinlupa CCTV Footage Request","Muntinlupa","OFFICIAL CITY CCTV INFO","https://ddrm.muntinlupacity.gov.ph/cctv-footage-request/","Official process to request CCTV footage for legitimate purposes."));
  }
  private void home(){
    root=col();root.setBackgroundColor(BG);
    LinearLayout header=col();header.setPadding(dp(18),dp(22),dp(18),dp(18));header.setBackground(bg(PANEL,0,0));
    TextView brand=txt("RJ PUBLIC CCTV",25,WHITE,true);header.addView(brand);
    TextView sub=txt("PUBLIC CAMERAS • TRAFFIC • CITY WEBCAMS",11,CYAN,true);sub.setPadding(0,dp(6),0,dp(10));header.addView(sub);
    TextView info=txt("Pumili ng camera source para buksan ang video o live view sa mismong publisher.",14,MUTED,false);header.addView(info);
    root.addView(header);
    search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);search.setHintTextColor(MUTED);search.setHint("Search camera, city o area");search.setTextSize(15);search.setPadding(dp(16),0,dp(12),0);search.setBackground(bg(PANEL,15,0));
    LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.setMargins(dp(14),dp(14),dp(14),dp(8));root.addView(search,sp);
    status=txt("PUBLIC SOURCES • EXTERNAL VIDEO",11,CYAN,true);status.setPadding(dp(18),dp(8),dp(18),dp(8));root.addView(status);
    ScrollView scroll=new ScrollView(this);list=col();list.setPadding(dp(12),0,dp(12),dp(20));scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render(s.toString());}public void afterTextChanged(android.text.Editable e){}});
    render("");
  }
  private void render(String q){
    list.removeAllViews();String n=q.toLowerCase(Locale.ROOT).trim();int shown=0;
    for(CameraSource c:cameras){if(!(c.name+" "+c.area+" "+c.kind+" "+c.note).toLowerCase(Locale.ROOT).contains(n))continue;
      LinearLayout card=col();card.setPadding(dp(15),dp(14),dp(15),dp(14));card.setBackground(bg(PANEL,17,0));
      LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(dp(2),dp(5),dp(2),dp(7));list.addView(card,cp);
      TextView tag=txt(c.kind,10,CYAN,true);card.addView(tag);
      TextView title=txt(c.name,18,WHITE,true);title.setPadding(0,dp(5),0,dp(3));card.addView(title);
      TextView area=txt("⌖  "+c.area,12,0xffc6d1df,true);card.addView(area);
      TextView note=txt(c.note,13,MUTED,false);note.setPadding(0,dp(8),0,dp(10));card.addView(note);
      Button open=btn("OPEN CAMERA / VIEW  →");card.addView(open,new LinearLayout.LayoutParams(-1,dp(46)));open.setOnClickListener(v->viewCamera(c));
      shown++;
    }
    if(shown==0){TextView empty=txt("Walang nahanap na camera source.",14,MUTED,false);empty.setPadding(dp(12),dp(25),dp(12),dp(25));list.addView(empty);}
  }
  private void viewCamera(CameraSource c){
    root.removeAllViews();LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(8),dp(10),dp(8));bar.setBackgroundColor(PANEL);
    Button back=btn("← LIST");bar.addView(back);back.setOnClickListener(v->{if(web!=null){web.destroy();web=null;}home();});
    TextView title=txt("  "+c.name,16,WHITE,true);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(bar);
    TextView disclaimer=txt("External public source • Live status and playback depend on publisher.",12,MUTED,false);disclaimer.setPadding(dp(12),dp(8),dp(12),dp(8));root.addView(disclaimer);
    web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setMediaPlaybackRequiresUserGesture(false);web.setWebChromeClient(new WebChromeClient());web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,String u){return false;}});
    root.addView(web,new LinearLayout.LayoutParams(-1,0,1));web.loadUrl(c.url);
    Button external=btn("OPEN IN BROWSER");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(dp(12),dp(8),dp(12),dp(10));root.addView(external,p);external.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(c.url)));}catch(Exception e){Toast.makeText(this,"Walang browser na ma-open.",Toast.LENGTH_SHORT).show();}});
  }
  @Override public void onBackPressed(){if(web!=null){web.destroy();web=null;home();}else super.onBackPressed();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}
