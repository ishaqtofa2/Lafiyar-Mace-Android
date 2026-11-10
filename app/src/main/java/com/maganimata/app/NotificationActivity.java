package com.maganimata.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class NotificationActivity extends Activity {
    private final int P=Color.rgb(201,67,128), D=Color.rgb(48,39,46), M=Color.rgb(112,101,110), BG=Color.rgb(255,248,252);
    private LinearLayout list;
    private String token="";
    private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
    private TextView tv(String s,float z,int c,boolean b){TextView v=new TextView(this);v.setText(s==null?"":s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(null,b?Typeface.BOLD:Typeface.NORMAL);return v;}
    private GradientDrawable bg(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}

    @Override public void onCreate(Bundle b){super.onCreate(b);token=getSharedPreferences("magani_prefs",MODE_PRIVATE).getString("auth_token","");build();load();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(8),dp(10),dp(8));bar.setBackgroundColor(P);
        TextView back=tv("‹",34,Color.WHITE,false);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(50)));
        TextView title=tv("Notifications",20,Color.WHITE,true);bar.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));
        TextView all=tv("Mark all read",12,Color.WHITE,true);all.setGravity(Gravity.CENTER);all.setOnClickListener(v->markAll());bar.addView(all,new LinearLayout.LayoutParams(dp(95),dp(50)));
        root.addView(bar);
        ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(dp(12),dp(12),dp(12),dp(25));sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }

    private void load(){
        if(token.isEmpty()){showEmpty("Login to receive and read your notifications.");return;}
        Api.get("notifications.php",token,new Api.Callback(){
            public void ok(String body){runOnUiThread(()->{try{JSONObject r=new JSONObject(body);JSONArray a=r.optJSONArray("notifications");list.removeAllViews();if(a==null||a.length()==0){showEmpty("Babu sabbin notifications a yanzu.");return;}for(int i=0;i<a.length();i++)addItem(a.getJSONObject(i));}catch(Exception e){showEmpty("Unable to load notifications.");}});}
            public void err(Exception e){runOnUiThread(()->showEmpty("Unable to load notifications. Check your internet connection."));}
        });
    }

    private void addItem(JSONObject n){
        boolean read=n.optInt("is_read",0)==1;
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(dp(10),dp(10),dp(10),dp(10));card.setBackground(bg(read?Color.WHITE:0xFFFFEDF5,16));
        ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable ph=bg(0xFFF1E7EE,12);im.setBackground(ph);String u=Api.absoluteImageUrl(n.optString("image_url",""));if(!u.isEmpty())Glide.with(this).load(u).placeholder(ph).error(ph).into(im);card.addView(im,new LinearLayout.LayoutParams(dp(62),dp(62)));
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(10),0,0,0);
        TextView title=tv(n.optString("title","Sabon bayani"),15,D,true);body.addView(title);
        TextView msg=tv(n.optString("message",""),13,D,false);msg.setMaxLines(2);body.addView(msg,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView date=tv(n.optString("display_date",""),10,M,false);body.addView(date);
        card.addView(body,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(9));list.addView(card,cp);
        card.setOnClickListener(v->{markRead(n.optInt("id",0));int postId=n.optInt("post_id",0);if(postId>0){Intent i=new Intent(this,PostActivity.class);i.putExtra("id",postId);i.putExtra("title",n.optString("post_title",n.optString("message")));i.putExtra("content",n.optString("content",""));i.putExtra("category",n.optString("category","General"));i.putExtra("image",Api.absoluteImageUrl(n.optString("image_url","")));i.putExtra("date",n.optString("display_date",""));startActivity(i);}});
    }

    private void markRead(int id){if(id<=0||token.isEmpty())return;Map<String,String> m=new HashMap<>();m.put("action","read");m.put("id",String.valueOf(id));Api.post("notifications.php",m,token,new Api.Callback(){public void ok(String b){}public void err(Exception e){}});}
    private void markAll(){if(token.isEmpty())return;Map<String,String> m=new HashMap<>();m.put("action","read_all");Api.post("notifications.php",m,token,new Api.Callback(){public void ok(String b){runOnUiThread(()->load());}public void err(Exception e){}});}
    private void showEmpty(String s){list.removeAllViews();TextView t=tv(s,15,M,false);t.setGravity(Gravity.CENTER);t.setPadding(dp(20),dp(70),dp(20),dp(20));list.addView(t,new LinearLayout.LayoutParams(-1,-2));}
}
