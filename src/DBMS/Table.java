package DBMS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Table implements Serializable
{
    private String name;
	private String[] columns;
    private int rows;
    private int lastPage;

    private ArrayList<String> ops;

    public Table (String name, String[] columns){
        this.name = name;
        this.columns = columns;
        rows = 0;
        lastPage = -1;

        ops = new ArrayList<>();

        String op = "Table created name:" + name + ", columnsNames:[";

        for (int i = 0; i+1 < columns.length; i++) {
            op += columns[i] + ", ";
        }
        op += columns[columns.length-1] + "]";
        ops.add(op);
    }

    public void insert(String[] record, long time){
        String op = "Inserted:[";

        for (int i = 0; i+1 < record.length; i++) {
            op += record[i] + ", ";
        }
        op += record[record.length-1] + "]";
        op += ", at page number:" + lastPage + ", execution time (mil):" + time;
        ops.add(op);
    }

    public void selectAll(String type, long time){
        String op = "Select " + type;
        op += " pages:" + (lastPage+1) + ", records:" + rows + ", execution time (mil):" + time;
        ops.add(op);
    }

    public void selectPointer(String type, long time, int page, int record){
        String op = "Select " + type;
        op += " page:" + page + ", record:" + record + ", total output count:1, execution time (mil):" + time;
        ops.add(op);
    }

    public void selectCondition(String type, String condition){
        String op = "Select " + type;
        op += ":" + condition;
        ops.add(op);
    }

    public ArrayList<String> getOps(){
        return ops;
    }

    public String getName(){
        return name;
    }

    public String[] getColumns(){
        return columns;
    }

    public int getRows(){
        return rows;
    }

    public void addRow(){
        rows++;
    }

    public int getLastPage(){
        return lastPage;
    }

    public void setLastPage(int lastPage){
        this.lastPage = lastPage;
    }

}
