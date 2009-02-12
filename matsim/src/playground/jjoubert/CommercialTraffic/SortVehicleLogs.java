package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 * 
 * @author johanwjoubert
 *
 */

public class SortVehicleLogs {
	final static String SOURCEFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/";
	final static String DESTFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/Sorted/";
	final static String DELIMITER = " "; // this could be "," or "\t"

	public static void main (String args[] ){
		int progress = 0;
		File outFolder = new File(DESTFOLDER);
		outFolder.mkdir();
		
		File files = new File(SOURCEFOLDER);
		File vehicleFiles[] = files.listFiles();
		int numFiles = vehicleFiles.length;
		if(vehicleFiles.length > 0){
			long maxLines = 0;
			File maxFile = null;
			for (File theFile : vehicleFiles) {				
				if(theFile.isFile() && !(theFile.getName().startsWith(".")) ){
						ArrayList<GPSRecord> log = readFileToArray(theFile);
						
						ArrayList<GPSRecord> sortedArray = sortTimeMerge( log );
						if(sortedArray.size() > maxLines){
							maxLines = sortedArray.size();
							maxFile = theFile;
						}
						
						try {
							writeSortedArray(DESTFOLDER, theFile, sortedArray, true);
						} catch (IOException e) {
							e.printStackTrace();
						}
				}	
				if(++progress%500 == 0){
					System.out.println(progress + " of " + numFiles + " files sorted.");
				}
			}
			System.out.println("Largest file is " + maxFile.getName() + " and has " + maxLines + " gps records.");
		}
	}
	
	public static ArrayList<GPSRecord> readFileToArray(File thisFile) { // I decided to not read in the speed... useless in DigiCore set
		int vehID;
		long time;
		double longitude;
		double latitude;
		int status;
		int speed;
		
		ArrayList<GPSRecord> log = new ArrayList<GPSRecord>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(thisFile) ) );
			while( input.hasNextLine() ){
				String [] inputString = input.nextLine().split(DELIMITER);
				if( inputString.length > 5){
					try{
						vehID = Integer.parseInt( inputString[0] );
						time =  Long.parseLong( inputString[1] );
						longitude = Double.parseDouble( inputString[2] );
						latitude = Double.parseDouble( inputString[3] );
						status = Integer.parseInt( inputString[4] );
						speed = Integer.parseInt( inputString[5] );

						log.add( new GPSRecord(vehID, time, longitude, latitude, status, speed) );
					} catch(NumberFormatException e){
						System.out.print("");
						e.printStackTrace();
					} 
				}
			}
			input.close();						
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return log;
	}

	private static void writeSortedArray(final String destFolder, File theFile,
			ArrayList<GPSRecord> sortedArray, boolean header) throws IOException {
		BufferedWriter output = new BufferedWriter(
				new FileWriter(
						new File(destFolder + "/" + theFile.getName() ) ) );
		// Write the header, if required, for ArcGIS inclusion
		if (header){
			output.write("VehicleID" + DELIMITER + "TIME" + DELIMITER + "LONG" + DELIMITER + "LAT" + DELIMITER + "STATUS" + DELIMITER + "SPEED");
			output.newLine();
		}
		if(sortedArray.size() > 0){
		// write all points, except the last, with a newLine
		for (int i = 0; i <= (sortedArray.size()-2); i++){
			output.write(sortedArray.get(i).getVehID() + DELIMITER + 
						 sortedArray.get(i).getTime() + DELIMITER +
						 sortedArray.get(i).getLongitude() + DELIMITER + 
						 sortedArray.get(i).getLatitude() + DELIMITER +
						 sortedArray.get(i).getStatus() + DELIMITER +
						 sortedArray.get(i).getSpeed() );
			output.newLine();
		}
		
		// write the last element
		output.write(sortedArray.get(sortedArray.size()-1).getVehID() + DELIMITER + 
				 sortedArray.get(sortedArray.size()-1).getTime() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getLongitude() + DELIMITER + 
				 sortedArray.get(sortedArray.size()-1).getLongitude() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getStatus() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getSpeed() );					
		output.close();
		} else{
			System.out.println("There's a problem... empty file");
		}
	}
	
	
	private static ArrayList<GPSRecord> sortTimeMerge( ArrayList<GPSRecord> log) {
		Collections.sort(log);		
		return log;
	}
	
	/*
	 * This RADIX sorting method looked very cool, but turned out to be much slower. Probably
	 * because of the size of the sorting field, i.e. time
	 *   
	private static ArrayList<GPSPoint> sortTimeRadix(int maxT, ArrayList<GPSPoint> log) {
		final int RUNS = (int)Math.ceil(Math.log10(maxT-1));;
		final int RADIX = 10;
		
		ArrayList<ArrayList<GPSPoint>> buckets = new ArrayList<ArrayList<GPSPoint>>(RADIX);	

		for (int i = 0 ; i < RADIX ; i++ ) {
			buckets.add(i, new ArrayList<GPSPoint>() );
		}
		
		for(int r = 1 ; r <= RUNS ; r++ ){
			ArrayList<GPSPoint> sortedLog = new ArrayList<GPSPoint>();
			int mod = (int)Math.pow(RADIX, r);
			int div = (int)Math.pow(RADIX, r-1);
			
			for(int j = 0 ; j < log.size() ; j++){
				int bucketNum = (log.get(j).getTime()%mod)/div;
				buckets.get(bucketNum).add(log.get(j) );			
			}
					
			for(int k = 0 ; k < buckets.size() ; k++ ){
				sortedLog.addAll(buckets.get(k) );
				buckets.set(k, new ArrayList<GPSPoint>() );
			}
			log = sortedLog;
		}
		return log;
	}
	*/	
}
