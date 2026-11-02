package com.smart.sanaa;

import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;

/** Minimal native RouterOS API client. Supports v6/v7 command paths and TLS. */
public final class RouterOsClient implements Closeable {
  private final Socket socket; private final InputStream in; private final OutputStream out; private int tag=1;
  public RouterOsClient(String host,int port,boolean tls,String user,String pass) throws Exception {
    Socket s=tls?((javax.net.ssl.SSLSocketFactory)javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(host,port):new Socket();
    if(!tls){s.connect(new InetSocketAddress(host,port),10000);} s.setSoTimeout(15000); socket=s;in=s.getInputStream();out=s.getOutputStream();login(user,pass);
  }
  private void word(String w) throws IOException {byte[] b=w.getBytes(StandardCharsets.UTF_8);int n=b.length;if(n<0x80)out.write(n);else if(n<0x4000){out.write((n>>8)|0x80);out.write(n&255);}else{out.write(0xE0|(n>>24));out.write(n>>16);out.write(n>>8);out.write(n);}out.write(b);}
  private String readWord() throws IOException {int c=in.read();if(c<0)throw new EOFException();int n;if((c&0x80)==0)n=c;else if((c&0xC0)==0x80)n=((c&0x3F)<<8)|in.read();else if((c&0xE0)==0xC0)n=((c&0x1F)<<16)|(in.read()<<8)|in.read();else if((c&0xF0)==0xE0)n=((c&0x0F)<<24)|(in.read()<<16)|(in.read()<<8)|in.read();else n=(in.read()<<24)|(in.read()<<16)|(in.read()<<8)|in.read();byte[] b=in.readNBytes(n);return new String(b,StandardCharsets.UTF_8);}
  private void send(String... words)throws IOException{for(String w:words)word(w);word("");out.flush();}
  private List<String> sentence()throws IOException{List<String> r=new ArrayList<>();while(true){String w=readWord();if(w.isEmpty())break;r.add(w);}return r;}
  private void login(String u,String p)throws Exception{send("/login","=name="+u,"=password="+p);List<String> r=sentence();String ret=null;for(String x:r)if(x.startsWith("=ret="))ret=x.substring(5);if(ret!=null){String md=md5Hex(new byte[]{0},hex(ret),p.getBytes(StandardCharsets.UTF_8));send("/login","=name="+u,"=response=00"+md);sentence();}}
  private static byte[] hex(String x){byte[]b=new byte[x.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(x.substring(i*2,i*2+2),16);return b;}
  private static String md5Hex(byte[]... parts)throws Exception{MessageDigest d=MessageDigest.getInstance("MD5");for(byte[]p:parts)d.update(p);StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format("%02x",b));return s.toString();}
  public synchronized void run(String path,String... args)throws Exception{String t="*"+(tag++);List<String>w=new ArrayList<>();w.add(path);w.addAll(Arrays.asList(args));w.add(".tag="+t);send(w.toArray(String[]::new));while(true){List<String> a=sentence();if(a.isEmpty())continue;String type=a.get(0);if("!done".equals(type))return;if("!trap".equals(type)||"!fatal".equals(type))throw new IOException("RouterOS: "+a);}}
  public synchronized String runReturning(String path,String... args)throws Exception{String t="*"+(tag++);List<String>w=new ArrayList<>();w.add(path);w.addAll(Arrays.asList(args));w.add(".tag="+t);send(w.toArray(String[]::new));String ret="";while(true){List<String>a=sentence();if(a.isEmpty())continue;String type=a.get(0);if("!trap".equals(type)||"!fatal".equals(type))throw new IOException("RouterOS: "+a);for(String x:a)if(x.startsWith("=ret="))ret=x.substring(5);if("!done".equals(type))return ret;}}
  public synchronized List<Map<String,String>> print(String path,String... args)throws Exception{String t="*"+(tag++);List<String>w=new ArrayList<>();w.add(path);w.addAll(Arrays.asList(args));w.add(".tag="+t);send(w.toArray(String[]::new));List<Map<String,String>> rows=new ArrayList<>();while(true){List<String> s=sentence();if(s.isEmpty())continue;String type=s.get(0);if("!done".equals(type))break;if("!trap".equals(type)||"!fatal".equals(type))throw new IOException("RouterOS: "+s);if("!re".equals(type)){Map<String,String>m=new LinkedHashMap<>();for(int i=1;i<s.size();i++){String x=s.get(i);if(x.startsWith("=")){int k=x.indexOf('=',1);if(k>0)m.put(x.substring(1,k),x.substring(k+1));}}rows.add(m);}}return rows;}
  public synchronized long count(String path)throws Exception{String t="*"+(tag++);send(path,"=count-only=",".tag="+t);while(true){List<String>a=sentence();if(a.isEmpty())continue;String type=a.get(0);if("!trap".equals(type)||"!fatal".equals(type))throw new IOException("RouterOS count failed: "+a);for(String x:a)if(x.startsWith("=ret="))return Long.parseLong(x.substring(5));if("!done".equals(type))throw new IOException("RouterOS count result missing");}}
  public void close(){try{socket.close();}catch(Exception ignored){}}
}
