package playground.pieter.demandgeneration.emme;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;

import org.matsim.core.utils.io.IOUtils;

public class HashMatrix{
	/**
	 * @param args
	 */
	private HashMap<Integer,HashMap<Integer, Integer>> intMatrix;
	private HashMap<Integer,HashMap<Integer, Double>> dblMatrix;
	private HashMap<Integer,HashMap<Integer, Double>> normalizedMatrix;
	private HashMap<Integer,HashMap<Integer, Double>> cumulativeProbMatrix;
	private String headers[];
	private Connection sqlConnection;
	private String sqlTable, fromField, toField, valueField;
	private Set<Integer> headerSet;

	public HashMatrix(String sqlServer, String sqlUser, String sqlPassword, String sqlTable,
			String fromField, String toField, String valueField) throws SQLException {
		super();
		this.dblMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		this.normalizedMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		this.cumulativeProbMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		this.sqlTable = sqlTable;
		this.fromField=fromField;
		this.toField = toField;
		this.valueField=valueField;
		makeConnection(sqlServer, sqlUser, sqlPassword);
		initializeSQLMatrices();
		populateSQLMatrix();
		this.headerSet = this.dblMatrix.keySet();
		normalizeMatrix();
		this.closeConnection();
	}

	public HashMatrix(String matrixFileWithHeaders) throws Exception {
		super();
		this.dblMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		this.normalizedMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		this.cumulativeProbMatrix=new HashMap<Integer,HashMap<Integer, Double>>();
		populateMatrix(matrixFileWithHeaders);
		this.headerSet = this.dblMatrix.keySet();
		normalizeMatrix();
	}
	public int getInt(int key1, int key2){
		return this.intMatrix.get(key1).get(key2).intValue(); 
	}

	public double getEntry(int key1, int key2){
		return this.dblMatrix.get(key1).get(key2).doubleValue(); 
	}

	public double getProb(int key1, int key2){
		return this.normalizedMatrix.get(key1).get(key2).doubleValue();
	}

	public double getCumProb(int key1, int key2){
		return this.cumulativeProbMatrix.get(key1).get(key2).doubleValue();
	}

	public Set<Integer> getHeaderSet(){
		return this.headerSet;
	}

	public Set<Entry<Integer, Double>> getRowProbabilitySet(int rowNumber){
		return this.normalizedMatrix.get(rowNumber).entrySet();
	}

	public String[] getHeaders(){
		return this.headers;
	}
	
	public double getRowTotal(int key){
		HashMap<Integer,Double> rowMap = this.dblMatrix.get(key);
		double rowTotal = 0.0;
		Iterator<Double> valueIt = rowMap.values().iterator();
		while (valueIt.hasNext()){
			rowTotal += valueIt.next().doubleValue();
		}

		return rowTotal;

	}

	private void normalizeMatrix() {
		Iterator<Integer> fromKeys = this.dblMatrix.keySet().iterator();
		while(fromKeys.hasNext()){
			Integer fromKey = fromKeys.next();
			double rowTotal = getRowTotal(fromKey);
			//now get the keys for each column in this row
			Iterator<Entry<Integer,Double>> columns = this.dblMatrix.get(fromKey).entrySet().iterator();
			double cumulProb = 0.0;
			while(columns.hasNext()){
				Entry<Integer,Double> columnAndValue = columns.next();
				Integer toKey = columnAndValue.getKey();
				double dblValue = columnAndValue.getValue().doubleValue();
				double prob=0;
				if(rowTotal>0){
					prob = dblValue/rowTotal;
				}
				cumulProb += prob;
				this.normalizedMatrix.get(fromKey).put(toKey, prob);
				this.cumulativeProbMatrix.get(fromKey).put(toKey, cumulProb);
			}
		}

	}

	private void populateSQLMatrix() {
		//reads values from SQL into matrix
		Statement myStmt;
		try{
			myStmt = sqlConnection.createStatement();
			String valueQuery = String.format(
					"SELECT * FROM %s ORDER BY %s,%s",
					this.sqlTable, this.fromField, this.toField);
			ResultSet result = myStmt.executeQuery(valueQuery); 
			long counter =0;
			while (result.next()){
				if(counter++ % 1000 == 0) System.out.println("Writing line " +counter);
				int fromKey=result.getInt(this.fromField);
				int toKey=result.getInt(this.toField);
				double value=result.getDouble(this.valueField);
				this.dblMatrix.get(fromKey).put(toKey, value);//retrieves the row hashmap and puts value for column	      
			}
		}
		catch (Exception sqlEx){
			System.err.println(sqlEx);
		}
	}

