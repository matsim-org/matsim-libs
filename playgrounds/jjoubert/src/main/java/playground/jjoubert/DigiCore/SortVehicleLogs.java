package playground.jjoubert.DigiCore;

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

import org.apache.log4j.Logger;

import playground.jjoubert.CommercialTraffic.GPSPoint;
import playground.jjoubert.Utilities.ProgressBar;

/**
 * The second step in processing the DigiCore data file. This class reads vehicle files 
 * split from the DigiCore data set and sort them chronologically according to the time 
 * stamp, i.e. the first field.
 * 
 * @author jwjoubert
 */

public class SortVehicleLogs {
	private final static Logger log = Logger.getLogger(SortVehicleLogs.class);
	
	// Set the home directory, depending on where the job is executed.
//	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/"; // Mac
//	final static String ROOT = "~/";												// IVT-Sim0
//	final static String ROOT = "~/data/DigiCore/";									// Satawal
//	final static String ROOT = "/home/jwjoubert/MATSim/MATSimData/DigiCore/2009/";	// IE-Calvin and IE-Hobbes
	
	// Other variables and parameters
	final static String DELIMITER = ","; // this could be "," or "\t"

	/**
	 * The main method prints and updates a progress bar every 100 vehicle files that
	 * have been sorted.
	 * @param args
	 */
	public static void main (String args[] ){
		if(args.length != 1){
			throw new RuntimeException("Must provide path where there `Vehicles' folder can be found.");
		}
		String root = args[0];
		String sourceFolder = root + "Vehicles/";
		String destFolder = root + "SortedVehicles/";
		log.info("=============================================================");
		log.info("  Sorting DigiCore vehicle files after they've been split.");
		log.info("-------------------------------------------------------------");
		
		File outFolder = new File(destFolder);
		boolean checkDirectory = outFolder.mkdir();
		if(!checkDirectory){
			throw new RuntimeException("Could nor create " + outFolder.toString() + ", or it already exists. Delete and then rerun.");
		}
		
		File files = new File(sourceFolder);
		File vehicleFiles[] = files.listFiles();
		int numFiles = vehicleFiles.length;
		
		ProgressBar pb = new ProgressBar('*', numFiles);
		pb.printProgressBar();
		
		if(vehicleFiles.length > 0){
			int filesSorted = 0;
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
							writeSortedArray(destFolder, theFile, sortedArray, true);
						} catch (IOException e) {
							e.printStackTrace();
						}
				}	
				if(filesSorted%100 == 0){
					pb.updateProgress(filesSorted);
				}
				filesSorted++;
			}
			log.info("Done. Largest file is " + maxFile.getName() + " and has " + maxLines + " gps records.");
			log.info("-------------------------------------------------------------");
			log.info("                     PROCESS COMPLETE ");
			log.info("=============================================================");
		}
	}
	/**
	 * This method reads the given vehicle file, creates a new <code>GPSRecord</code> for
	 * each line, and returns the complete GPS log as an <code>ArrayList</code> of 
	 * <code>GPSRecord</code>s.
	 *
	 * @param file a valid existing vehicle file containing one or more GPS log entries;
	 * @return an <code>ArrayList</code> of <code>GPSRecord</code>s; one record for each
	 * 	  	   line.
	 */
	public static ArrayList<GPSRecord> readFileToArray(File file) { // I decided to not read in the speed... useless in DigiCore set
		int vehID;
		long time;
		double longitude;
		double latitude;
		int status;
		int speed;
		
		ArrayList<GPSRecord> log = new ArrayList<GPSRecord>();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(file) ) );
			try{
				while( input.hasNextLine() ){
					String [] inputString = input.nextLine().split(DELIMITER);
					if( inputString.length == 6){
						try{
							vehID = Integer.parseInt( inputString[0] );
							time =  Long.parseLong( inputString[1] );
							longitude = Double.parseDouble( inputString[2] );
							latitude = Double.parseDouble( inputString[3] );
							status = Integer.parseInt( inputString[4] );
							speed = Integer.parseInt( inputString[5] );

							log.add( new GPSRecord(vehID, time, longitude, latitude, status, speed) );
						} catch(NumberFormatException e){
							e.printStackTrace();
						} 
					}
				}
			} finally{
				input.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return log;
	}

	private static void writeSortedArray(final String destFolder, File theFile,
			ArrayList<GPSRecord> sortedArray, boolean header) throws IOException {
		
		BufferedWriter output = new BufferedWriter(new FileWriter(new File(destFolder + "/" + theFile.getName())));
		// Write the header, if required, for ArcGIS inclusion
		if (header){
			output.write("VehicleID");
			output.write(DELIMITER);
			output.write("Time");
			output.write(DELIMITER);
			output.write("Long");
			output.write(DELIMITER);
			output.write("Lat");
			output.write(DELIMITER);
			output.write("Status");
			output.write(DELIMITER);
			output.write("Speed");
			output.newLine();
		}
		if(sortedArray.size() > 0){
			// write all points, except the last, with a newLine
			for (int i = 0; i <= (sortedArray.size()-2); i++){
				output.write(String.valueOf(sortedArray.get(i).getVehID()));
				output.write(DELIMITER);
				output.write(String.valueOf(sortedArray.get(i).getTime()));
				output.write(DELIMITER);
				output.write(String.valueOf(sortedArray.get(i).getLongitude()));
				output.write(DELIMITER); 
				output.write(String.valueOf(sortedArray.get(i).getLatitude()));
				output.write(DELIMITER);
				output.write(String.valueOf(sortedArray.get(i).getStatus()));
				output.write(DELIMITER);
				output.write(String.valueOf(sortedArray.get(i).getSpeed()));
				output.newLine();
			}

			// write the last element
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getVehID()));
			output.write(DELIMITER);
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getTime()));
			output.write(DELIMITER);
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getLongitude()));
			output.write(DELIMITER); 
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getLatitude()));
			output.write(DELIMITER);
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getStatus()));
			output.write(DELIMITER);
			output.write(String.valueOf(sortedArray.get(sortedArray.size()-1).getSpeed()));

			output.close();
		} else{
			log.warn("There's a problem... empty file");
		}
	}

	
	private static ArrayList<GPSRecord> sortTimeMerge( ArrayList<GPSRecord> log) {
		Collections.sort(log);		
		return log;
	}
	
	/**
	 * This <i>RADIX</i> sorting method looked very cool, but turned out to be much slower. 
	 * Probably because of the size of the sorting field, i.e. time.
	 */ 
	@SuppressWarnings("unused")
	@Deprecated
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
				int bucketNum = (int) ((log.get(j).getTime().getTimeInMillis() % mod)/div);
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
	
}
