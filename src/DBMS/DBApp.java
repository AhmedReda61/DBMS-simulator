package DBMS;

import java.io.IOException;
import java.util.*;

public class DBApp
{
	static int dataPageSize = 2;
	static int indexPageSize = 5;
	
	public static void createTable(String tableName, String[] columnsNames)
	{
		FileManager.storeTable(tableName, new Table(tableName, columnsNames));
	}
	
	public static void insert(String tableName, String[] record)
	{

		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);
		int pageNumber = t.getLastPage();
		Page p;

		if (pageNumber == -1 || FileManager.loadTablePage(tableName, pageNumber).getPageSize() == dataPageSize){
			pageNumber++;
			p = new Page(pageNumber);
		}else{
			p = FileManager.loadTablePage(tableName, pageNumber);
		}
		t.addRow();
		t.setLastPage(pageNumber);
		p.addRecord(record);

		long endTime = System.nanoTime();

		t.insert(record, (endTime - startTime) / 1000000);
		FileManager.storeTable(tableName, t);
		FileManager.storeTablePage(tableName, pageNumber, p);
	}
	
	public static ArrayList<String []> select(String tableName)
	{

		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = new ArrayList<>();

		for (int i = 0; i <= t.getLastPage(); i++) {
			Page p = FileManager.loadTablePage(tableName, i);
			res.addAll(p.getRecords());
		}

		long endTime = System.nanoTime();

		t.selectAll("all", (endTime - startTime) / 1000000);
		FileManager.storeTable(tableName, t);

		return res;
	}
	
	public static ArrayList<String []> select(String tableName, int pageNumber, int recordNumber)
	{

		long startTime = System.nanoTime();

		ArrayList<String []> res = new ArrayList<>();
		Table t = FileManager.loadTable(tableName);
		if (pageNumber >= 0 && pageNumber <= t.getLastPage()) {
			Page p = FileManager.loadTablePage(tableName, pageNumber);
			if (p != null && recordNumber >=0 && recordNumber < p.getRecords().size()) {
				res.add(FileManager.loadTablePage(tableName, pageNumber).getRecord(recordNumber));
			}
		}

		long endTime = System.nanoTime();

		t.selectPointer("pointer", (endTime - startTime) / 1000000, pageNumber, recordNumber);
		FileManager.storeTable(tableName, t);

		return res;
	}
	
	public static ArrayList<String []> select(String tableName, String[] cols, String[] vals)
	{

		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);
		ArrayList<String []> res = new ArrayList<>();

		HashMap<String , String> map = new HashMap<>();

		for(int i = 0; i < cols.length; i++){
			map.put(cols[i], vals[i]);
		}

		ArrayList<Integer> arr = new ArrayList<>();

		String[] columns = t.getColumns();

		for(int i = 0; i < columns.length; i++){
			if (map.containsKey(columns[i])){
				arr.add(i);
			}
		}

		int[] pages = new int[t.getLastPage() + 1];

		for (int i = 0; i < pages.length; i++) {
			Page p = FileManager.loadTablePage(tableName, i);
			for(String[] row : p.getRecords()) {
				boolean ok = true;
				for (int j : arr) {
					if (!row[j].equals(map.get(columns[j]))) {
						ok = false;
						break;
					}
				}
				if (ok) {
					res.add(row);
					pages[i]++;
				}
			}
		}

		long endTime = System.nanoTime();

		String op = "[";
		for (int i = 0; i + 1 < cols.length; i++) {
			op += cols[i] + ", ";
		}
		op += cols[cols.length - 1] + "]->[";

		for (int i = 0; i + 1 < vals.length; i++) {
			op += vals[i] + ", ";
		}
		op += vals[vals.length-1] + "], Records per page:[";

		boolean first = true;
		for (int i = 0; i < pages.length; i++) {
			if (pages[i] == 0)continue;
			if (first){
				op += "[" + i + ", " + pages[i] + "]";
				first = false;
			}else{
				op += ", [" + i + ", " + pages[i] + "]";
			}
		}
		op += "], records:" + res.size() + ", execution time (mil):" + ( (endTime - startTime) / 1000000 );

		t.selectCondition("condition", op);
		FileManager.storeTable(tableName, t);

		return res;
	}


	// -----------------MS2----------------------

	public static ArrayList<String []> validateRecords(String tableName){



		return null;
	}

	public static void recoverRecords(String tableName, ArrayList<String[]> missing){

	}

	public static void createBitMapIndex(String tableName, String colName){

	}

	public static String getValueBits(String tableName, String colName, String value){
		return null;
	}

	public static ArrayList<String []> selectIndex(String tableName, String[] cols, String[] vals){
		return null;
	}

	// -----------------MS3----------------------

	public static void createDenseIndex(String tableName, String colName){

		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);

		int colNumber = 0;

		for (int i = 0; i < t.getColumns().length; i++) {
			if (t.getColumns()[i].equals(colName)) {
				colNumber = i;
				break;
			}
		}

		ArrayList<pair> pairs = new ArrayList<>();

		for (int i = 0; i <= t.getLastPage(); i++) {
			Page p = FileManager.loadTablePage(tableName, i);

			for (int j = 0; j < p.getPageSize(); j++) {
				pointer pointer = new pointer(i , j);
				pair pair = new pair(p.getRecord(j)[colNumber] , pointer);
				pairs.add(pair);
			}

		}

		Collections.sort(pairs , (a,b)->{
			return a.key.compareTo(b.key);
		});


		int blockNumber = 0;
		DenseIndexBlock block = new DenseIndexBlock();

        for (DBMS.pair pair : pairs) {
            if (!block.insert(pair)) {
                FileManager.storeIndexBlock(tableName, colName, blockNumber, block);
                blockNumber++;
                block = new DenseIndexBlock();
                block.insert(pair);
            }
        }
		FileManager.storeIndexBlock(tableName,colName,blockNumber,block);

		long endTime = System.nanoTime();

		long time = (endTime - startTime) / 1000000;

		String op = "Dense Index created ont Table: \"" + tableName + "\" on column: \"" + colName
				+ "\", execution time (mil):" + time;

		t.getOps().add(op);
		FileManager.storeTable(tableName, t);
	}

	public static String getIndexRepresentation(String tableName, String colName){
		StringBuilder out = new StringBuilder("[");

		Table t = FileManager.loadTable(tableName);

		int rows = t.getRows();

		int numberOfBlocks = (rows + indexPageSize - 1) / indexPageSize;

		for (int i = 0; i < numberOfBlocks ; i++) {
			DenseIndexBlock block = FileManager.loadIndexBlock(tableName, colName, i);

			if (block == null)continue;

			out.append(block.toString());

			out.append(i == numberOfBlocks-1 ? "]" : ", ");

		}

		return out.toString();
	}

	// ------------------------------------------

	public static String getFullTrace(String tableName)
	{
		StringBuilder res = new StringBuilder();

		Table t = FileManager.loadTable(tableName);

		for(String op : t.getOps()){
			res.append(op).append("\n");
		}

		res.append("Pages Count: ").append(t.getLastPage() + 1).append(", Records Count: ").append(t.getRows());

		return res.toString();
	}

	public static String getLastTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		return t.getOps().getLast();
	}


	public static void main(String []args) throws IOException
	{

		FileManager.reset();
		String[] cols = {"id","name","major","semester","gpa"};
		createTable("student", cols);
		String[] r1 = {"1", "stud1", "CS", "5", "0.9"};
		insert("student", r1);
		String[] r2 = {"2", "stud2", "BI", "7", "1.2"};
		insert("student", r2);
		String[] r3 = {"3", "stud3", "CS", "2", "2.4"};
		insert("student", r3);
		String[] r4 = {"4", "stud4", "DMET", "9", "1.2"};
		insert("student", r4);
		String[] r5 = {"5", "stud5", "BI", "4", "3.5"};
		insert("student", r5);

		System.out.println("--------------------------------");
		System.out.println("Full Trace of the table:");
		System.out.println(getFullTrace("student"));
		System.out.println("--------------------------------");
		System.out.println("Last Trace of the table:");
		System.out.println(getLastTrace("student"));
		System.out.println("--------------------------------");
		System.out.println("The trace of the Tables Folder:");
		System.out.println(FileManager.trace());




//		FileManager.reset();
//		System.out.println("--------------------------------");
//		System.out.println("The trace of the Tables Folder after resetting:");
//		System.out.println(FileManager.trace());

	}

}
