package DBMS;

import java.io.Serializable;
import java.util.ArrayList;

public class Page implements Serializable
{
    private int pageNumber;
	private ArrayList<String []> rows;

    public Page(int pageNumber){
        this.pageNumber = pageNumber;
        rows = new ArrayList<>();
    }

    public void addRecord(String[] record){
        rows.add(record);
    }

    public String[] getRecord(int index){
        return rows.get(index);
    }

    public int getPageNumber(){
        return pageNumber;
    }

    public int getPageSize(){
        return rows.size();
    }

    public ArrayList<String[]> getRecords(){
        return rows;
    }
}