	private void makeConnection(String server, String user, String password) {
		try{
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			//	      this.sqlConnection = DriverManager.getConnection(
			//	              "jdbc:mysql://localhost/temp",
			//	              "pfourie","koos");
			this.sqlConnection = DriverManager.getConnection(server, user, password);
		}catch (Exception sqlEx){
			System.err.println(sqlEx);
		}
	}




	private void initializeSQLMatrices(){
		//this reads SQL source where from, to and value fields are all ints, 
		//		and creates the nxn matrix with all vals set to 0
		Statement fromStmt;
		try{
			fromStmt = sqlConnection.createStatement();
			String headerQuery = String.format(
					"SELECT DISTINCT %s as taz FROM %s UNION SELECT DISTINCT %s as taz FROM %s ORDER BY taz",
					this.fromField, this.sqlTable, this.toField, this.sqlTable);
			ResultSet fromKeys = fromStmt.executeQuery(headerQuery); //returns row headers ordered by taz number
			long counter=0;
			while (fromKeys.next()){
				int fromKey=fromKeys.getInt("taz");
				this.dblMatrix.put(fromKey, new HashMap<Integer, Double>()); //each row needs a hashmap
				this.normalizedMatrix.put(fromKey, new HashMap<Integer, Double>()); 
				this.cumulativeProbMatrix.put(fromKey, new HashMap<Integer, Double>()); 
				Statement toStmt = sqlConnection.createStatement();
				ResultSet toKeys = toStmt.executeQuery(headerQuery);
				while(toKeys.next()){
					if(counter++ % 1000 == 0) System.out.println("Initing line " +counter);
					int toKey = toKeys.getInt("taz");
//					System.out.println("creating combo "+ fromKey+":"+toKey);
					this.dblMatrix.get(fromKey).put(toKey,0.0);
					this.normalizedMatrix.get(fromKey).put(toKey,0.0);
					this.cumulativeProbMatrix.get(fromKey).put(toKey,0.0);
				}
			}
		}
		catch (Exception sqlEx){
			System.err.println(sqlEx);
		}	
	}

	private void populateMatrix(String matrixFileWithHeaders) throws Exception{
		/*Assumes matrix file is a .csv that looks like this:
		 *FROM,		zone_1,		zone_2,		...,	zone_n
		 *zone_1,	entry_1:1,	entry_1:2,	...,	entry_1:n	
		 *zone_2,	entry_2:1,	entry_2:2,	...,	entry_2:n
		 *...,		...,		...,		...,	...
		 *zone_n	entry_n:1,	entry_n:2,	...,	entry_n:n
		 */

		//first line contains the headers
		BufferedReader zoneReader = IOUtils.getBufferedReader(matrixFileWithHeaders);
		String headertabs[] = zoneReader.readLine().split(",");
		//drop the first element ("FROM") as row headings are below it
		this.headers = Arrays.copyOfRange(headertabs, 1, headertabs.length);
		for(String tabs : this.headers){
			//			ignore the first entry
			int fromKey = Integer.parseInt(tabs);
			this.dblMatrix.put(fromKey, new HashMap<Integer, Double>()); //each row needs a hashmap
			this.normalizedMatrix.put(fromKey, new HashMap<Integer, Double>()); 
			this.cumulativeProbMatrix.put(fromKey, new HashMap<Integer, Double>()); 
			for(String tabs2 : this.headers){
				int toKey = Integer.parseInt(tabs2);
				this.dblMatrix.get(fromKey).put(toKey,0.0);
				this.normalizedMatrix.get(fromKey).put(toKey,0.0);
				this.cumulativeProbMatrix.get(fromKey).put(toKey,0.0);
			}
		}
		String nextLine = zoneReader.readLine();
		do{
			String splits[] = nextLine.split(",");
			int fromKey = Integer.parseInt(splits[0]);
			for (int i = 1; i<splits.length; i++){

				int toKey = Integer.parseInt(headers[i-1]);
				double doubleVal = Double.parseDouble(splits[i]);
				this.dblMatrix.get(fromKey).put(toKey,doubleVal);

			}
			nextLine = zoneReader.readLine();
		}while(nextLine != null);
		zoneReader.close();
	}

	private void closeConnection() throws SQLException{
		this.sqlConnection.close();
	}

	public static void main(String[] args) throws Exception {
		HashMatrix test = new HashMatrix("jdbc:mysql://localhost/trip_distro_eth",
				"root","kr","durations","otaz","dtaz","avdur" );
//						HashMatrix test =new HashMatrix("jdbc:mysql://localhost/temp",
//		        "pfourie","koos","emmettmatrix","fzone","tzone","mins" );
//		HashMatrix test = new HashMatrix("d:/temp/ethttimes.csv");
		test.printMatrices();
		test.writeMatrices("d:/temp/durations.txt");

	}

