package playground.jjoubert.digicoreNew.step2_Sort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.jjoubert.Utilities.FileSampler.MyFileFilter;
import playground.jjoubert.digicoreNew.containers.DigicoreRecord;

/**
 * The second step in processing the DigiCore data file. This class reads vehicle files 
 * split from the DigiCore data set and sort them chronologically according to the time 
 * stamp.
 * 
 * @author jwjoubert
 */

public class SortDigicoreFiles {
	private final static Logger log = Logger.getLogger(SortDigicoreFiles.class);
	private final static String DELIMITER = ","; // this could be "," or "\t"
	private static String root;
	
	
	public static void main (String args[] ){
		if(args.length != 1){
			throw new RuntimeException("Must provide path for `Vehicles' folder.");
		} else{
			root = args[0];
		}

		log.info("=============================================================");
		log.info("  Sorting DigiCore vehicle files after they've been split.");
		log.info("-------------------------------------------------------------");
		File folder = new File(root);
				
		MyFileFilter mffSorted = new MyFileFilter(".txt.gz");
		File filesSorted[] = folder.listFiles(mffSorted);		
		log.info("Number of files already sorted (*.txt.gz): " + filesSorted.length);

		MyFileFilter mffUnsorted = new MyFileFilter(".txt");
		File filesUnsorted[] = folder.listFiles(mffUnsorted);
		log.info("Number of files to sort (*.txt)          : " + filesUnsorted.length);		

		int counter = 0;
		int multiplier = 1;
		int maxLines = 0;
		File maxFile = null;
		
		for (File theFile : filesUnsorted) {				
			List<DigicoreRecord> unsortedList = readFileToList(theFile);
			List<DigicoreRecord> sortedList = sortTimeMerge( unsortedList );
			
			if(sortedList.size() > maxLines){
				maxLines = sortedList.size();
				maxFile = theFile;
			}

			writeList(theFile, sortedList, true);
			
			// Report progress.
			if(++counter == multiplier){
				log.info("   files sorted: " + counter);
				multiplier *= 2;
			}
			
			boolean deleted = theFile.delete();
			if(!deleted){
				log.warn("Could not delete file " + theFile.getName() + " after sorting.");
			}
		}
		log.info("   files sorted: " + counter + " (Done)");
		
		log.info("Largest file is " + maxFile.getName() + " and has " + maxLines + " gps records.");
		log.info("-------------------------------------------------------------");
		log.info("                     PROCESS COMPLETE ");
		log.info("=============================================================");

	}


	/**
	 * This method reads the given vehicle file, creates a new {@link DigicoreRecord} for
	 * each line, and returns the complete GPS log as a {@link List} of {@link DigicoreRecord}s.
	 *
	 * @param file a valid existing vehicle file containing one or more GPS log records;
	 * @return a {@link List} of {@link DigicoreRecord}s; one record for each line.
	 */
	public static List<DigicoreRecord> readFileToList(File file) {
		int vehID;
		long time;
		double longitude;
		double latitude;
		int status;
		int speed;
		
		List<DigicoreRecord> log = new ArrayList<DigicoreRecord>();
		try {
			BufferedReader br = IOUtils.getBufferedReader(file.getAbsolutePath());
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String [] sa = line.split(DELIMITER);
					if( sa.length == 6){
						try{
							vehID = Integer.parseInt( sa[0] );
							time =  Long.parseLong( sa[1] );
							longitude = Double.parseDouble( sa[2] );
							latitude = Double.parseDouble( sa[3] );
							status = Integer.parseInt( sa[4] );
							speed = Integer.parseInt( sa[5] );

							log.add( new DigicoreRecord(vehID, time, longitude, latitude, status, speed) );
						} catch(NumberFormatException e){
							e.printStackTrace();
						} 
					}
				}
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return log;
	}

	private static void writeList(File file, List<DigicoreRecord> sortedList, boolean header){
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(file.getAbsolutePath() + ".gz");
			try{
				// Write the header, if required, for ArcGIS inclusion
				if (header){
					bw.write("VehicleID");
					bw.write(DELIMITER);
					bw.write("Time");
					bw.write(DELIMITER);
					bw.write("Long");
					bw.write(DELIMITER);
					bw.write("Lat");
					bw.write(DELIMITER);
					bw.write("Status");
					bw.write(DELIMITER);
					bw.write("Speed");
					bw.newLine();
				}
				
				// write all points, except the last, with a newLine
				for (int i = 0; i < (sortedList.size()-1); i++){
					DigicoreRecord dr = sortedList.get(i);
					bw.write(String.valueOf(dr.getVehID()));
					bw.write(DELIMITER);
					bw.write(String.valueOf(dr.getTime()));
					bw.write(DELIMITER);
					bw.write(String.valueOf(dr.getLongitude()));
					bw.write(DELIMITER); 
					bw.write(String.valueOf(dr.getLatitude()));
					bw.write(DELIMITER);
					bw.write(String.valueOf(dr.getStatus()));
					bw.write(DELIMITER);
					bw.write(String.valueOf(dr.getSpeed()));
					bw.newLine();
				}

				// write the last element
				DigicoreRecord drLast = sortedList.get(sortedList.size()-1);
				bw.write(String.valueOf(drLast.getVehID()));
				bw.write(DELIMITER);
				bw.write(String.valueOf(drLast.getTime()));
				bw.write(DELIMITER);
				bw.write(String.valueOf(drLast.getLongitude()));
				bw.write(DELIMITER); 
				bw.write(String.valueOf(drLast.getLatitude()));
				bw.write(DELIMITER);
				bw.write(String.valueOf(drLast.getStatus()));
				bw.write(DELIMITER);
				bw.write(String.valueOf(drLast.getSpeed()));
			} finally{
				bw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Sorts the list of records according to the time stamp.
	 * @param log unsorted {@link List} of {@link DigicoreRecord}s.
	 * @return sorted {@link List} of {@link DigicoreRecord}s.
	 * @see {@link DigicoreRecord#compareTo(DigicoreRecord)}
	 */
	private static List<DigicoreRecord> sortTimeMerge( List<DigicoreRecord> log) {
		Collections.sort(log);		
		return log;
	}
	
}
