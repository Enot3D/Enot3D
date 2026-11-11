package ru.enot.music;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;
public class MainActivity extends Activity {
 static final String HUB="https://homemusic-hub-production.up.railway.app";
 LinearLayout root,nav; FrameLayout screen; WebView web; MediaPlayer player;
 SeekBar seek; TextView title,time; boolean offline=false; int current=-1;
 final ArrayList<Uri> tracks=new ArrayList<>(); final ArrayList<String> names=new ArrayList<>();
 Handler handler=new Handler(Looper.getMainLooper());
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextColor(-1);t.setTextSize(size);t.setPadding(dp(12),dp(10),dp(12),dp(10));return t;}
 void msg(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xff111827);root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(0xff111827);setContentView(root);TextView logo=text("🦝 ENOT MUSIC",22);logo.setTypeface(null,Typeface.BOLD);root.addView(logo);screen=new FrameLayout(this);root.addView(screen,new LinearLayout.LayoutParams(-1,0,1));nav=new LinearLayout(this);root.addView(nav);tab("♫ Музыка",()->online());tab("↓ Офлайн",()->local());tab("⚙ Настройки",()->settings());online();}
 void tab(String name,Runnable r){TextView t=text(name,13);t.setGravity(Gravity.CENTER);nav.addView(t,new LinearLayout.LayoutParams(0,dp(56),1));t.setOnClickListener(v->r.run());}
 void online(){offline=false;screen.removeAllViews();if(web==null){web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.setWebViewClient(new WebViewClient());web.setWebChromeClient(new WebChromeClient());web.setDownloadListener((url,ua,cd,mime,len)->download(url,ua,cd,mime));web.loadUrl(HUB);}if(web.getParent()!=null)((android.view.ViewGroup)web.getParent()).removeView(web);screen.addView(web);}
 void download(String url,String ua,String cd,String mime){try{if(!url.startsWith("https://")){msg("Нужна HTTPS-ссылка");return;}DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));if(ua!=null)r.addRequestHeader("User-Agent",ua);String cookie=CookieManager.getInstance().getCookie(url);if(cookie!=null)r.addRequestHeader("Cookie",cookie);String name=URLUtil.guessFileName(url,cd,mime);r.setTitle(name);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);r.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC,"ENOTMusic/"+name);((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(r);msg("🦝 Загрузка началась");}catch(Exception e){msg("Ошибка загрузки: "+e.getMessage());}}
 void local(){offline=true;screen.removeAllViews();LinearLayout page=new LinearLayout(this);page.setOrientation(1);screen.addView(page);page.addView(text("🦝 Офлайн-музыка",21));Button refresh=new Button(this);refresh.setText("↻ Обновить");page.addView(refresh);refresh.setOnClickListener(v->local());if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO},10);return;}if(Build.VERSION.SDK_INT>=23&&Build.VERSION.SDK_INT<33&&checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},10);return;}
 tracks.clear();names.clear();try(Cursor c=getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME},null,null,MediaStore.Audio.Media.DISPLAY_NAME+" ASC")){if(c!=null)while(c.moveToNext()){tracks.add(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,c.getLong(0)));names.add(c.getString(1));}}catch(Exception e){msg("Нет доступа к файлам: "+e.getMessage());}
 ScrollView sc=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));for(int i=0;i<names.size();i++){final int n=i;TextView t=text("♫ "+names.get(i),15);list.addView(t);t.setOnClickListener(v->play(n));}if(names.isEmpty())list.addView(text("🦝 Пока нет сохранённых песен",15));
 title=text("Выбери песню",14);page.addView(title);seek=new SeekBar(this);seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));page.addView(seek);seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int p,boolean from){if(from&&player!=null)player.seekTo(p);}});
 LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);page.addView(controls);control(controls,"⏮",()->play(Math.max(0,current-1)));control(controls,"⏯",()->{if(player!=null){if(player.isPlaying())player.pause();else player.start();}});control(controls,"⏭",()->play(Math.min(tracks.size()-1,current+1)));time=text("0:00",12);page.addView(time);handler.removeCallbacksAndMessages(null);handler.postDelayed(new Runnable(){public void run(){if(offline&&player!=null&&seek!=null){seek.setMax(player.getDuration());seek.setProgress(player.getCurrentPosition());time.setText(fmt(player.getCurrentPosition())+" / "+fmt(player.getDuration()));}handler.postDelayed(this,700);}},700);}
 void control(LinearLayout row,String name,Runnable r){Button b=new Button(this);b.setText(name);row.addView(b);b.setOnClickListener(v->r.run());}
 void play(int i){if(i<0||i>=tracks.size())return;try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(this,tracks.get(i));player.prepare();player.start();current=i;title.setText("🦝 "+names.get(i));player.setOnCompletionListener(mp->{if(current+1<tracks.size())play(current+1);});}catch(Exception e){msg("Не удалось воспроизвести: "+e.getMessage());}}
 String fmt(int ms){int s=ms/1000;return s/60+":"+String.format(Locale.US,"%02d",s%60);}
 void settings(){offline=false;screen.removeAllViews();LinearLayout p=new LinearLayout(this);p.setOrientation(1);screen.addView(p);p.addView(text("🦝 ENOT Music",22));p.addView(text("Сервер: "+HUB,13));p.addView(text("Офлайн-библиотека читает аудиофайлы из памяти телефона.",13));}
 @Override public void onRequestPermissionsResult(int req,String[] p,int[] result){super.onRequestPermissionsResult(req,p,result);if(req==10&&result.length>0&&result[0]==PackageManager.PERMISSION_GRANTED)local();}
 @Override public void onBackPressed(){if(!offline&&web!=null&&web.getParent()!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(player!=null)player.release();super.onDestroy();}
}
