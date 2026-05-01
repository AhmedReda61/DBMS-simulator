package DBMS;

import java.io.IOException;
import java.util.*;

public class DBApp
{
	static int dataPageSize = 2;
	
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
	
	public static String getFullTrace(String tableName)
	{
		String res = "";

		Table t = FileManager.loadTable(tableName);

		for(String op : t.getOps()){
			res += op + "\n";
		}

		res += "Pages Count: " + (t.getLastPage()+1) + ", Records Count: " + t.getRows();

		return res;
	}
	
	public static String getLastTrace(String tableName)
	{
		Table t = FileManager.loadTable(tableName);
		return t.getOps().getLast();
	}


	public static void main(String []args) throws IOException
	{

//		String[] cols = {"id","name","major","semester","gpa"};
//		createTable("student", cols);
//		String[] r1 = {"1", "stud1", "CS", "5", "0.9"};
//		insert("student", r1);
//		String[] r2 = {"2", "stud2", "BI", "7", "1.2"};
//		insert("student", r2);
//		String[] r3 = {"3", "stud3", "CS", "2", "2.4"};
//		insert("student", r3);
//		String[] r4 = {"4", "stud4", "DMET", "9", "1.2"};
//		insert("student", r4);
//		String[] r5 = {"5", "stud5", "BI", "4", "3.5"};
//		insert("student", r5);
//
//		System.out.println("Output of selecting the whole table content:");
//		ArrayList<String[]> result1 = select("student");
//		for (String[] array : result1) {
//			for (String str : array) {
//				System.out.print(str + " ");
//			}
//			System.out.println();
//		}
//		System.out.println("----------------------------------------");
//
//		System.out.println("Output of selecting the output by position:");
//		ArrayList<String[]> result2 = select("student", 1, 1);
//		for (String[] array : result2) {
//			for (String str : array) {
//				System.out.print(str + " ");
//			}
//			System.out.println();
//		}
//		System.out.println("----------------------------------------");
//
//		System.out.println("Output of selecting the output by column condition:");
//		ArrayList<String[]> result3 = select("student", new String[]{"gpa"}, new String[]{"1.2"});
//		for (String[] array : result3) {
//			for (String str : array) {
//				System.out.print(str + " ");
//			}
//			System.out.println();
//		}
//		System.out.println("----------------------------------------");
//
//		System.out.println("Full Trace of the table:");
//		System.out.println(getFullTrace("student"));
//		System.out.println("----------------------------------------");
//
//		System.out.println("Last Trace of the table:");
//		System.out.println(getLastTrace("student"));
//		System.out.println("----------------------------------------");
//
//		System.out.println("The trace of the Tables Folder:");
//		System.out.println(FileManager.trace());
//		FileManager.reset();
//		System.out.println("----------------------------------------");
//
//		System.out.println("The trace of the Tables Folder after resetting:");
//		System.out.println(FileManager.trace());

//		System.out.println((int) (Math.random() * (2 + 1)));
//
//		Random random = new Random(267);
//		System.out.println((int) (random.nextDouble() * (4 + 1)));

	}
	
	
	
}