	private void printMatrices() {
		int keys[];
		keys = new int[this.dblMatrix.keySet().size()];
		Iterator<Integer> fromKeys = this.dblMatrix.keySet().iterator();
		int keycount=0;
		while(fromKeys.hasNext()){
			keys[keycount++]=fromKeys.next().intValue();
		}
		Arrays.sort(keys);
		//print top row
		System.out.printf("%15d",0);
		for (int k : keys) System.out.printf("%15d",k);
		System.out.print("\n");
		for (int from : keys){
			System.out.printf("%15d",from);
			for (int to : keys){
				System.out.printf("%15.4f",this.getEntry(from, to));
			}
			System.out.print("\n");
		}

		System.out.println("\n Probabilities:");
		System.out.printf("%15d",0);
		for (int k : keys) System.out.printf("%15d",k);
		System.out.print("\n");
		for (int from : keys){
			System.out.printf("%15d",from);
			for (int to : keys){
				System.out.printf("%15.4f",this.getProb(from, to));
			}
			System.out.print("\n");
		}

		System.out.println("\n Cumulative Probabilities:");
		System.out.printf("%15d",0);
		for (int k : keys) System.out.printf("%15d",k);
		System.out.print("\n");
		for (int from : keys){
			System.out.printf("%15d",from);
			for (int to : keys){
				System.out.printf("%15.4f",this.getCumProb(from, to));
			}
			System.out.print("\n");
		}
	}
	
	private void writeMatrices(String fileName) throws Exception{
		int keys[];
		keys = new int[this.dblMatrix.keySet().size()];
		Iterator<Integer> fromKeys = this.dblMatrix.keySet().iterator();
		int keycount=0;
		while(fromKeys.hasNext()){
			keys[keycount++]=fromKeys.next().intValue();
		}
		Arrays.sort(keys);
		BufferedWriter output = IOUtils.getBufferedWriter(fileName);
		//print top row
		output.write(String.format("%15d",0));
		for (int k : keys) output.write(String.format("%15d",k));
		output.write("\n");
		for (int from : keys){
			output.write(String.format("%15d",from));
			for (int to : keys){
//				output.write(String.format("%15d",to));
				output.write(String.format("%15.4f",this.getEntry(from, to)));
			}
			output.write("\n");
		}
		output.close();

//		output.write("\n Probabilities:");
//		output.write(String.format("%15d",0));
//		for (int k : keys) output.write(String.format("%15d",k));
//		output.write("\n");
//		for (int from : keys){
//			output.write(String.format("%15d",from));
//			for (int to : keys){
//				output.write(String.format("%15.4f",this.getProb(from, to)));
//			}
//			output.write("\n");
//		}
//
//		output.write("\n Cumulative Probabilities:");
//		output.write(String.format("%15d",0));
//		for (int k : keys) output.write(String.format("%15d",k));
//		output.write("\n");
//		for (int from : keys){
//			output.write(String.format("%15d",from));
//			for (int to : keys){
//				output.write(String.format("%15.4f",this.getCumProb(from, to)));
//			}
//			output.write("\n");
//		}
	}
	
	private void writeFlatFile(String fileName) throws Exception{
		int keys[];
		keys = new int[this.dblMatrix.keySet().size()];
		Iterator<Integer> fromKeys = this.dblMatrix.keySet().iterator();
		int keycount=0;
		while(fromKeys.hasNext()){
			keys[keycount++]=fromKeys.next().intValue();
		}
		Arrays.sort(keys);
		BufferedWriter output = IOUtils.getBufferedWriter(fileName);
		//print top row
		for (int from : keys){
			
			for (int to : keys){
				output.write(String.format("%15d",from));
				output.write(String.format("%15d",to));
				output.write(String.format("%15.4f \n",this.getEntry(from, to)));
			}
			
		}
		output.close();

//		output.write("\n Probabilities:");
//		output.write(String.format("%15d",0));
//		for (int k : keys) output.write(String.format("%15d",k));
//		output.write("\n");
//		for (int from : keys){
//			output.write(String.format("%15d",from));
//			for (int to : keys){
//				output.write(String.format("%15.4f",this.getProb(from, to)));
//			}
//			output.write("\n");
//		}
//
//		output.write("\n Cumulative Probabilities:");
//		output.write(String.format("%15d",0));
//		for (int k : keys) output.write(String.format("%15d",k));
//		output.write("\n");
//		for (int from : keys){
//			output.write(String.format("%15d",from));
//			for (int to : keys){
//				output.write(String.format("%15.4f",this.getCumProb(from, to)));
//			}
//			output.write("\n");
//		}
	}
	
}