package playground.singapore.calibration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Reads benchmark/survey .csv files and calculates totals and shares for different modes 
 *
 * @author artemc
 */

public class BenchmarkDataReader {

	private Integer linecount = 0;
	private SortedMap<Integer, Integer[]> dataMap = new TreeMap<Integer, Integer[]>();
	private String[] modes;	 
	private String filePath = "";	
	private String[] categories;
	private Double[] totalTripShareArray;
	private Double conversionFactor  = 1.0;

	private	SortedMap<Integer, Double> totalTripsMap = new TreeMap<Integer, Double>();		
	private	SortedMap<Integer, Double> tripShareMap = new TreeMap<Integer, Double>();
	private	SortedMap<String, Integer> tripsPerModeMap = new TreeMap<String, Integer>();
	private	Integer totalTrips = 0;	

	public BenchmarkDataReader(final String path, final String unit) throws IOException{
		this.setFilePath(path);		
		if(unit.equals("km"))
			this.conversionFactor = 1000.0;
		else if(unit.equals("min"))
			this.conversionFactor = 60.0;
		readCSV(filePath);
	}

	public BenchmarkDataReader(final SortedMap<Integer, Integer[]> dataMap, String[] modes, String[] categories, final String unit) throws IOException{
		if(unit.equals("km"))
			this.conversionFactor = 1000.0;
		else if(unit.equals("min"))
			this.conversionFactor = 60.0;
		this.dataMap = dataMap;
		this.modes = modes;
		this.categories = categories;
	}

	public void readCSV(String filePath) throws IOException{
		  			  
		  BufferedReader CSVFile = new BufferedReader(new FileReader(filePath));
		  String dataRow = CSVFile.readLine(); 
		  modes = dataRow.substring(dataRow.indexOf(",")+1, dataRow.length()).split(",");
		  Boolean emptyCategorie = true;
		  
		  dataRow = CSVFile.readLine(); // Read next line of data.
		  while (dataRow != null){		  
			  linecount++;			  
			  emptyCategorie = true;
			  Integer[] modeShares = new Integer[modes.length];
			  String[] dataArray = dataRow.split(",");
			  for (int i=1; i < dataArray.length; i++) {
				  modeShares[i-1]=Integer.parseInt(dataArray[i]);
				  if(modeShares[i-1]!=0){
					  emptyCategorie = false;
				  }
			  }
			  
			  if(emptyCategorie == false){ //check if the category in the dataset contains values other than 0
				  dataMap.put(Integer.parseInt(dataArray[0]), modeShares); //add the cagtegory to the dataMap
			  }
			  dataRow = CSVFile.readLine(); // Read next line of data.
		  }
			  // Close the file once all data has been read.
		  CSVFile.close();	
		  categories = new String[dataMap.keySet().size()];
		  int catCounter=0;
		  for(Integer cat:dataMap.keySet()){
			  categories[catCounter] = String.valueOf(cat);
			  catCounter++;
		  }
		  
		 
	}
	
	public void calculateSharesAndTotals(){
	
		categories = new String[dataMap.keySet().size()];
		int catCounter=0;
		for(Integer cat:dataMap.keySet()){
			Double catConv = cat/conversionFactor;
			if(conversionFactor==1000.0){
				categories[catCounter]= catConv.toString();
			}
			else{
				int catConvInt = catConv.intValue();
				categories[catCounter] = String.valueOf(catConvInt);
			}
			catCounter++;
		}		
		
		//Calculate total number of trips for each distance class
		for(Integer key:dataMap.keySet()){			
			Integer[] shares = dataMap.get(key);
			totalTripsMap.put(key, 0.0);
			tripShareMap.put(key, 0.0);
			for(Integer tripsOfModeForDistance:shares){
				totalTripsMap.put(key, totalTripsMap.get(key)+(double)tripsOfModeForDistance);
				totalTrips = totalTrips + tripsOfModeForDistance;
			}			
		}
		
		//Calculate trip share for each distance class
		totalTripShareArray = new Double[categories.length];
		int l=0;
		for(Integer key:dataMap.keySet()){		
			tripShareMap.put(key, totalTripsMap.get(key)/totalTrips);
			totalTripShareArray[l]=(totalTripsMap.get(key)/totalTrips)*100;
			l++;
		}
		
		//Calculate total number of trips for each mode				
		int modeNumber = 0;
		for(String mode:modes){
			tripsPerModeMap.put(mode, 0);
			for(Integer key:dataMap.keySet()){		
					Integer[] modeTrips = dataMap.get(key);
					tripsPerModeMap.put(mode, tripsPerModeMap.get(mode)+modeTrips[modeNumber]);	
			}
			modeNumber++;
		}		
		
	}

	public SortedMap<String, Integer> getTripsPerModeMap() {
		return tripsPerModeMap;
	}


	public void setTripsPerModeMap(SortedMap<String, Integer> tripsPerModeMap) {
		this.tripsPerModeMap = tripsPerModeMap;
	}


	public SortedMap<Integer, Integer[]> getDataMap() {
		return dataMap;
	}


	public String[] getModes() {
		return modes;
	}


	public String[] getCategories() {
		return categories;
	}
	
		
	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void setTotalTripsMap(SortedMap<Integer, Double> totalTripsMap) {
		this.totalTripsMap = totalTripsMap;
	}


	public SortedMap<Integer, Double> getTripShareMap() {
		return tripShareMap;
	}

	public SortedMap<Integer, Double> getTotalTripsMap() {
		return totalTripsMap;
	}

	public void setTripShareMap(SortedMap<Integer, Double> tripShareMap) {
		this.tripShareMap = tripShareMap;
	}


	public Integer getTotalTrips() {
		return totalTrips;
	}


	public void setTotalTrips(Integer totalTrips) {
		this.totalTrips = totalTrips;
	}
	
	public Double[] getTotalTripShareArray() {
		return totalTripShareArray;
	}
 
} 