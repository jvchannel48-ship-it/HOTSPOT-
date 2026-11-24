package com.jvchannel48.hotspotcontrol;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int YELLOW=Color.rgb(255,211,35), BLACK=Color.rgb(8,9,11), PANEL=Color.rgb(24,26,30), WHITE=Color.WHITE, MUTED=Color.LTGRAY;
    private final ArrayList<Song> songs=new ArrayList<>();
    private LinearLayout root, list, nowPlaying;
    private EditText search;
    private MediaPlayer player;
    private Uri currentUri;
    private TextView playingTitle, lyric;
    private int currentIndex=-1;
    private boolean videoMode=false;
    private VideoView video;

    static class Song {
        String number,title,artist;
        Uri uri;
        Song(String n,String t,String a){number=n;title=t;artist=a;}
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke>0)d.setStroke(dp(2),YELLOW);return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(BLACK);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(shape(YELLOW,14,0));return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BLACK);getWindow().setNavigationBarColor(BLACK);seed();build();}
    private void seed(){
        songs.add(new Song("00001","Amazing Grace","Traditional"));
        songs.add(new Song("00002","Auld Lang Syne","Traditional"));
        songs.add(new Song("00003","Oh! Susanna","Stephen Foster"));
        songs.add(new Song("00004","When the Saints Go Marching In","Traditional"));
        songs.add(new Song("00005","Danny Boy","Traditional"));
        songs.add(new Song("00006","Greensleeves","Traditional"));
        songs.add(new Song("00007","House of the Rising Sun","Traditional"));
        songs.add(new Song("00008","The Water Is Wide","Traditional"));
        songs.add(new Song("00009","Shenandoah","Traditional"));
        songs.add(new Song("00010","Home on the Range","Traditional"));
    }
    private void build(){
        root=vertical();root.setBackgroundColor(BLACK);
        LinearLayout top=vertical();top.setPadding(dp(16),dp(12),dp(16),dp(10));
        LinearLayout banner=vertical();banner.setBackground(shape(Color.argb(220,12,13,15),22,2));banner.setClipToOutline(true);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);titleRow.setPadding(dp(18),dp(12),dp(16),dp(10));
        TextView note=text("♫",30,YELLOW,true);titleRow.addView(note);TextView heading=text("  NEW SONG",23,WHITE,true);heading.setTypeface(Typeface.create("sans-serif",Typeface.BOLD_ITALIC));titleRow.addView(heading);
        banner.addView(titleRow);View line=new View(this);line.setBackgroundColor(YELLOW);banner.addView(line,new LinearLayout.LayoutParams(-1,dp(2)));
        LinearLayout newSongs=vertical();newSongs.setPadding(dp(14),dp(4),dp(14),dp(8));
        String[][] latest={{"10143","Na Para Bang","Mariah Deborah"},{"10182","SIGABO","Yeng Constantino"},{"27602","Multo","Cup of Joe"},{"28023","Pahina","Cup of Joe"},{"27764","Sarangola","Ben&Ben"},{"28430","Kalapastangan","Fitterkarma"},{"25022","Saksi Ang Langit","December Avenue"},{"28495","Libu-Libong Buwan (Uuwian)","Kyle Raphael"},{"28574","Ikaw Lang Patutunguhan","Amiel Sol"},{"27727","Nailang","Le John"}};
        for(String[] row:latest){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(9),dp(2),dp(9));
            TextView n=text(row[0],15,WHITE,true);n.setWidth(dp(82));r.addView(n);
            TextView t=text(row[1],15,WHITE,true);t.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));r.addView(t);
            TextView a=text(row[2],15,YELLOW,true);a.setGravity(Gravity.RIGHT);r.addView(a);
            newSongs.addView(r);View divider=new View(this);divider.setBackgroundColor(0x66777777);newSongs.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        banner.addView(newSongs);TextView foot=text("▰ ▰ ▰     ♫     ▰ ▰ ▰",17,YELLOW,true);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(4),0,dp(8));banner.addView(foot);
        top.addView(banner);root.addView(top);
        LinearLayout searchRow=new LinearLayout(this);searchRow.setGravity(Gravity.CENTER_VERTICAL);searchRow.setPadding(dp(14),dp(10),dp(14),dp(8));
        Button close=button("×");close.setTextSize(24);searchRow.addView(close,new LinearLayout.LayoutParams(dp(54),dp(54)));close.setOnClickListener(v->{search.setText("");});
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);search.setHintTextColor(0xffa7a7a7);search.setHint("⌕  Title, artist or number");search.setTextSize(16);search.setPadding(dp(16),0,dp(12),0);search.setBackground(shape(PANEL,18,0));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(54),1);ep.leftMargin=dp(12);searchRow.addView(search,ep);root.addView(searchRow);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){render(s.toString());}public void afterTextChanged(android.text.Editable e){}});
        View sep=new View(this);sep.setBackgroundColor(0xff303238);root.addView(sep,new LinearLayout.LayoutParams(-1,dp(1)));
        ScrollView scroll=new ScrollView(this);list=vertical();list.setPadding(dp(16),dp(4),dp(16),dp(10));scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        nowPlaying=vertical();nowPlaying.setPadding(dp(16),dp(10),dp(16),dp(12));nowPlaying.setBackgroundColor(0xff101115);
        playingTitle=text("VIDEO KARAOKE • SONGBOOK",16,YELLOW,true);nowPlaying.addView(playingTitle);
        lyric=text("Pumili ng kanta o magdagdag ng sarili mong audio/video file.",18,WHITE,true);lyric.setGravity(Gravity.CENTER);lyric.setPadding(dp(8),dp(16),dp(8),dp(16));nowPlaying.addView(lyric);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);String[] names={"|◀","▶","Ⅱ","■","▶|","＋ FILE"};for(String name:names){Button b=button(name);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),name.equals("＋ FILE")?1.5f:1);p.setMargins(dp(3),0,dp(3),0);controls.addView(b,p);b.setOnClickListener(v->control(name));}nowPlaying.addView(controls);
        root.addView(nowPlaying);setContentView(root);render("");
    }
    private void render(String q){if(list==null)return;list.removeAllViews();String needle=q.toLowerCase(Locale.ROOT).trim();int count=0;
        for(int i=0;i<songs.size();i++){Song s=songs.get(i);if(!(s.number+" "+s.title+" "+s.artist).toLowerCase(Locale.ROOT).contains(needle))continue;final int index=i;
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),dp(12),dp(6),dp(12));
            TextView num=text(s.number,15,YELLOW,false);num.setWidth(dp(78));row.addView(num);
            LinearLayout details=vertical();details.addView(text(s.title,18,WHITE,false));details.addView(text(s.artist,14,0xffaaaaaa,false));row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            TextView play=text(s.uri==null?"＋":"▶",20,YELLOW,true);row.addView(play);row.setOnClickListener(v->{if(s.uri!=null)playSong(index);else chooseMedia(index);});list.addView(row);
            View divider=new View(this);divider.setBackgroundColor(0xff303238);list.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));count++;
        }
        if(count==0){TextView empty=text("Walang kanta. Magdagdag ng audio/video file.",15,MUTED,false);empty.setPadding(dp(8),dp(24),0,dp(24));list.addView(empty);}
    }
    private void chooseMedia(int index){currentIndex=index;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"audio/*","video/*"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,41);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==41&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);songs.get(currentIndex).uri=u;playSong(currentIndex);render(search.getText().toString());}}
    private void playSong(int index){currentIndex=index;Song s=songs.get(index);playingTitle.setText(s.title+"  •  "+s.artist);lyric.setText("♫  "+s.title+"  ♫\n\nLyrics file can be added with the song.");String type=getContentResolver().getType(s.uri);videoMode=type!=null&&type.startsWith("video");if(videoMode){if(player!=null){player.release();player=null;}if(video==null){video=new VideoView(this);nowPlaying.addView(video,1,new LinearLayout.LayoutParams(-1,dp(180)));}video.setVisibility(View.VISIBLE);video.setVideoURI(s.uri);video.setOnPreparedListener(mp->video.start());}else{if(video!=null)video.setVisibility(View.GONE);try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(this,s.uri);player.setOnPreparedListener(mp->mp.start());player.setOnErrorListener((mp,w,e)->{toast("Hindi mabuksan ang audio file.");return true;});player.prepareAsync();}catch(Exception e){toast("Hindi mabuksan ang audio file.");}}}
    private void control(String action){if(action.equals("＋ FILE")){chooseMedia(Math.max(0,currentIndex));return;}try{if(action.equals("▶")){if(videoMode&&video!=null)video.start();else if(player!=null)player.start();}else if(action.equals("Ⅱ")){if(videoMode&&video!=null)video.pause();else if(player!=null&&player.isPlaying())player.pause();}else if(action.equals("■")){if(videoMode&&video!=null)video.stopPlayback();else if(player!=null){player.stop();player.release();player=null;}}else if(action.equals("|◀")){if(currentIndex>0)playSong(currentIndex-1);}else if(action.equals("▶|")){if(currentIndex+1<songs.size())playSong(currentIndex+1);}}catch(Exception e){toast("Pumili muna ng kanta.");}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){super.onDestroy();if(player!=null){player.release();player=null;}}
}
