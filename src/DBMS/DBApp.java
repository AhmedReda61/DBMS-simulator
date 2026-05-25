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

		for (int i = 0; i < t.getColumns().length ; i++) {
			BitmapIndex index = FileManager.loadTableIndex(tableName , t.getColumns()[i]);
			if (index == null) continue;
			index.insert(record[i]);
			FileManager.storeTableIndex(tableName, t.getColumns()[i], index);
		}

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

		String op = Arrays.toString(cols) + "->" + Arrays.toString(vals) +"Records per page:[";

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

		// 1. Load the Table object (which contains the full operation trace)
		Table t = FileManager.loadTable(tableName);
		ArrayList<String[]> res = new ArrayList<>();
		int missingCount = 0;

		// 2. Iterate through all page numbers up to lastPage
		for (int i = 0; i <= t.getLastPage(); i++) {
			Page p = FileManager.loadTablePage(tableName, i);

			// 3. If a page file is missing on disk, reconstruct its records from the trace logs
			if (p == null) {
				for (String op : t.getOps()) {
					if (op.startsWith("Inserted:[")) {
						int pageStart = op.indexOf(", at page number:");
						if (pageStart != -1) {
							// Extract the page number from the log entry
							int pageEnd = op.indexOf(",", pageStart + 17);
							if (pageEnd != -1) {
								String pageNumStr = op.substring(pageStart + 17, pageEnd).trim();
								int pageNum = Integer.parseInt(pageNumStr);

								// If this log entry belongs to our missing page
								if (pageNum == i) {
									// Extract the bracketed record contents: "val1, val2, ..."
									String recordStr = op.substring(10, pageStart - 1);
									String[] record = recordStr.split(", ");
									res.add(record);
									missingCount++;
								}
							}
						}
					}
				}
			}
		}

		// 4. Update the table trace logs and save the table metadata
		t.getOps().add("Validating records: " + missingCount + " records missing.");
		FileManager.storeTable(tableName, t);

		return res;
	}

	public static void recoverRecords(String tableName, ArrayList<String[]> missing){
		Table t = FileManager.loadTable(tableName);
		if (t == null) return;

		Set<Integer> deletedPagesSet = new TreeSet<>();
		
		// 1. Identify which pages were deleted by matching missing records with the table log entries
		for (String[] record : missing) {
			StringBuilder sb = new StringBuilder("Inserted:[");
			for (int j = 0; j + 1 < record.length; j++) {
				sb.append(record[j]).append(", ");
			}
			sb.append(record[record.length - 1]).append("]");
			String targetPrefix = sb.toString() + ", at page number:";

			for (String op : t.getOps()) {
				if (op.startsWith(targetPrefix)) {
					int pageStart = op.indexOf(", at page number:");
					int pageEnd = op.indexOf(",", pageStart + 17);
					if (pageEnd != -1) {
						int pageNum = Integer.parseInt(op.substring(pageStart + 17, pageEnd).trim());
						deletedPagesSet.add(pageNum);
						break;
					}
				}
			}
		}

		ArrayList<Integer> deletedPages = new ArrayList<>(deletedPagesSet);

		// 2. Reconstruct each deleted page using the log entries in t.getOps()
		for (int pageNum : deletedPages) {
			Page p = new Page(pageNum);
			for (String op : t.getOps()) {
				if (op.startsWith("Inserted:[")) {
					int pageStart = op.indexOf(", at page number:");
					if (pageStart != -1) {
						int pageEnd = op.indexOf(",", pageStart + 17);
						if (pageEnd != -1) {
							int parsedPageNum = Integer.parseInt(op.substring(pageStart + 17, pageEnd).trim());
							if (parsedPageNum == pageNum) {
								String recordStr = op.substring(10, pageStart - 1);
								String[] rec = recordStr.split(", ");
								p.addRecord(rec);
							}
						}
					}
				}
			}
			// Store the fully reconstructed Page
			FileManager.storeTablePage(tableName, pageNum, p);
		}

		// 3. Log recovery operation and save table metadata
		t.getOps().add("Recovering " + missing.size() + " records in pages: " + deletedPages);
		FileManager.storeTable(tableName, t);
	}

	public static void createBitMapIndex(String tableName, String colName){

		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);

		int colNumber = 0;

		for (int i = 0; i < t.getColumns().length; i++) {
			if (t.getColumns()[i].equals(colName)) {
				colNumber = i;
				break;
			}
		}

		BitmapIndex index = new BitmapIndex();

		for (int i = 0; i <= t.getLastPage(); i++) {
			Page p = FileManager.loadTablePage(tableName, i);
			for (String[] record : p.getRecords()){
				index.insert(record[colNumber]);
			}
		}

		long endTime = System.nanoTime();

		long time = (endTime - startTime) / 1000000;

		// Index created for column: [colName], execution time (mil): [time]

		String op = "Index created for column: \"" + tableName + "\" on column: \"" + colName
				+ "\", execution time (mil):" + time;

		t.getOps().add(op);
		FileManager.storeTableIndex(tableName, colName, index);
		FileManager.storeTable(tableName, t);

	}

	public static String getValueBits(String tableName, String colName, String value){
		BitmapIndex index = FileManager.loadTableIndex(tableName , colName);
		return index.get(value);
	}

	public static ArrayList<String []> selectIndex(String tableName, String[] cols, String[] vals){
		long startTime = System.nanoTime();

		Table t = FileManager.loadTable(tableName);
		ArrayList<String[]> res = new ArrayList<>();
		String[] columns = t.getColumns();

		ArrayList<String> indexedCols = new ArrayList<>();
		ArrayList<String> nonIndexedCols = new ArrayList<>();

		for (int i = 0; i < cols.length; i++) {
			BitmapIndex idx = FileManager.loadTableIndex(tableName, cols[i]);
			if (idx != null) {
				indexedCols.add(cols[i]);
			} else {
				nonIndexedCols.add(cols[i]);
			}
		}

		int indexedSelectionCount = 0;

		if (indexedCols.isEmpty()) {
			// Case 1: No columns are indexed
			// Do standard linear search
			for (int i = 0; i <= t.getLastPage(); i++) {
				Page p = FileManager.loadTablePage(tableName, i);
				if (p == null) continue;
				for (String[] record : p.getRecords()) {
					boolean ok = true;
					for (int j = 0; j < cols.length; j++) {
						int colIndex = -1;
						for (int k = 0; k < columns.length; k++) {
							if (columns[k].equals(cols[j])) {
								colIndex = k;
								break;
							}
						}
						if (colIndex != -1 && !record[colIndex].equals(vals[j])) {
							ok = false;
							break;
						}
					}
					if (ok) {
						res.add(record);
					}
				}
			}
		} else {
			// Case 2 & 3: Some or all columns are indexed
			// Perform index ANDing
			StringBuilder resString = new StringBuilder();
			for (int i = 0; i < t.getRows(); i++) {
				resString.append("1");
			}

			for (int i = 0; i < cols.length; i++) {
				BitmapIndex idx = FileManager.loadTableIndex(tableName, cols[i]);
				if (idx == null) continue;
				String bitstream = idx.get(vals[i]);
				for (int j = 0; j < t.getRows(); j++) {
					if (resString.charAt(j) == '1' && j < bitstream.length() && bitstream.charAt(j) == '1') {
						// keep '1'
					} else {
						resString.setCharAt(j, '0');
					}
				}
			}

			// Count matching records from indices
			for (int i = 0; i < resString.length(); i++) {
				if (resString.charAt(i) == '1') {
					indexedSelectionCount++;
				}
			}

			// Filter matching records with non-indexed conditions
			for (int i = 0; i < t.getRows(); i++) {
				if (resString.charAt(i) == '1') {
					Page p = FileManager.loadTablePage(tableName, i / dataPageSize);
					if (p != null) {
						String[] record = p.getRecord(i % dataPageSize);
						boolean ok = true;
						for (int j = 0; j < cols.length; j++) {
							int colIndex = -1;
							for (int k = 0; k < columns.length; k++) {
								if (columns[k].equals(cols[j])) {
									colIndex = k;
									break;
								}
							}
							if (colIndex != -1 && !record[colIndex].equals(vals[j])) {
								ok = false;
								break;
							}
						}
						if (ok) {
							res.add(record);
						}
					}
				}
			}
		}

		long endTime = System.nanoTime();
		long time = (endTime - startTime) / 1000000;

		// Construct trace operation string
		String op = "";
		if (indexedCols.isEmpty()) {
			op = Arrays.toString(cols) + "->" + Arrays.toString(vals)
					+ ", Non Indexed: " + Arrays.toString(cols)
					+ ", Final count: " + res.size()
					+ ", execution time (mil):" + time;
		} else if (nonIndexedCols.isEmpty()) {
			op = Arrays.toString(cols) + "->" + Arrays.toString(vals)
					+ ", Indexed columns: " + indexedCols.toString()
					+ ", Indexed selection count: " + indexedSelectionCount
					+ ", Final count: " + res.size()
					+ ", execution time (mil):" + time;
		} else {
			op = Arrays.toString(cols) + "->" + Arrays.toString(vals)
					+ ", Indexed columns: " + indexedCols.toString()
					+ ", Indexed selection count: " + indexedSelectionCount
					+ ", Non Indexed: " + nonIndexedCols.toString()
					+ ", Final count: " + res.size()
					+ ", execution time (mil):" + time;
		}

		t.selectCondition("index condition", op);
		FileManager.storeTable(tableName, t);

		return res;
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

		Table t = FileManager.loadTable(tableName);

		int rows = t.getRows();

		int numberOfBlocks = (rows + indexPageSize - 1) / indexPageSize;

		ArrayList<DenseIndexBlock> blocks = new ArrayList<>();

		for (int i = 0; i < numberOfBlocks ; i++) {
			DenseIndexBlock block = FileManager.loadIndexBlock(tableName, colName, i);

			if (block == null)continue;

			blocks.add(block);

		}

		return blocks.toString();
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

		ArrayList<String> indexedCols = new ArrayList<>();
		for (String col : t.getColumns()) {
			if (FileManager.loadTableIndex(tableName, col) != null) {
				indexedCols.add(col);
			}
		}
		res.append(", Indexed Columns: ").append(indexedCols.toString());

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

		createDenseIndex("student" , "major");

		System.out.println(getIndexRepresentation("student" , "major"));

		FileManager.reset();
		System.out.println("--------------------------------");
		System.out.println("The trace of the Tables Folder after resetting:");
		System.out.println(FileManager.trace());

	}

}
