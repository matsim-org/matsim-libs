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
	final static String SOURCEFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/";
	final static String DESTFOLDER = "/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Sorted/";
	final static String DELIMITER = ","; // this could be "," or "\t" 

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
				if(theFile.isFile() && 
						!theFile.getName().equalsIgnoreCase(".DS_Store") ){
					ArrayList<GPSPoint> log = new ArrayList<GPSPoint>();
					try {
						int maxNumber = readVehicleFileToArray(theFile, log);
						
						// turns out the RADIX is much slower, probably due to the length
						// of the sorting field: the 10-digit time stamp
//						long startRadix = System.currentTimeMillis();
//						ArrayList<GPSPoint> sortedArray1 = sortTimeRadix(maxNumber, log);
//						long endRadix = System.currentTimeMillis();
						
//						long startMerge = System.currentTimeMillis();
						ArrayList<GPSPoint> sortedArray2 = sortTimeMerge(maxNumber, log);
//						long endMerge = System.currentTimeMillis();
						if(sortedArray2.size() > maxLines){
							maxLines = sortedArray2.size();
							maxFile = theFile;
						}
						
						writeSortedArray(DESTFOLDER, theFile, sortedArray2, true);
					} catch (Exception e) {
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


	private static int readVehicleFileToArray(File theFile,
			ArrayList<GPSPoint> log) throws FileNotFoundException {
		int maxTime = 0;
		Scanner input = new Scanner( new BufferedReader( new FileReader(theFile) ) );
		while(input.hasNextLine() ){
			String [] inputString = input.nextLine().split(" ");
			if(inputString.length > 5){ // avoid reading empty lines to an array 
				GPSPoint gps = new GPSPoint();
				gps.setVehID( Integer.parseInt( inputString[0] ) );
				gps.setTime( Integer.parseInt( inputString[1] ) );
				gps.setX( Double.parseDouble( inputString[2] ) );
				gps.setY( Double.parseDouble( inputString[3] ) );
				gps.setStatus( Integer.parseInt( inputString[4] ) );
				gps.setSpeed( Integer.parseInt( inputString[5] ) );
				log.add(gps);
				maxTime = Math.max(maxTime, gps.getTime() );
			}
		}
		return maxTime;
	}

	private static void writeSortedArray(final String destFolder, File theFile,
			ArrayList<GPSPoint> sortedArray, boolean header) throws IOException {
		BufferedWriter output = new BufferedWriter(
				new FileWriter(
						new File(destFolder + "/" + theFile.getName() ) ) );
		// Write the header, if required, for ArcGIS inclusion
		if (header){
			output.write("VehicleID" + DELIMITER + "TIME" + DELIMITER + "LONG" + DELIMITER + "LAT" + DELIMITER + "STATUS" + DELIMITER + "SPEED");
			output.newLine();
		}
		// write all points, except the last, with a newLine
		for (int i = 0; i <= (sortedArray.size()-2); i++){
			output.write(sortedArray.get(i).getVehID() + DELIMITER + 
						 sortedArray.get(i).getTime() + DELIMITER +
						 sortedArray.get(i).getX() + DELIMITER + 
						 sortedArray.get(i).getY() + DELIMITER +
						 sortedArray.get(i).getStatus() + DELIMITER +
						 sortedArray.get(i).getSpeed() );
			output.newLine();
		}
		
		// write the last element
		output.write(sortedArray.get(sortedArray.size()-1).getVehID() + DELIMITER + 
				 sortedArray.get(sortedArray.size()-1).getTime() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getX() + DELIMITER + 
				 sortedArray.get(sortedArray.size()-1).getY() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getStatus() + DELIMITER +
				 sortedArray.get(sortedArray.size()-1).getSpeed() );					
		output.close();
	}

//	private static ArrayList<GPSPoint> sortTimeRadix(int maxT, ArrayList<GPSPoint> log) {
//		final int RUNS = (int)Math.ceil(Math.log10(maxT-1));;
//		final int RADIX = 10;
//		
//		ArrayList<ArrayList<GPSPoint>> buckets = new ArrayList<ArrayList<GPSPoint>>(RADIX);	
//
//		for (int i = 0 ; i < RADIX ; i++ ) {
//			buckets.add(i, new ArrayList<GPSPoint>() );
//		}
//		
//		for(int r = 1 ; r <= RUNS ; r++ ){
//			ArrayList<GPSPoint> sortedLog = new ArrayList<GPSPoint>();
//			int mod = (int)Math.pow(RADIX, r);
//			int div = (int)Math.pow(RADIX, r-1);
//			
//			for(int j = 0 ; j < log.size() ; j++){
//				int bucketNum = (log.get(j).getTime()%mod)/div;
//				buckets.get(bucketNum).add(log.get(j) );			
//			}
//					
//			for(int k = 0 ; k < buckets.size() ; k++ ){
//				sortedLog.addAll(buckets.get(k) );
//				buckets.set(k, new ArrayList<GPSPoint>() );
//			}
//			log = sortedLog;
//		}
//		return log;
//	}

	private static ArrayList<GPSPoint> sortTimeMerge(int maxNumber,
			ArrayList<GPSPoint> log) {
		Collections.sort(log);		
		return log;
	}

}
