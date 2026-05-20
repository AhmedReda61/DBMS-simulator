package DBMS;

import java.io.Serializable;
import java.util.HashMap;

public class BitmapIndex implements Serializable {

    HashMap<String , StringBuilder> map;
    int rows;

    BitmapIndex(){
        map = new HashMap<>();
        rows = 0;
    }

    public void insert(String value){
        if (!map.containsKey(value)){
            map.put(value , new StringBuilder());
            for (int i = 0; i < rows; i++) {
                map.get(value).append("0");
            }
        }
        for (var entry : map.entrySet()){
            if (entry.getKey().equals(value)){
                entry.getValue().append("1");
            }else{
                entry.getValue().append("0");
            }
        }
        rows++;
    }

    public String get(String value){
        if (!map.containsKey(value)){
            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i < rows; i++) {
                zeros.append("0");
            }
            return zeros.toString();
        }
        return map.get(value).toString();
    }

}
