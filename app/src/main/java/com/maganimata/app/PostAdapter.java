package com.maganimata.app;

import android.app.*;import android.content.*;import android.view.*;import android.widget.*;import androidx.annotation.NonNull;import androidx.recyclerview.widget.RecyclerView;import com.bumptech.glide.Glide;import org.json.JSONObject;import java.util.*;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.Holder>{
 public interface Click{void open(JSONObject o);} private final List<JSONObject> data=new ArrayList<>();private final Click click;private final Set<Integer> saved=new HashSet<>();private final android.content.SharedPreferences prefs;private final Context context;private String token="";
 public PostAdapter(Context c,Click x){context=c;click=x;prefs=c.getSharedPreferences("saved_posts",Context.MODE_PRIVATE);token=c.getSharedPreferences("magani_prefs",Context.MODE_PRIVATE).getString("auth_token","");loadSaved();}
 void loadSaved(){for(String k:prefs.getAll().keySet())try{saved.add(Integer.parseInt(k));}catch(Exception ignored){}}
 public void setData(List<JSONObject>d){data.clear();data.addAll(d);notifyDataSetChanged();}
 public List<JSONObject> getSaved(){List<JSONObject>x=new ArrayList<>();for(JSONObject o:data)if(saved.contains(o.optInt("id")))x.add(o);return x;}
 public void showSavedOnly(){setData(getSaved());}public void showAll(List<JSONObject>d){setData(d);}
 @NonNull public Holder onCreateViewHolder(@NonNull ViewGroup p,int v){return new Holder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_post,p,false));}
 public void onBindViewHolder(@NonNull Holder h,int i){h.bind(data.get(i));}public int getItemCount(){return data.size();}
 class Holder extends RecyclerView.ViewHolder{
  TextView title,cat,excerpt,date,summary,like,comment,share;ImageView image;ImageButton save;
  Holder(View v){super(v);title=v.findViewById(R.id.title);cat=v.findViewById(R.id.category);excerpt=v.findViewById(R.id.excerpt);date=v.findViewById(R.id.date);summary=v.findViewById(R.id.summary);image=v.findViewById(R.id.image);save=v.findViewById(R.id.save);like=v.findViewById(R.id.like);comment=v.findViewById(R.id.comment);share=v.findViewById(R.id.share);
   v.setOnClickListener(x->{int p=getAdapterPosition();if(p!=RecyclerView.NO_POSITION)click.open(data.get(p));});save.setOnClickListener(x->{int p=getAdapterPosition();if(p==RecyclerView.NO_POSITION)return;JSONObject o=data.get(p);toggleSave(o.optInt("id"));bind(o);});
   like.setOnClickListener(x->{int p=getAdapterPosition();if(p!=RecyclerView.NO_POSITION)toggleLike(data.get(p));});
   comment.setOnClickListener(x->{int p=getAdapterPosition();if(p!=RecyclerView.NO_POSITION)click.open(data.get(p));});
   share.setOnClickListener(x->{int p=getAdapterPosition();if(p!=RecyclerView.NO_POSITION)share(data.get(p));});
  }
  void toggleSave(int id){if(saved.contains(id)){saved.remove(id);prefs.edit().remove(String.valueOf(id)).apply();}else{saved.add(id);prefs.edit().putBoolean(String.valueOf(id),true).apply();}}
  void toggleLike(JSONObject o){token=context.getSharedPreferences("magani_prefs",Context.MODE_PRIVATE).getString("auth_token","");if(token==null||token.isEmpty()){context.startActivity(new Intent(context,AuthActivity.class));return;}Map<String,String>m=new HashMap<>();m.put("action","like");m.put("post_id",String.valueOf(o.optInt("id")));Api.post("engagement.php",m,token,new Api.Callback(){public void ok(String b){try{JSONObject r=new JSONObject(b);o.put("liked",r.optBoolean("liked"));o.put("like_count",r.optInt("like_count"));((Activity)context).runOnUiThread(()->bind(o));}catch(Exception ignored){}}public void err(Exception e){((Activity)context).runOnUiThread(()->Toast.makeText(context,"Unable to like post.",Toast.LENGTH_SHORT).show());}});}
  void share(JSONObject o){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,o.optString("title")+"\n\nLafiyar Mace");context.startActivity(Intent.createChooser(i,"Share post"));Map<String,String>m=new HashMap<>();m.put("action","share");m.put("post_id",String.valueOf(o.optInt("id")));Api.post("engagement.php",m,token,new Api.Callback(){public void ok(String b){try{JSONObject r=new JSONObject(b);o.put("share_count",r.optInt("share_count"));}catch(Exception ignored){}}public void err(Exception e){}});}
  void bind(JSONObject o){title.setText(o.optString("title"));cat.setText(o.optString("category"));excerpt.setText(o.optString("excerpt"));date.setText(o.optString("display_date",o.optString("created_at","")));String u=Api.absoluteImageUrl(o.optString("image_url",""));if(!u.isEmpty()){image.setVisibility(View.VISIBLE);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);Glide.with(image.getContext()).load(u).placeholder(R.drawable.header_woman).error(R.drawable.header_woman).into(image);}else image.setVisibility(View.GONE);save.setImageResource(saved.contains(o.optInt("id"))?android.R.drawable.btn_star_big_on:android.R.drawable.btn_star_big_off);boolean liked=o.optBoolean("liked",false)||o.optInt("liked",0)==1;int lc=o.optInt("like_count",0),cc=o.optInt("comment_count",0),sc=o.optInt("share_count",0);like.setText((liked?"♥":"♡")+"  Like  "+lc);comment.setText("💬  Comment  "+cc);share.setText("↗  Share  "+sc);summary.setText(lc+" likes   •   "+cc+" comments   •   "+sc+" shares");like.setTextColor(liked?0xFFC94380:0xFF5F5660);}
 }
}
