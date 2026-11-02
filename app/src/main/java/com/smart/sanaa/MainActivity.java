package com.smart.sanaa;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;import java.util.concurrent.*;import java.io.IOException;

/** Native Smart Manager application. UI is backed by real RouterOS API calls. */
public class MainActivity extends Activity {
  final int NAVY=Color.rgb(16,42,67),TEAL=Color.rgb(21,155,168),MUTED=Color.rgb(107,124,147),SURFACE=Color.rgb(244,248,251),DANGER=Color.rgb(214,69,85); LinearLayout body; SharedPreferences prefs; ExecutorService io=Executors.newSingleThreadExecutor(); RouterOsClient client; String backend="unknown",version="—"; Map<String,String> resource=new HashMap<>(); int hotspotActive=-1,pppActive=-1,umCount=-1; boolean refreshing=false,creatingBatch=false; ArrayList<String> lastBatch=new ArrayList<>(); String lastProfile=""; static final int PDF_REQUEST=408;
  @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("router",0);showDashboard();}
  TextView text(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(18,12,18,12);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);return t;}
  GradientDrawable shape(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
  Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(c,28));return b;}
  void base(String title){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(prefs.getBoolean("dark",false)?Color.rgb(12,25,41):SURFACE);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout head=new LinearLayout(this);head.setPadding(16,18,16,8);TextView h=text(title,24,NAVY);h.setTypeface(null,1);head.addView(h,new LinearLayout.LayoutParams(0,66,1));Button menu=button("☰",TEAL);head.addView(menu,new LinearLayout.LayoutParams(62,58));menu.setOnClickListener(v->{PopupMenu pm=new PopupMenu(this,menu);String[] items={"الرئيسية","إضافة كروت","الكروت","الكروت المنتهية","الجلسات","التقارير","صيانة قاعدة البيانات","الإعدادات","الوضع الليلي / النهاري"};for(String item:items)pm.getMenu().add(item);pm.setOnMenuItemClickListener(item->{String choice=item.getTitle().toString();switch(choice){case "الرئيسية":showDashboard();break;case "إضافة كروت":showAdd();break;case "الكروت":showCards();break;case "الكروت المنتهية":showExpired();break;case "الجلسات":showSessions();break;case "التقارير":showReports();break;case "صيانة قاعدة البيانات":showMaintenance();break;case "الإعدادات":showSettings();break;default:prefs.edit().putBoolean("dark",!prefs.getBoolean("dark",false)).apply();showDashboard();}return true;});pm.show();});root.addView(head);ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(18,8,18,130);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav());setContentView(root);}
  LinearLayout nav(){LinearLayout n=new LinearLayout(this);n.setPadding(5,6,5,6);String[] a={"الرئيسية","الكروت","الجلسات","التقارير","الصيانة","الإعدادات"};for(String x:a){Button b=button(x,Color.WHITE);b.setTextColor(NAVY);b.setTextSize(11);b.setOnClickListener(v->{switch(x){case "الرئيسية":showDashboard();break;case "الكروت":showCards();break;case "الجلسات":showSessions();break;case "التقارير":showReports();break;case "الصيانة":showMaintenance();break;default:showSettings();}});n.addView(b,new LinearLayout.LayoutParams(0,60,1));}return n;}
  void space(){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,12));}
  void card(String h,String v){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(10,8,10,10);c.setBackground(shape(prefs.getBoolean("dark",false)?Color.rgb(26,49,74):Color.WHITE,24));c.addView(text(h,14,TEAL));TextView v1=text(v,23,prefs.getBoolean("dark",false)?Color.WHITE:NAVY);v1.setTypeface(null,1);c.addView(v1);body.addView(c);space();}
  void showDashboard(){
    base("الرئيسية");
    LinearLayout hero=new LinearLayout(this);
    hero.setOrientation(LinearLayout.VERTICAL);
    hero.setPadding(18,18,18,18);
    hero.setBackground(shape(Color.rgb(5,31,61),30));
    TextView branding=text("سمارت صنعاء",29,Color.WHITE);
    branding.setTypeface(null,1);hero.addView(branding);
    hero.addView(text("MikroTik Network Management",14,Color.rgb(137,210,233)));
    hero.addView(text("إدارة الشبكات المحلية • RouterOS 6 / 7",14,Color.WHITE));
    body.addView(hero);space();
    if(client==null){
      Button connect=button("الاتصال بالراوتر",TEAL);
      connect.setOnClickListener(v->showSettings());body.addView(connect);space();
    }else{
      Button refresh=button("↻ تحديث البيانات",TEAL);
      refresh.setOnClickListener(v->loadDashboard());body.addView(refresh);space();
    }
    card("RouterOS",version);
    card("استخدام CPU",resource.getOrDefault("cpu-load","—")+"%");
    card("مدة التشغيل",resource.getOrDefault("uptime","—"));
    body.addView(text("الإحصاءات",22,prefs.getBoolean("dark",false)?Color.WHITE:NAVY));
    card("PPPoE النشط",pppActive<0?"—":String.valueOf(pppActive));
    card("Hotspot النشط",hotspotActive<0?"—":String.valueOf(hotspotActive));
    card("إجمالي كروت User Manager",umCount<0?"غير متاح":String.valueOf(umCount));
    body.addView(text("الوصول السريع",22,prefs.getBoolean("dark",false)?Color.WHITE:NAVY));
    String[] routes={"إضافة الكروت","عمليات الكروت","الجلسات","الكروت المنتهية","صيانة User Manager"};
    for(String r:routes){
      Button link=button("›   "+r,Color.rgb(18,89,132));
      link.setOnClickListener(v->{switch(r){case "إضافة الكروت":showAdd();break;case "عمليات الكروت":showCards();break;case "الجلسات":showSessions();break;case "الكروت المنتهية":showExpired();break;default:showMaintenance();}});
      body.addView(link);space();
    }
    card("معلومات الراوتر",resource.getOrDefault("board-name","—")+"\n"+resource.getOrDefault("architecture-name","—")+"\nالذاكرة الحرة: "+resource.getOrDefault("free-memory","—")+"\nالتخزين الحر: "+resource.getOrDefault("free-hdd-space","—"));
  }
  void loadDashboard(){
    if(client==null){showSettings();return;}
    if(refreshing)return;
    refreshing=true;
    io.execute(()->{
      try{
        List<Map<String,String>> r=client.print("/system/resource/print");
        if(r.isEmpty())throw new IllegalStateException("No RouterOS resource data");
        resource=new HashMap<>(r.get(0));
        version=resource.getOrDefault("version","—");
        hotspotActive=count("/ip/hotspot/active/print");
        pppActive=count("/ppp/active/print");
        umCount=-1;backend="غير متوفر";
        if(version.startsWith("7.")){
          try{umCount=count("/user-manager/user/print");backend="User Manager 7";}catch(Exception ignored){}
        }else if(version.startsWith("6.")){
          try{umCount=count("/tool/user-manager/user/print");backend="User Manager 6";}catch(Exception ignored){}
        }
        runOnUiThread(()->{refreshing=false;showDashboard();});
      }catch(Exception e){runOnUiThread(()->{refreshing=false;toast("تعذر قراءة الراوتر: "+e.getMessage());});}
    });
  }
  int count(String path){try{long result=client.count(path);return (int)Math.min(Integer.MAX_VALUE,result);}catch(Exception e){return -1;}}
  void showCards(){base("الكروت");body.addView(text("إدارة الكروت والمستخدمين",22,NAVY));LinearLayout actions=new LinearLayout(this);Button add=button("＋ إضافة دفعة",TEAL);add.setOnClickListener(v->showAdd());Button exp=button("الكروت المنتهية",DANGER);exp.setOnClickListener(v->showExpired());actions.addView(add,new LinearLayout.LayoutParams(0,58,1));actions.addView(exp,new LinearLayout.LayoutParams(0,58,1));body.addView(actions);space();Button load=button("تحميل الكروت من الراوتر",NAVY);load.setOnClickListener(v->loadUsers());body.addView(load);}
  void loadUsers(){if(client==null){toast("اتصل بالراوتر أولاً");return;}io.execute(()->{try{String path=backend.equals("User Manager 7")?"/user-manager/user/print":backend.equals("User Manager 6")?"/tool/user-manager/user/print":"/ip/hotspot/user/print";List<Map<String,String>> rows=client.print(path,".proplist=.id,name,username,profile,disabled,comment");runOnUiThread(()->{for(Map<String,String> r:rows){body.addView(text((r.getOrDefault("name",r.getOrDefault("username","—"))+"   "+r.getOrDefault("profile","—")),16,NAVY));}toast("تم تحميل "+rows.size()+" كرت");});}catch(Exception e){runOnUiThread(()->toast("تعذر تحميل الكروت: "+e.getMessage()));}});}
  EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(16);e.setPadding(18,10,18,10);e.setBackground(shape(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,58);p.setMargins(0,6,0,6);e.setLayoutParams(p);return e;}
  void showAdd(){base("إضافة الكروت");body.addView(text("إنشاء دفعة حقيقية على MikroTik",22,NAVY));EditText names=field("بادئة اسم المستخدم","sm");EditText count=field("عدد الكروت 1–50","10");EditText profile=field("البروفايل","default");body.addView(names);body.addView(count);body.addView(profile);Button preview=button("معاينة الدفعة",TEAL);preview.setOnClickListener(v->confirmAdd(names.getText().toString(),count.getText().toString(),profile.getText().toString()));body.addView(preview);space();Button pdf=button("تصدير الدفعة المؤكدة PDF",NAVY);pdf.setOnClickListener(v->exportBatch());body.addView(pdf);}
  void confirmAdd(String prefix,String count,String profile){int n;try{n=Integer.parseInt(count);if(n<1||n>50)throw new NumberFormatException();}catch(Exception e){toast("عدد الكروت يجب أن يكون 1 إلى 50");return;}if(!prefix.matches("[A-Za-z0-9_-]{1,24}")){toast("بادئة غير صالحة");return;}final int total=n;new AlertDialog.Builder(this).setTitle("تأكيد إنشاء الكروت").setMessage("سيتم إنشاء "+total+" كرت فعلياً على الراوتر.").setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->createBatch(prefix,total,profile)).show();}
  void createBatch(String prefix,int n,String profile){if(client==null){toast("اتصل بالراوتر أولاً");return;}if(creatingBatch){toast("توجد دفعة قيد التنفيذ");return;}if(backend.startsWith("User Manager")){toast("إنشاء User Manager يحتاج تحققاً من أوامر الإصدار والبروفايلات قبل التنفيذ");return;}creatingBatch=true;io.execute(()->{ArrayList<String> confirmed=new ArrayList<>();try{String path="/ip/hotspot/user/add";for(int i=1;i<=n;i++){String name=prefix+i;List<Map<String,String>> existing=client.print(path.replace("/add","/print"),"?name="+name);if(!existing.isEmpty())throw new IOException("اسم موجود مسبقاً: "+name);client.run(path,"=name="+name,"=password="+name,"=profile="+profile,"=comment=Smart Sanaa");confirmed.add(name);}lastBatch=confirmed;lastProfile=profile;runOnUiThread(()->{creatingBatch=false;toast("تم إنشاء "+confirmed.size()+" كرت مؤكداً");});}catch(Exception e){lastBatch=confirmed;lastProfile=profile;runOnUiThread(()->{creatingBatch=false;new AlertDialog.Builder(this).setTitle("توقفت الدفعة").setMessage(e.getMessage()+"\nالكروت المؤكدة: "+confirmed.size()+"\nافحص الراوتر قبل إعادة المحاولة").setPositiveButton("موافق",null).show();});}});}
  void exportBatch(){if(lastBatch.isEmpty()){toast("لا توجد كروت مؤكدة للتصدير");return;}Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("application/pdf");intent.putExtra(Intent.EXTRA_TITLE,"Smart-Sanaa-Vouchers.pdf");startActivityForResult(intent,PDF_REQUEST);}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=PDF_REQUEST||result!=RESULT_OK||data==null||data.getData()==null)return;try{VoucherPdf.write(getContentResolver(),data.getData(),lastBatch,lastProfile);toast("تم حفظ PDF");}catch(Exception e){toast("فشل تصدير PDF: "+e.getMessage());}}
  void showSessions(){base("الجلسات");body.addView(text("جلسات Hotspot وPPPoE النشطة",22,NAVY));Button b=button("↻ تحميل الجلسات",TEAL);b.setOnClickListener(v->loadSessions());body.addView(b);}
  void loadSessions(){
    if(client==null){toast("اتصل بالراوتر أولاً");return;}
    io.execute(()->{
      try{
        List<Map<String,String>> hs=client.print("/ip/hotspot/active/print");
        List<Map<String,String>> ppp=client.print("/ppp/active/print");
        runOnUiThread(()->{
          base("الجلسات النشطة");
          card("Hotspot",String.valueOf(hs.size()));
          for(Map<String,String> x:hs)card(x.getOrDefault("user","—"),x.getOrDefault("address","—")+"  "+x.getOrDefault("uptime",""));
          card("PPPoE",String.valueOf(ppp.size()));
          for(Map<String,String> x:ppp)card(x.getOrDefault("name","—"),x.getOrDefault("address","—")+"  "+x.getOrDefault("uptime",""));
        });
      }catch(Exception e){runOnUiThread(()->toast("خطأ قراءة الجلسات: "+e.getMessage()));}
    });
  }
  void showReports(){base("التقارير");body.addView(text("تقارير الاستخدام والكروت",22,NAVY));card("إجمالي User Manager",umCount<0?"غير متاح":String.valueOf(umCount));card("جلسات Hotspot",hotspotActive<0?"غير متاح":String.valueOf(hotspotActive));card("جلسات PPPoE",pppActive<0?"غير متاح":String.valueOf(pppActive));}
  void showExpired(){base("الكروت المنتهية");body.addView(text("راجع النتائج قبل التعطيل أو الحذف",22,NAVY));Button b=button("فحص الكروت المنتهية غير متاح بعد",MUTED);b.setOnClickListener(v->toast("يجب إكمال تحليل مدة الاستخدام والانتهاء الحقيقي لكل إصدار قبل التفعيل"));body.addView(b);card("User Manager","الكروت المنتهية");Button del=button("الحذف غير مفعل لحماية الكروت",MUTED);del.setOnClickListener(v->toast("يجب التحقق من الانتهاء وإنشاء Snapshot قبل تفعيل الحذف."));body.addView(del);body.addView(text("غير معروف = كرت لم يبدأ استخدامه. لا يتم حذفه.",14,MUTED));}
  void showMaintenance(){base("صيانة قاعدة البيانات");body.addView(text("أدوات User Manager الأصلية",22,NAVY));card("RouterOS",version);card("التخزين الحر على الراوتر",resource.getOrDefault("free-hdd-space","غير متاح"));body.addView(text("لا تُشغّل إعادة بناء قاعدة User Manager دون نسخة UMB صالحة واختبار الأوامر حسب إصدار الراوتر.",14,MUTED));Button rebuild=button("إعادة بناء قاعدة البيانات (غير مفعلة)",MUTED);rebuild.setOnClickListener(v->toast("لن تُنفّذ صيانة على راوتر حقيقي قبل التحقق من توافق إصدار RouterOS."));body.addView(rebuild);Button exp=button("فتح الكروت المنتهية",TEAL);exp.setOnClickListener(v->showExpired());body.addView(exp);}
  void showSettings(){
    base("اتصال MikroTik");
    body.addView(text("اتصال API مباشر بالشبكة المحلية",22,NAVY));
    body.addView(text("أدخل عنوان الراوتر MikroTik نفسه، وليس عنوان لوحة OpenWrt.",14,MUTED));
    EditText host=field("عنوان MikroTik IP",prefs.getString("host","10.11.0.1"));
    EditText user=field("اسم المستخدم",prefs.getString("user","admin"));
    EditText port=field("منفذ API",prefs.getString("port","8728"));
    EditText pass=field("كلمة المرور","");
    pass.setInputType(129);
    CheckBox tls=new CheckBox(this);tls.setText("اتصال TLS مشفر (API-SSL)");tls.setChecked(prefs.getBoolean("tls",false));tls.setTextColor(NAVY);
    body.addView(host);body.addView(port);body.addView(user);body.addView(pass);body.addView(tls);
    body.addView(text("افتراضياً API = 8728 و API-SSL = 8729. يمكن اختيار TLS بمعزل عن المنفذ.",13,MUTED));
    Button b=button("اتصال واكتشاف RouterOS",TEAL);
    b.setOnClickListener(v->{
      String address=host.getText().toString().trim();
      String username=user.getText().toString().trim();
      int numericPort;
      try{numericPort=Integer.parseInt(port.getText().toString().trim());if(numericPort<1||numericPort>65535)throw new NumberFormatException();}
      catch(NumberFormatException e){toast("المنفذ غير صالح");return;}
      if(address.isEmpty()||username.isEmpty()){toast("أدخل العنوان واسم المستخدم");return;}
      connect(address,numericPort,tls.isChecked(),username,pass.getText().toString());
    });
    body.addView(b);
  }
  void connect(String h,int port,boolean tls,String u,String p){
    io.execute(()->{
      RouterOsClient candidate=null;
      try{
        candidate=new RouterOsClient(h,port,tls,u,p);
        List<Map<String,String>> resourceInfo=candidate.print("/system/resource/print");
        if(resourceInfo.isEmpty())throw new IOException("RouterOS did not return system information");
        if(client!=null)client.close();
        client=candidate;
        prefs.edit().putString("host",h).putString("port",String.valueOf(port)).putString("user",u).putBoolean("tls",tls).apply();
        runOnUiThread(()->{toast("تم الاتصال بالراوتر");showDashboard();loadDashboard();});
      }catch(Exception e){
        if(candidate!=null&&candidate!=client)candidate.close();
        runOnUiThread(()->toast("تعذر الاتصال: "+e.getMessage()));
      }
    });
  }
  void confirm(String msg){new AlertDialog.Builder(this).setMessage(msg).setNegativeButton("إلغاء",null).setPositiveButton("متابعة",(d,w)->toast("العملية غير منفذة؛ يلزم التحقق من API أولاً" )).show();}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override protected void onDestroy(){io.shutdownNow();if(client!=null)client.close();super.onDestroy();}
}
