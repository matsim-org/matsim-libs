package playground.southafrica.freight.digicore.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.freight.digicore.containers.DigicoreVehicle;
import playground.southafrica.utilities.FileUtils;


/**
 * A class with various static methods that are frequently used in Digicore
 * analysis.
 * 
 * @author jwjoubert
 */
public class DigicoreUtils {
	private final static Logger LOG = Logger.getLogger(DigicoreUtils.class);

	 /**
	  * Reads a file that contains a single column of human-readable dates in
	  * the format YYYY/MM/DD, each representing and converts them to the
	  * specific {@link Calendar#DAY_OF_YEAR}.
	  * 
	  * @param filename the absolute path of the file to be read in
	  * @return {@link List} of integers representing the day of the year.
	  * @throws NumberFormatException
	  * @throws IOException
	  */
	public static List<Integer> readDayOfYear(String filename){
		LOG.info("Reading in day-of-year from " + filename);
		List<Integer> list = new ArrayList<Integer>();

		File inputFile = new File(filename);
		if(!inputFile.exists() || !inputFile.isFile()){
			LOG.warn("Error reading abnormal days from " + filename);
			LOG.warn("Returning empty list.");
		} else{
			GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), new Locale("en", "ZA"));

			BufferedReader br = IOUtils.getBufferedReader(inputFile.getAbsolutePath());
			String inputLine = null;
			try{
				while((inputLine = br.readLine()) != null) {
					/*date format is:	yyyy/mm/dd	
					 *substring indices:0123456789
					 */
					//get the year value
					int yearValue = Integer.parseInt(inputLine.substring(0, 4));//subtring(startIndex,endIndex) where endIndex is NOT included
					//get the month value, where 0=January,1=February... thus subtract 1 from month value that is read...
					int monthValue = (Integer.parseInt(inputLine.substring(5, 7))) - 1;
					//get the day value
					int dayValue = Integer.parseInt(inputLine.substring(8, 10));
					
					/*set year, month and day values in Gregorian Calendar
					 * NB!!! MONTH VALUE STARTS AT 0, thus 0=January,1=February... */
					gc.set(yearValue, monthValue, dayValue);
					
					/* add day-of-year to Abnormal Days' List<Integer> */
					list.add(gc.get(GregorianCalendar.DAY_OF_YEAR));
				}
				
			} catch (NumberFormatException e) {
				LOG.warn("Could not parse date from " + inputLine  + " and will be ignored.");
			} catch (IOException e) {
				throw new RuntimeException("IOException reading from " + filename);
			} finally{
				try {
					br.close();
				} catch (IOException e) {
					throw new RuntimeException("Couldn't read from Bufferedreader " + filename);
				};
			}

			LOG.info("Done reading day-of-year values (" + list.size() + " found)");
		}
		return list;
	}
	
	
	/**
	 * This method reads in a .txt file containing vehicle {@link Id}s, one per 
	 * line, and creates a {@link List} of {@link File}s if a corresponding 
	 * {@link DigicoreVehicle} file can be identified. 
	 * 
	 * @param inputFile containing the {@link Id}s;
	 * @param xmlFolder where {@link DigicoreVehicle} files will be checked;
	 * @return the {@link List} of {@link DigicoreVehicle} files.
	 */
	public static List<File> readDigicoreVehicleIds(String inputFile, String xmlFolder) throws IOException{
		File inputFolder = new File(xmlFolder);
		
		List<String> list = new ArrayList<String>();
		List<File> listOfVehicleFiles = new ArrayList<File>();
		
		/* Read in the vehicle Ids, and add the file extension. */
		BufferedReader br = IOUtils.getBufferedReader(inputFile);
		try{
			String inputLine = null;
			while((inputLine = br.readLine()) != null){
				list.add(inputLine + ".xml.gz");
			}
		} finally{
			br.close();
		}
		
		/* Get all the xml files in the folder. */
		List<File> allVehicleFiles = FileUtils.sampleFiles(inputFolder, Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
		
		/* Compare the read Ids with the available files, and add those matching
		 * to the result list. */
		for(File vehicleFile : allVehicleFiles){
			String fileName = vehicleFile.getName();
			if(list.contains(fileName)){
				listOfVehicleFiles.add(vehicleFile);
			}
		}
		
		return listOfVehicleFiles;
	}

		
	/**
	 * Reads a file that contains one date per line in the format <code>YYYY/MM/DD</code>.
	 *  
	 * @param filename path to the file containing the abnormal days.
	 * @return a {@link List}<{@link String}>s where each string is a date in the format
	 * 		   <code>YYYY/MM/DD</code>.
	 */
	public static List<String> readAbnormalDays(String filename){
		LOG.info("Reading abnormal days from " + filename);
		
		List<String> list = new ArrayList<String>();
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line=br.readLine()) != null){
				list.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from abnormal days file.");
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close abnormal days file.");
			}
		}
		LOG.info("Number of abnormal days read: " + list.size());
		return list;
	}
	
	
	/**
	 * Converts a {@link GregorianCalendar} to an easy-to-read {@link String} 
	 * in the format <code>YYYY/MM/DD</code>.
	 * @param cal
	 * @return
	 */
	public static String getShortDate(GregorianCalendar cal){
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH)+1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		
		return String.format("%d/%02d/%02d", year, month, day);
	}
	



}
