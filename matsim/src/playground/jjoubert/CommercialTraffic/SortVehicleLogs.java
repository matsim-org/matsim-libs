package playground.jjoubert.CommercialTraffic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
				if(theFile.isFile() && !(theFile.getName().startsWith(".")) ){
						ArrayList<GPSPoint> log = ActivityLocations.readFileToArray(theFile);
						int maxNumber = log.size();
						
						ArrayList<GPSPoint> sortedArray2 = sortTimeMerge(maxNumber, log);
						if(sortedArray2.size() > maxLines){
							maxLines = sortedArray2.size();
							maxFile = theFile;
						}
						
						try {
							writeSortedArray(DESTFOLDER, theFile, sortedArray2, true);
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
	}
	
	
	private static ArrayList<GPSPoint> sortTimeMerge(int maxNumber,
			ArrayList<GPSPoint> log) {
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
