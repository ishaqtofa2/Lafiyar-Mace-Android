package com.maganimata.app;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.net.Uri;import android.os.*;import android.view.*;import android.widget.*;import com.bumptech.glide.Glide;import org.json.*;import java.util.*;

public class PostActivity extends Activity {
 private final int P=Color.rgb(201,67,128),D=Color.rgb(48,39,46),M=Color.rgb(112,101,110),BG=Color.rgb(255,248,252); private LinearLayout page,commentsBox; private TextView likeBtn,counts,saveBtn; private String token=""; private int postId; private Handler handler=new Handler(); private AdController ads; private SharedPreferences savedPrefs;
 private int dp(float v){return(int)(v*getResources().getDisplayMetrics().density+.5f);} private TextView tv(String s,float z,int c,boolean b){TextView v=new TextView(this);v.setText(s==null?"":s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(null,b?Typeface.BOLD:Typeface.NORMAL);v.setLineSpacing(dp(1),1.05f);return v;} private GradientDrawable bg(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
 @Override public void onCreate(Bundle b){super.onCreate(b);token=getSharedPreferences("magani_prefs",MODE_PRIVATE).getString("auth_token","");savedPrefs=getSharedPreferences("saved_posts",MODE_PRIVATE);ads=new AdController(this);setContentView(R.layout.activity_post);findViewById(R.id.back).setOnClickListener(v->finish());page=findViewById(R.id.page);postId=getIntent().getIntExtra("id",0);String path=getIntent().getDataString();if(postId<=0&&path!=null){try{Uri u=getIntent().getData();String last=u.getLastPathSegment();if(last!=null)postId=Integer.parseInt(last);if(postId<=0)postId=Integer.parseInt(u.getQueryParameter("id"));}catch(Exception ignored){}}loadAdSettingsThenOpenPost();}
 private void loadAdSettingsThenOpenPost(){
  Api.get("settings.php",token,new Api.Callback(){
   public void ok(String b){
    JSONObject s;
    try { JSONObject r=new JSONObject(b); s=r.optJSONObject("settings")!=null?r.optJSONObject("settings"):r; }
    catch(Exception ignored){ s=new JSONObject(); }
    final JSONObject settings = s;
    runOnUiThread(() -> { if (isFinishing() || isDestroyed()) return; ads.apply(settings); openPostAfterSettings(); });
   }
   public void err(Exception e){ runOnUiThread(() -> { if (isFinishing() || isDestroyed()) return; ads.apply(new JSONObject()); openPostAfterSettings(); }); }
  });
 }
 private void openPostAfterSettings(){
  Intent source = getIntent();
  String passedTitle = source.getStringExtra("title");
  String passedBody = source.getStringExtra("content");
  // MainActivity already has the selected article data. Render it immediately so a backend
  // endpoint that does not support ?id= cannot show the wrong article or a blank page.
  if (passedTitle != null && !passedTitle.trim().isEmpty() && passedBody != null && !passedBody.trim().isEmpty()) {
   render(source);
   return;
  }
  if(postId>0)loadPost();else render(source);
 }
 private void loadPost(){Api.get("posts.php?id="+postId,token,new Api.Callback(){public void ok(String b){runOnUiThread(()->{try{JSONArray a=new JSONObject(b).optJSONArray("posts");if(a==null||a.length()==0){showError("This post is no longer available.");return;}JSONObject o=a.getJSONObject(0);render(o); }catch(Exception e){showError("Unable to open this post.");}});}public void err(Exception e){runOnUiThread(()->showError("Unable to open this post. Check your internet connection."));}});}
 private void render(Intent i){JSONObject o=new JSONObject();try{o.put("title",i.getStringExtra("title"));o.put("category",i.getStringExtra("category"));o.put("content",i.getStringExtra("content"));o.put("image_url",i.getStringExtra("image"));o.put("display_date",i.getStringExtra("date"));}catch(Exception ignored){}render(o);}
 private void render(JSONObject o){page.removeAllViews();String title=o.optString("title","Health article"),category=o.optString("category","General"),body=o.optString("content",""),image=o.optString("image_url","");if(image!=null&&!image.trim().isEmpty()){ImageView hero=new ImageView(this);hero.setAdjustViewBounds(true);hero.setScaleType(ImageView.ScaleType.FIT_CENTER);hero.setBackground(bg(0xFFF1E7EE,18));Glide.with(this).load(Api.absoluteImageUrl(image)).placeholder(bg(0xFFF1E7EE,18)).error(bg(0xFFF1E7EE,18)).into(hero);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);hp.setMargins(0,0,0,dp(16));page.addView(hero,hp);}TextView cat=tv(category.toUpperCase(),11,P,true);cat.setPadding(dp(10),dp(6),dp(10),dp(6));cat.setBackground(bg(0xFFFFEEF5,20));page.addView(cat,new LinearLayout.LayoutParams(-2,dp(32)));TextView heading=tv(title,27,D,true);heading.setPadding(0,dp(10),0,dp(5));page.addView(heading);String date=o.optString("display_date",o.optString("created_at",""));if(!date.isEmpty()){TextView dt=tv(date,12,M,false);dt.setPadding(0,0,0,dp(12));page.addView(dt);}int paragraph=0;int articleAdEvery=Math.max(1,Math.min(20,ads.getArticleAdFrequency()));int articleAdsShown=0;for(String raw:body.split("\\n\\s*\\n|\\n")){String p=raw.trim();if(p.isEmpty())continue;TextView text=tv(p,17,0xFF4D454B,false);text.setPadding(0,0,0,dp(18));page.addView(text);paragraph++;if(paragraph%articleAdEvery==0&&articleAdsShown<3){if(articleAdsShown%2==0){if(ads.isArticleNativeEnabled())ads.showArticleNative(page);else if(ads.isArticleBannerEnabled())ads.showArticleBanner(page);}else{if(ads.isArticleBannerEnabled())ads.showArticleBanner(page);else if(ads.isArticleNativeEnabled())ads.showArticleNative(page);}articleAdsShown++;}}TextView note=tv("Important: This app provides general educational information and is not a substitute for diagnosis, treatment, or advice from a qualified healthcare professional.",13,0xFF756B73,false);note.setPadding(dp(14),dp(14),dp(14),dp(14));note.setBackground(bg(0xFFFFEAF3,14));page.addView(note);addActions();addComments();loadComments();loadRelated();}
 private TextView action(String s){TextView v=tv(s,13,0xFF5F5660,true);v.setGravity(Gravity.CENTER);v.setBackground(bg(0xFFFFFFFF,10));v.setPadding(dp(4),0,dp(4),0);return v;}
 private void addActions(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(5),dp(7),dp(5),dp(5));box.setBackground(bg(Color.WHITE,16));counts=tv("Loading engagement…",12,M,false);counts.setPadding(dp(10),0,dp(10),dp(5));box.addView(counts,new LinearLayout.LayoutParams(-1,dp(27)));LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);likeBtn=action("♡  Like");TextView comment=action("💬  Comment");TextView share=action("↗  Share");saveBtn=action(savedPrefs.getBoolean(String.valueOf(postId),false)?"★  Saved":"☆  Save");row.addView(likeBtn,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(comment,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(saveBtn,new LinearLayout.LayoutParams(0,dp(48),1));box.addView(row);page.addView(box,new LinearLayout.LayoutParams(-1,-2));likeBtn.setOnClickListener(v->toggleLike());comment.setOnClickListener(v->showCommentInput(0,false));share.setOnClickListener(v->sharePost());saveBtn.setOnClickListener(v->{boolean n=!savedPrefs.getBoolean(String.valueOf(postId),false);savedPrefs.edit().putBoolean(String.valueOf(postId),n).apply();saveBtn.setText(n?"★  Saved":"☆  Save");});loadPostStats();}
 private void loadPostStats(){Api.get("posts.php?id="+postId,token,new Api.Callback(){public void ok(String b){runOnUiThread(()->{try{JSONArray a=new JSONObject(b).optJSONArray("posts");if(a!=null&&a.length()>0){JSONObject o=a.getJSONObject(0);updateStats(o.optBoolean("liked",false)||o.optInt("liked",0)==1,o.optInt("like_count"),o.optInt("comment_count"),o.optInt("share_count"));}}catch(Exception ignored){}});}public void err(Exception e){}});}
 private void updateStats(boolean liked,int l,int c,int s){likeBtn.setText((liked?"♥":"♡")+"  Like");likeBtn.setTextColor(liked?P:0xFF5F5660);counts.setText(l+" likes   •   "+c+" comments   •   "+s+" shares");}
 private void toggleLike(){if(token.isEmpty()){startActivity(new Intent(this,AuthActivity.class));return;}Map<String,String>m=new HashMap<>();m.put("action","like");m.put("post_id",String.valueOf(postId));Api.post("engagement.php",m,token,new Api.Callback(){public void ok(String b){runOnUiThread(()->{try{JSONObject r=new JSONObject(b);updateStats(r.optBoolean("liked"),r.optInt("like_count"),r.optInt("comment_count"),r.optInt("share_count"));}catch(Exception ignored){}});}public void err(Exception e){showError("Unable to update like.");}});}
 private void sharePost(){String url=Config.WEB_BASE_URL+"share.php?id="+postId;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,getIntent().getStringExtra("title"));i.putExtra(Intent.EXTRA_TEXT,url);startActivity(Intent.createChooser(i,"Share post"));Map<String,String>m=new HashMap<>();m.put("action","share");m.put("post_id",String.valueOf(postId));Api.post("engagement.php",m,token,new Api.Callback(){public void ok(String b){loadPostStats();}public void err(Exception e){}});}
 private void addComments(){page.addView(tv("Comments",21,D,true),new LinearLayout.LayoutParams(-1,dp(38)));commentsBox=new LinearLayout(this);commentsBox.setOrientation(LinearLayout.VERTICAL);page.addView(commentsBox);TextView add=action("＋  Write a comment");LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(8),0,dp(12));page.addView(add,p);add.setOnClickListener(v->showCommentInput(0,false));}
 private void showCommentInput(int parentId, boolean reply) {
  if (token.isEmpty()) {
   startActivity(new Intent(this, AuthActivity.class));
   return;
  }
  if (postId <= 0) {
   showError("This post could not be identified. Open the article again and retry.");
   return;
  }

  LinearLayout box = new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.setPadding(dp(2), 0, dp(2), 0);
  EditText input = new EditText(this);
  input.setHint(reply ? "Write a reply…" : "Write something respectful…");
  input.setTextSize(15);
  input.setGravity(Gravity.TOP | Gravity.START);
  input.setMinLines(4);
  input.setPadding(dp(14), dp(12), dp(14), dp(12));
  input.setBackground(bg(Color.WHITE, 16));
  box.addView(input, new LinearLayout.LayoutParams(-1, dp(120)));

  AlertDialog dialog = new AlertDialog.Builder(this)
      .setTitle(reply ? "Reply to comment" : "Add comment")
      .setView(box)
      .setNegativeButton("Cancel", null)
      .setPositiveButton(reply ? "Reply" : "Post", null)
      .create();

  dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
   String text = input.getText().toString().trim();
   if (text.isEmpty()) {
    input.setError("Please write a comment first.");
    input.requestFocus();
    return;
   }
   if (text.length() > 5000) {
    input.setError("Comments must be 5,000 characters or fewer.");
    return;
   }

   android.widget.Button submit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
   submit.setEnabled(false);
   submit.setText("Sending…");
   Map<String, String> values = new HashMap<>();
   values.put("post_id", String.valueOf(postId));
   values.put("content", text);
   if (reply) {
    values.put("action", "reply");
    values.put("parent_id", String.valueOf(parentId));
   }

   Api.post("comments.php", values, token, new Api.Callback() {
    @Override public void ok(String response) {
     runOnUiThread(() -> {
      try {
       JSONObject result = new JSONObject(response);
       if (result.has("ok") && !result.optBoolean("ok", true)) {
        submit.setEnabled(true);
        submit.setText(reply ? "Reply" : "Post");
        showError(result.optString("error", "The comment was not accepted by the server."));
        return;
       }
      } catch (Exception ignoredParse) {
       // Some existing API versions return a plain-text success response.
      }
      dialog.dismiss();
      loadComments();
      loadPostStats();
     });
    }
    @Override public void err(Exception error) {
     runOnUiThread(() -> {
      submit.setEnabled(true);
      submit.setText(reply ? "Reply" : "Post");
      String detail = error == null ? "Unknown network error" : error.getMessage();
      if (detail == null || detail.trim().isEmpty()) detail = "Please check your internet connection and comments API.";
      showError("Unable to post your comment. " + detail);
     });
    }
   });
  }));
  dialog.show();
 }
 private void loadComments(){Api.get("comments.php?post_id="+postId,token,new Api.Callback(){public void ok(String b){runOnUiThread(()->{try{JSONArray a=new JSONObject(b).optJSONArray("comments");commentsBox.removeAllViews();if(a==null||a.length()==0){TextView empty=tv("No comments yet. Be the first to join the conversation.",13,M,false);empty.setPadding(dp(14),dp(14),dp(14),dp(14));commentsBox.addView(empty);return;}Map<Integer,JSONObject> byId=new HashMap<>();Map<Integer,List<JSONObject>> children=new HashMap<>();for(int i=0;i<a.length();i++){JSONObject c=a.getJSONObject(i);int id=c.optInt("id"),parent=c.optInt("parent_id",0);byId.put(id,c);if(parent>0){List<JSONObject> list=children.get(parent);if(list==null){list=new ArrayList<>();children.put(parent,list);}list.add(c);}}for(int i=0;i<a.length();i++){JSONObject c=a.getJSONObject(i);if(c.optInt("parent_id",0)==0)renderCommentTree(c,children,0);}}catch(Exception e){showError("Unable to load comments.");}});}public void err(Exception e){runOnUiThread(()->showError("Unable to load comments. " + (e == null || e.getMessage() == null ? "Check the comments API endpoint." : e.getMessage())));}});}
 private void renderCommentTree(JSONObject c,Map<Integer,List<JSONObject>> children,int depth){LinearLayout card=commentCard(c,depth>0);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(depth*dp(24),dp(depth>0?4:0),0,dp(8));commentsBox.addView(card,cp);List<JSONObject> kids=children.get(c.optInt("id"));if(kids!=null)for(JSONObject child:kids)renderCommentTree(child,children,depth+1);}
 private LinearLayout commentCard(JSONObject c,boolean reply){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(11),dp(14),dp(10));card.setBackground(bg(Color.WHITE,16));TextView n=tv(c.optString("name","User"),14,D,true);card.addView(n);TextView dt=tv(c.optString("display_date"),10,M,false);dt.setPadding(0,dp(2),0,dp(7));card.addView(dt);TextView body=tv(c.optString("content"),14,0xFF4D454B,false);body.setPadding(0,0,0,dp(8));card.addView(body);LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);TextView like=action((c.optBoolean("liked",false)||c.optInt("liked",0)==1?"♥":"♡")+"  "+c.optInt("like_count",0));TextView replyBtn=action("↩  Reply");actions.addView(like,new LinearLayout.LayoutParams(dp(95),dp(34)));actions.addView(replyBtn,new LinearLayout.LayoutParams(dp(100),dp(34)));card.addView(actions);like.setOnClickListener(v->likeComment(c));replyBtn.setOnClickListener(v->showCommentInput(c.optInt("id"),true));return card;}
 private void likeComment(JSONObject c){if(token.isEmpty()){startActivity(new Intent(this,AuthActivity.class));return;}Map<String,String>m=new HashMap<>();m.put("action","like_comment");m.put("post_id",String.valueOf(postId));m.put("comment_id",String.valueOf(c.optInt("id")));Api.post("comments.php",m,token,new Api.Callback(){public void ok(String b){try{JSONObject r=new JSONObject(b);c.put("liked",r.optBoolean("liked"));c.put("like_count",r.optInt("like_count"));}catch(Exception ignored){}runOnUiThread(()->loadComments());}public void err(Exception e){showError("Unable to like comment.");}});}
 private void loadRelated(){Api.get("related_posts.php?post_id="+postId+"&limit=5",token,new Api.Callback(){public void ok(String b){runOnUiThread(()->{try{JSONArray a=new JSONObject(b).optJSONArray("posts");if(a==null||a.length()==0)return;page.addView(tv("Wasu posts makamancin wannan",20,D,true));LinearLayout wrap=new LinearLayout(PostActivity.this);wrap.setOrientation(LinearLayout.VERTICAL);page.addView(wrap);for(int i=0;i<a.length();i++)addRelatedCard(wrap,a.getJSONObject(i));}catch(Exception ignored){}});}public void err(Exception e){}});}
 private void addRelatedCard(LinearLayout wrap,JSONObject o){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(dp(8),dp(8),dp(8),dp(8));card.setBackground(bg(Color.WHITE,14));ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable ph=bg(0xFFF1E7EE,10);im.setBackground(ph);String u=Api.absoluteImageUrl(o.optString("image_url",""));if(!u.isEmpty())Glide.with(this).load(u).placeholder(ph).error(ph).into(im);card.addView(im,new LinearLayout.LayoutParams(dp(105),dp(82)));LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(10),0,0,0);body.addView(tv(o.optString("category","General").toUpperCase(),10,P,true));TextView title=tv(o.optString("title","Health article"),14,D,true);title.setMaxLines(2);body.addView(title);body.addView(tv(o.optString("display_date",""),10,M,false));card.addView(body,new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(9));wrap.addView(card,cp);card.setOnClickListener(v->{Intent i=new Intent(this,PostActivity.class);i.putExtra("id",o.optInt("id"));startActivity(i);});}
 private void showError(String s){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Lafiyar Mace").setMessage(s).setPositiveButton("OK",null).show());}
}
