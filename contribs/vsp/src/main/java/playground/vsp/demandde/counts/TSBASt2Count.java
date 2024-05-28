package playground.vsp.demandde.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.referencing.FactoryException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.vsp.demandde.counts.BastHourlyCountData.Day;

/** 
 *<pre>
 *this class reads hourly traffic data from BASt counting stations out of a .txt file,
 *averages it per station and converts it into one MATSim Counts file, written out to <b>OUTPUTDIR</b>.
 *(see <link> http://www.bast.de/DE/FB-V/Fachthemen/v2-verkehrszaehlung/Verkehrszaehlung.html to download data </link>)
 *
 *The resulting MATSim counts have station names set by the following pattern:
 *<b><i>BASt_ID-DAY-DIRECTION</i></b> where DAY is either WEEKDAY or WEEKEND.
 *Their linkID is currently set to the same.
 *Every original BASt counting station is converted into two Count objects, one for each direction.
 *The interval of the weekdays to be considered goes from <b>BEGINNING_WEEKDAY </b> to
 *<b>ENDING_WEEKDAY</b>, 1 representing Monday... 5 Friday.
 *<b><i>CALC_WEEKENDS</i></b> defines whether weekends should be considered. Weekends are defined from Saturday to Sunday. In addition, every public holiday is considered.
 *
 *<b>NOTE: </b> only correct values and corrected (estimated) values in the BASt file are considered. (see BASt datensatzbeschreibung).
 * </pre>
 * @author Tilmann Schlenther
 */
public class TSBASt2Count {
	
	private static final Logger logger = LogManager.getLogger(TSBASt2Count.class);

	private final static String bastInputFile = "C:/Users/Tille/WORK/BASt/2013_A_S.txt";
	
	//note: should end with .gz since it is probably quite big
	private final static String OUTPUTDIR = "C:/Users/Tille/WORK/BASt/2013_A_S_TUE_THU_WKEND.xml.gz";
	
	private final static int BEGINNING_WEEKDAY = 2;
	private final static int ENDING_WEEKDAY = 4;
	
	private final static boolean CALC_WEEKENDS = true;
	
	public static void main(String[] args) throws IOException, ParseException, FactoryException {
		
		Map<String,BastHourlyCountData> allCounts = new HashMap<String,BastHourlyCountData>();
		
		logger.info("Loading bast counts...");
		BufferedReader reader = new BufferedReader(new FileReader(bastInputFile));
		
		String line = reader.readLine();
		String[] header = line.split(";");
		Map<String, Integer> colIndices = new HashMap<String, Integer>();
		for(int i = 0; i < header.length; i++) {
			header[i].trim();
			colIndices.put(header[i], i);
		}
		
		NumberFormat format = NumberFormat.getInstance(Locale.US);
		
		int indexCountNr = colIndices.get("Zst");
		int indexVolumeD1 = colIndices.get("KFZ_R1");
		int indexValidityOfD1 = colIndices.get("K_KFZ_R1");
		int indexVolumeD2 = colIndices.get("KFZ_R2");
		int indexValidityOfD2 = colIndices.get("K_KFZ_R2");
		int indexHour = colIndices.get("Stunde");
		int indexDayOfWeek = colIndices.get("Wotag");
		int indexPurpose = colIndices.get("Fahrtzw");
		
		int lineNr = 1;
		
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split(";", -1);
			int weekDay = format.parse(tokens[indexDayOfWeek].trim()).intValue();
			
			boolean isConsideredWeekDay = (tokens[indexPurpose].equals("w") && (weekDay >= BEGINNING_WEEKDAY && weekDay <= ENDING_WEEKDAY)); 
			boolean isConsideredWeekEnd = (CALC_WEEKENDS && (tokens[indexPurpose].equals("s") || weekDay == 6));
			
			if(isConsideredWeekDay || isConsideredWeekEnd ){
				int stationNumber = format.parse(tokens[indexCountNr]).intValue();
				
				Day day = (isConsideredWeekDay) ? Day.WEEKDAY : Day.WEEKEND;
				String identifier = "" + stationNumber + "-" + day;
				BastHourlyCountData data = allCounts.get(identifier);
				if(data == null){
					data = new BastHourlyCountData(identifier, day);
				}
				
				int hour = format.parse(tokens[indexHour]).intValue();
				switch(tokens[indexValidityOfD1]){
				case "-":
				case "s":
				case "k":	
					data.computeAndSetVolume(true, hour, format.parse(tokens[indexVolumeD1].trim()).doubleValue());
				}
				switch(tokens[indexValidityOfD2]){
				case "-":
				case "s":
				case "k":	
					data.computeAndSetVolume(false, hour, format.parse(tokens[indexVolumeD2].trim()).doubleValue());
				}
				allCounts.put(identifier, data);
			}
			if(lineNr % 100000 == 0){
				logger.info("read line " + lineNr);
			}
			lineNr ++;
		}
		reader.close();

		logger.info("start converting to MATSim counts..");
		
		Counts result = new Counts();
		result.setName("BASt Zählstellen - Jahreesdurchschnitt");
		result.setYear(2013);
		
		for(BastHourlyCountData data : allCounts.values()){
			
			String str = data.getId() + "-D1";
			Id<Link> id = Id.createLinkId(str);
			Count currentCount = result.createAndAddCount(id, str);
			if(currentCount != null) {
				for(int i = 1; i < 25; i++) {
					Double value = data.getR1Values().get(i);
					if(value == null){
						logger.error("station " + str + " has no valid entry for hour " + i +". Please check this. Instead, a negative volume for this hour is written into the counts file.");
						value = -1.0;
					}
					currentCount.createVolume(i, value);
				}
			} else {
				logger.warn("Cannot add two counts for one link (" + str+").");
			}
			
			str = data.getId() + "-D2";
			id = Id.createLinkId(str);
			currentCount = result.createAndAddCount(id, str);
			if(currentCount != null) {
				for(int i = 1; i < 25; i++) {
					Double value = data.getR2Values().get(i);
					if(value == null){
						logger.error("station " + str + " has no valid entry for hour " + i +". Please check this. Instead, a negative volume for this hour is written into the counts file.");
						value = -1.0;
					}
					currentCount.createVolume(i, value);
				}
			} else {
				logger.warn("Cannot add two counts for one link (" + str+").");
			}
		}
		
		logger.info("writing counts to " + OUTPUTDIR);
		CountsWriter writer = new CountsWriter(result);
		writer.write(OUTPUTDIR);
		
		logger.info("conversion FINISHED...");
		
//		System.out.println(" allcounts:  (countsdata) \n");
//		for(BastHourlyCountData data : allCounts.values()){
//			System.out.println(data.toString());
//		}
	}
}








