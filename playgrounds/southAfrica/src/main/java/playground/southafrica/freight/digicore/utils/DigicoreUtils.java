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
import org.matsim.core.utils.io.IOUtils;

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
	 * Reads in a {@link DigicoreVehicle} from a given {@link File}.
	 * @param vehicleFile
	 * @return {@link DigicoreVehicle}
	 * @throws IOException
	 * 
	 * FIXME I have to relook at how vehicle files are read in!! (jwj 201304)
	 */
//	public static DigicoreVehicle readDigicoreVehicle(File vehicleFile){
//		synchronized (vehicleFile) {
//			if(!vehicleFile.exists() || !vehicleFile.canRead()){
//				LOG.warn("Cannot read DigicoreVehicle from " + vehicleFile.getAbsolutePath());
//				LOG.warn("Returning null.");
//			}
//			MyXmlConverter mxc = new MyXmlConverter(true);
//			Object o = mxc.readObjectFromFile(vehicleFile.getAbsolutePath());
//			if(o instanceof DigicoreVehicle){
//				return (DigicoreVehicle) o;
//			} else{
//				LOG.warn("Object is not of type DigicoreVehicle.");
//			}			
//		}
//		return null;
//	}


	
}
