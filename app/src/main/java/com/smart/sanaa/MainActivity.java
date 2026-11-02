package com.smart.sanaa;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;import java.util.*;import java.util.concurrent.*;import java.io.IOException;

/** Native Smart Manager application. UI is backed by real RouterOS API calls. */
public class MainActivity extends Activity {
  final int NAVY=Color.rgb(16,42,67),TEAL=Color.rgb(21,155,168),MUTED=Color.rgb(107,124,147),SURFACE=Color.rgb(244,248,251),DANGER=Color.rgb(214,69,85); LinearLayout body; SharedPreferences prefs; ExecutorService io=Executors.newSingleThreadExecutor(); RouterOsClient client; String backend="unknown",version="—"; Map<String,String> resource=new HashMap<>(); int hotspotActive=-1,pppActive=-1,umCount=-1; boolean refreshing=false,creatingBatch=false; ArrayList<String> lastBatch=new ArrayList<>(); String lastProfile="",batchTarget="Hotspot",batchOwner="admin"; static final int PDF_REQUEST=408;
  @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("router",0);showDashboard();}
  TextView text(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(18,12,18,12);t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);return t;}
  GradientDrawable shape(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
  Button button(String s,int c){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(c,28));return b;}
  void base(String title){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(prefs.getBoolean("dark",false)?Color.rgb(12,25,41):SURFACE);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);LinearLayout head=new LinearLayout(this);head.setPadding(16,18,16,8);TextView h=text(title,24,prefs.getBoolean("dark",false)?Color.WHITE:NAVY);h.setTypeface(null,1);head.addView(h,new LinearLayout.LayoutParams(0,66,1));Button menu=button("☰",TEAL);head.addView(menu,new LinearLayout.LayoutParams(62,58));menu.setOnClickListener(v->{PopupMenu pm=new PopupMenu(this,menu);String[] items={"الرئيسية","إضافة كروت","الكروت","الكروت المنتهية","الجلسات","التقارير","صيانة قاعدة البيانات","الإعدادات","الوضع الليلي / النهاري"};for(String item:items)pm.getMenu().add(item);pm.setOnMenuItemClickListener(item->{String choice=item.getTitle().toString();switch(choice){case "الرئيسية":showDashboard();break;case "إضافة كروت":showAdd();break;case "الكروت":showCards();break;case "الكروت المنتهية":showExpired();break;case "الجلسات":showSessions();break;case "التقارير":showReports();break;case "صيانة قاعدة البيانات":showMaintenance();break;case "الإعدادات":showSettings();break;default:prefs.edit().putBoolean("dark",!prefs.getBoolean("dark",false)).apply();showDashboard();}return true;});pm.show();});root.addView(head);ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(18,8,18,130);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav());setContentView(root);}
  LinearLayout nav(){LinearLayout n=new LinearLayout(this);n.setPadding(5,6,5,6);String[] a={"الرئيسية","الكروت","الجلسات","التقارير","الإعدادات"};for(String x:a){Button b=button(x,Color.WHITE);b.setTextColor(NAVY);b.setTextSize(11);b.setOnClickListener(v->{switch(x){case "الرئيسية":showDashboard();break;case "الكروت":showCards();break;case "الجلسات":showSessions();break;case "التقارير":showReports();break;case "الصيانة":showMaintenance();break;default:showSettings();}});n.addView(b,new LinearLayout.LayoutParams(0,60,1));}return n;}
  void space(){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,12));}
  void card(String h,String v){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(10,8,10,10);c.setBackground(shape(prefs.getBoolean("dark",false)?Color.rgb(26,49,74):Color.WHITE,24));c.addView(text(h,14,TEAL));TextView v1=text(v,23,prefs.getBoolean("dark",false)?Color.WHITE:NAVY);v1.setTypeface(null,1);c.addView(v1);body.addView(c);space();}
  LinearLayout tile(String label,String value,int tint){
    LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
    tile.setPadding(9,14,9,14);tile.setBackground(shape(prefs.getBoolean("dark",false)?Color.rgb(26,49,74):Color.WHITE,22));
    TextView name=text(label,12,tint);name.setGravity(Gravity.CENTER);tile.addView(name);
    TextView number=text(value,17,prefs.getBoolean("dark",false)?Color.WHITE:NAVY);number.setTypeface(null,1);number.setGravity(Gravity.CENTER);tile.addView(number);
    return tile;
  }
  void tiles(String[] names,String[] values,int[] tints){
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
    for(int i=0;i<names.length;i++){
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(4,4,4,4);
      row.addView(tile(names[i],values[i],tints[i]),p);
    }
    body.addView(row);space();
  }
  void showDashboard(){
    base("الرئيسية");
    tiles(new String[]{"RouterOS","CPU","وقت التشغيل"},new String[]{version,resource.getOrDefault("cpu-load","—")+"%",resource.getOrDefault("uptime","—")},new int[]{Color.rgb(17,110,202),Color.rgb(118,70,219),Color.rgb(0,151,89)});
    LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(19,22,19,24);
    hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.rgb(6,35,78),Color.rgb(2,86,139),Color.rgb(4,28,68)}));
    TextView brand=text("سمارت صنعاء",27,Color.WHITE);brand.setTypeface(null,1);hero.addView(brand);
    hero.addView(text("MikroTik Network Management",14,Color.WHITE));
    hero.addView(text("RouterOS 6 / 7 • إدارة الشبكات المحلية",13,Color.rgb(164,234,248)));body.addView(hero);space();
    String[] quickNames={"الرئيسية","إضافة كروت","عمليات الكروت"};
    LinearLayout shortcuts=new LinearLayout(this);
    for(String n:quickNames){Button btn=button(n,Color.rgb(226,246,250));btn.setTextColor(NAVY);btn.setTextSize(13);
      btn.setOnClickListener(v->{if(n.equals("إضافة كروت"))showAdd();else if(n.equals("عمليات الكروت"))showCards();else showDashboard();});
      LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,70,1);lp.setMargins(4,4,4,4);shortcuts.addView(btn,lp);
    }
    body.addView(shortcuts);space();
    tiles(new String[]{"Hotspot النشط","PPPoE النشط"},new String[]{hotspotActive<0?"—":String.valueOf(hotspotActive),pppActive<0?"—":String.valueOf(pppActive)},new int[]{TEAL,Color.rgb(119,84,211)});
    card("إجمالي كروت User Manager",umCount<0?"غير متاح":String.valueOf(umCount));
    body.addView(text("الوصول السريع",19,prefs.getBoolean("dark",false)?Color.WHITE:NAVY));
    String[] routes={"الباقات","الجلسات","الكروت المنتهية","صيانة User Manager"};
    for(int i=0;i<routes.length;i+=2){
      LinearLayout row=new LinearLayout(this);
      for(int j=i;j<Math.min(i+2,routes.length);j++){
        String name=routes[j];Button link=button("›  "+name,Color.rgb(20,131,168));link.setTextSize(13);
        link.setOnClickListener(v->{switch(name){case "الباقات":showCards();break;case "الجلسات":showSessions();break;case "الكروت المنتهية":showExpired();break;default:showMaintenance();}});
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,74,1);p.setMargins(4,4,4,4);row.addView(link,p);
      }
      body.addView(row);
    }
    space();card("معلومات النظام",resource.getOrDefault("board-name","—")+" | "+resource.getOrDefault("architecture-name","—")+"\nCPU: "+resource.getOrDefault("cpu-load","—")+"% | "+resource.getOrDefault("uptime","—")+"\nالذاكرة الحرة: "+resource.getOrDefault("free-memory","—")+"\nالتخزين الحر: "+resource.getOrDefault("free-hdd-space","—"));
    Button action=button(client==null?"اتصال بالراوتر":"↻ تحديث من الراوتر",TEAL);
    action.setOnClickListener(v->{if(client==null)showSettings();else loadDashboard();});body.addView(action);
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
  void loadUsers(){if(client==null){toast("اتصل بالراوتر أولاً");return;}io.execute(()->{try{String path=backend.equals("User Manager 7")?"/user-manager/user/print":backend.equals("User Manager 6")?"/tool/user-manager/user/print":"/ip/hotspot/user/print";List<Map<String,String>> rows=client.print(path,"=.proplist=.id,name,username,profile,disabled,comment");runOnUiThread(()->{for(Map<String,String> r:rows){body.addView(text((r.getOrDefault("name",r.getOrDefault("username","—"))+"   "+r.getOrDefault("profile","—")),16,NAVY));}toast("تم تحميل "+rows.size()+" كرت");});}catch(Exception e){runOnUiThread(()->toast("تعذر تحميل الكروت: "+e.getMessage()));}});}
  EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextSize(16);e.setPadding(18,10,18,10);e.setBackground(shape(Color.WHITE,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,58);p.setMargins(0,6,0,6);e.setLayoutParams(p);return e;}
  void showAdd(){base("إضافة الكروت");body.addView(text("دفعة MikroTik جديدة",22,NAVY));Spinner mode=new Spinner(this);ArrayAdapter<String> modes=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Hotspot","User Manager"});mode.setAdapter(modes);mode.setSelection(batchTarget.equals("Hotspot")?0:1);mode.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> parent){} public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){batchTarget=position==0?"Hotspot":"User Manager";}});body.addView(mode);EditText names=field("بادئة اسم المستخدم","sm");EditText count=field("عدد الكروت 1–50","10");EditText profile=field("البروفايل","default");EditText owner=field("مالك User Manager 6 (customer)","admin");body.addView(names);body.addView(count);body.addView(profile);body.addView(owner);Button preview=button("معاينة الدفعة",TEAL);preview.setOnClickListener(v->{batchOwner=owner.getText().toString().trim();confirmAdd(names.getText().toString(),count.getText().toString(),profile.getText().toString());});body.addView(preview);space();Button pdf=button("تصدير الدفعة المؤكدة PDF",NAVY);pdf.setOnClickListener(v->exportBatch());body.addView(pdf);}
  void confirmAdd(String prefix,String count,String profile){int n;try{n=Integer.parseInt(count);if(n<1||n>50)throw new NumberFormatException();}catch(Exception e){toast("عدد الكروت يجب أن يكون 1 إلى 50");return;}if(!prefix.matches("[A-Za-z0-9_-]{1,24}")){toast("بادئة غير صالحة");return;}final int total=n;new AlertDialog.Builder(this).setTitle("تأكيد إنشاء الكروت").setMessage("سيتم إنشاء "+total+" كرت فعلياً على الراوتر.").setNegativeButton("إلغاء",null).setPositiveButton("إنشاء",(d,w)->createBatch(prefix,total,profile)).show();}
  void createBatch(String prefix,int n,String profile){if(client==null){toast("اتصل بالراوتر أولاً");return;}if(creatingBatch){toast("توجد دفعة قيد التنفيذ");return;}if(batchTarget.equals("User Manager")&&!backend.startsWith("User Manager ")){toast("لم يتم اكتشاف User Manager على الراوتر");return;}creatingBatch=true;io.execute(()->{ArrayList<String> confirmed=new ArrayList<>();try{boolean um=batchTarget.equals("User Manager");boolean um6=um&&backend.equals("User Manager 6");String path=um6?"/tool/user-manager/user/add":um?"/user-manager/user/add":"/ip/hotspot/user/add";String profilePath=um6?"/tool/user-manager/profile/print":um?"/user-manager/profile/print":"/ip/hotspot/user/profile/print";if(client.print(profilePath,"?name="+profile).isEmpty())throw new IOException("البروفايل غير موجود: "+profile);for(int i=1;i<=n;i++){String name=prefix+i;List<Map<String,String>> existing=client.print(path.replace("/add","/print"),(um6?"?username=":"?name=")+name);if(!existing.isEmpty())throw new IOException("اسم موجود مسبقاً: "+name);if(um6){String internalId=client.runReturning(path,"=customer="+batchOwner,"=username="+name,"=password="+name,"=shared-users=1","=disabled=no","=comment=Smart Sanaa");client.run("/tool/user-manager/user/create-and-activate-profile","=customer="+batchOwner,"=numbers="+(internalId.isEmpty()?name:internalId),"=profile="+profile);}else if(um){client.run(path,"=name="+name,"=password="+name,"=comment=Smart Sanaa");client.run("/user-manager/user-profile/add","=user="+name,"=profile="+profile);}else{client.run(path,"=name="+name,"=password="+name,"=profile="+profile,"=comment=Smart Sanaa");}confirmed.add(name);}lastBatch=confirmed;lastProfile=profile;runOnUiThread(()->{creatingBatch=false;toast("تم إنشاء "+confirmed.size()+" كرت مؤكداً");});}catch(Exception e){lastBatch=confirmed;lastProfile=profile;runOnUiThread(()->{creatingBatch=false;new AlertDialog.Builder(this).setTitle("توقفت الدفعة").setMessage(e.getMessage()+"\nالكروت المؤكدة: "+confirmed.size()+"\nافحص الراوتر قبل إعادة المحاولة").setPositiveButton("موافق",null).show();});}});}
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
  void showExpired(){
    base("الكروت المنتهية");
    body.addView(text("فحص حسب بيانات الاستخدام الفعلية من RouterOS",19,NAVY));
    body.addView(text("الكروت التي لم تبدأ الاستخدام لا تدخل ضمن النتائج.",14,MUTED));
    Button scan=button("↻ فحص الكروت المنتهية",TEAL);scan.setOnClickListener(v->loadExpired());body.addView(scan);
    body.addView(text("الحذف غير مفعل حتى تُراجع النتائج وتختبر سلامته على إصدار الراوتر.",13,MUTED));
  }
  void loadExpired(){
    if(client==null){toast("اتصل بالراوتر أولاً");return;}
    io.execute(()->{
      try{
        List<Map<String,String>> expired=ExpiredScanner.scan(client,backend);
        runOnUiThread(()->{
          showExpired();card("عدد الكروت المنتهية",String.valueOf(expired.size()));
          int limit=Math.min(100,expired.size());
          for(int i=0;i<limit;i++){
            Map<String,String> r=expired.get(i);
            card(r.getOrDefault("name",r.getOrDefault("username","—")),"معرف: "+r.getOrDefault(".id","—"));
          }
          if(expired.size()>limit)body.addView(text("تظهر أول "+limit+" نتيجة فقط لتخفيف الحمل على الهاتف.",14,MUTED));
        });
      }catch(Exception e){runOnUiThread(()->toast("تعذر فحص المنتهية: "+e.getMessage()));}
    });
  }
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
