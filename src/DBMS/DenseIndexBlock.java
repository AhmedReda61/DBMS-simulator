package DBMS;


import java.io.Serializable;
import java.util.ArrayList;

public class DenseIndexBlock implements Serializable {

    ArrayList<pair> block;

    DenseIndexBlock() {
        block = new ArrayList<pair>();
    }

    public boolean isFull(){
        return block.size() == DBApp.indexPageSize;
    }

    public boolean insert(pair pair) {
        if (isFull())return false;
        block.add(pair);
        return true;
    }


    public String toString(){
        StringBuilder out = new StringBuilder("[");

        for (int i = 0; i < block.size(); i++) {
            out.append(block.get(i).toString()).append(i == block.size() - 1 ? "]" : ", ");
        }
        return out.toString();
    }

}


class pointer implements Serializable {
    public int pageNumber;
    public int offset;

    pointer(int pageNumber, int offset){
        this.pageNumber = pageNumber;
        this.offset = offset;
    }

    public String toString(){
        return "r" + offset + "@p" + pageNumber;
    }

}

class pair implements Serializable {
    String key;
    pointer pointer;

    pair(String key,pointer pointer){
        this.key = key;
        this.pointer = pointer;
    }

    public String toString(){
        return "(" + key + ", " + pointer.toString() + ")";
    }

}