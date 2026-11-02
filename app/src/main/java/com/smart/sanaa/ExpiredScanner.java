package com.smart.sanaa;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only expiration detection matching legacy Smart Manager semantics. */
public final class ExpiredScanner {
    private ExpiredScanner() {}
    public static long seconds(String value) {
        if(value==null||value.isEmpty())return 0;
        String text=value.toLowerCase(Locale.ROOT).trim();
        if(text.matches("[0-9]+:[0-9]{2}:[0-9]{2}")){
            String[] parts=text.split(":");
            return Long.parseLong(parts[0])*3600+Long.parseLong(parts[1])*60+Long.parseLong(parts[2]);
        }
        Matcher matcher=Pattern.compile("(\\d+)(w|d|h|m|s)").matcher(text);
        long total=0;
        while(matcher.find()){
            long amount=Long.parseLong(matcher.group(1));
            switch(matcher.group(2)){
                case "w":total+=amount*604800;break;
                case "d":total+=amount*86400;break;
                case "h":total+=amount*3600;break;
                case "m":total+=amount*60;break;
                case "s":total+=amount;break;
            }
        }
        return total;
    }
    public static boolean expiredUm6(Map<String,String> row){
        return row.getOrDefault("actual-profile","").trim().isEmpty()
            && seconds(row.get("uptime-used"))>0;
    }
    public static boolean expiredHotspot(Map<String,String> row){
        long limit=seconds(row.get("limit-uptime"));
        return limit>0&&seconds(row.get("uptime"))>=limit;
    }
    public static List<Map<String,String>> scan(RouterOsClient client,String backend)throws Exception{
        List<Map<String,String>> results=new ArrayList<>();
        if("User Manager 6".equals(backend)){
            for(Map<String,String> row:client.print("/tool/user-manager/user/print","=.proplist=.id,username,actual-profile,uptime-used"))
                if(expiredUm6(row))results.add(row);
        }else if("User Manager 7".equals(backend)){
            Map<String,Integer> states=new HashMap<>();
            for(Map<String,String> profile:client.print("/user-manager/user-profile/print","=.proplist=user,state")){
                String user=profile.getOrDefault("user","");
                int bit="used".equalsIgnoreCase(profile.getOrDefault("state","").trim())?1:2;
                states.put(user,states.getOrDefault(user,0)|bit);
            }
            for(Map<String,String> row:client.print("/user-manager/user/print","=.proplist=.id,name")){
                int flags=states.getOrDefault(row.getOrDefault("name",""),states.getOrDefault(row.getOrDefault(".id",""),0));
                if(flags==1)results.add(row);
            }
        }else{
            for(Map<String,String> row:client.print("/ip/hotspot/user/print","=.proplist=.id,name,limit-uptime,uptime"))
                if(expiredHotspot(row))results.add(row);
        }
        return results;
    }
}
