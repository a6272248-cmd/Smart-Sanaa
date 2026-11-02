package com.smart.sanaa;

import org.junit.Test;
import static org.junit.Assert.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline fake RouterOS socket; never connects to a real router. */
public class RouterOsClientTest {
    static void word(OutputStream out,String value)throws IOException{
        byte[] b=value.getBytes(StandardCharsets.UTF_8);
        if(b.length>=128)throw new IOException("Test word too long");
        out.write(b.length);out.write(b);
    }
    static void reply(OutputStream out,String... values)throws IOException{
        for(String s:values)word(out,s);
        word(out,"");out.flush();
    }
    static List<String> request(InputStream in)throws IOException{
        List<String> words=new ArrayList<>();
        while(true){
            int n=in.read();if(n<0)throw new EOFException();
            byte[] b=new byte[n];new DataInputStream(in).readFully(b);
            if(n==0)return words;
            words.add(new String(b,StandardCharsets.UTF_8));
        }
    }
    @Test public void logsInAndCountsWithoutRealRouter()throws Exception{
        try(ServerSocket server=new ServerSocket(0)){
            server.setSoTimeout(4000);
            Thread thread=new Thread(()->{
                try(Socket peer=server.accept()){
                    peer.setSoTimeout(4000);
                    List<String> login=request(peer.getInputStream());
                    assertEquals("/login",login.get(0));
                    reply(peer.getOutputStream(),"!done");
                    List<String> count=request(peer.getInputStream());
                    assertEquals("/ip/hotspot/active/print",count.get(0));
                    assertTrue(count.contains("=count-only="));
                    reply(peer.getOutputStream(),"!done","=ret=7");
                }catch(Exception ex){throw new RuntimeException(ex);}
            });
            thread.start();
            try(RouterOsClient api=new RouterOsClient("127.0.0.1",server.getLocalPort(),false,"admin","test")){
                assertEquals(7L,api.count("/ip/hotspot/active/print"));
            }
            thread.join(4000);
            assertFalse("Server thread did not terminate",thread.isAlive());
        }
    }
    @Test public void rejectsAuthenticationFailure()throws Exception{
        try(ServerSocket server=new ServerSocket(0)){
            server.setSoTimeout(4000);
            Thread thread=new Thread(()->{
                try(Socket peer=server.accept()){
                    peer.setSoTimeout(4000);
                    request(peer.getInputStream());
                    reply(peer.getOutputStream(),"!trap","=message=invalid user");
                }catch(Exception ex){throw new RuntimeException(ex);}
            });
            thread.start();
            try{
                new RouterOsClient("127.0.0.1",server.getLocalPort(),false,"admin","wrong");
                fail("Invalid authentication should fail");
            }catch(IOException expected){
                assertTrue(expected.getMessage().contains("authentication"));
            }
            thread.join(4000);
        }
    }
}
