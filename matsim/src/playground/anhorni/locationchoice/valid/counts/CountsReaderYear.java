package playground.anhorni.locationchoice.valid.counts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;


public class CountsReaderYear {
	
	private final static Logger log = Logger.getLogger(CountsReaderYear.class);
	TreeMap<String, Vector<RawCount>> rawCounts = new TreeMap<String, Vector<RawCount>>();
	
	public void read(String datasetsfile) {
		List<String> paths = new Vector<String>();
		try {
			FileReader fileReader = new FileReader(datasetsfile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);	
			
			String curr_line;			
			while ((curr_line = bufferedReader.readLine()) != null) {
				String dataset = curr_line;
				paths.add(dataset);
			}	
			bufferedReader.close();
			fileReader.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.readFiles(paths);
	}
	
	private void readFiles(final List<String> paths) {
		try {
			
			for (int i = 0; i < paths.size(); i++) {
				FileReader fileReader = new FileReader(paths.get(i));
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				log.info("Reading: " + paths.get(i));
				
				String curr_line = bufferedReader.readLine(); // Skip header
								
				while ((curr_line = bufferedReader.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);											
					String dataset = entries[0].trim();
					String nr = entries[1].trim();
					String year = entries[3].trim();
					String month = entries[4].trim();
					String day = entries[5].trim();
					String hour = entries[6].trim();					
					String vol1 = entries[7].trim();
					String vol2 = entries[8].trim();
					
					if (vol2.length() == 0) {
						vol2 = "-1";
					}
												
					RawCount rawCount = new RawCount(dataset+nr, year, month, day, hour, vol1, vol2);
					
					//if (Integer.parseInt(vol1) > -0.5 && Integer.parseInt(vol2) > 0.5) {
						if (rawCounts.get(dataset + nr) == null) {
							rawCounts.put(dataset + nr, new Vector<RawCount>());
						}
						rawCounts.get(dataset + nr).add(rawCount);	
					//}
				}	
				bufferedReader.close();
				fileReader.close();
			}
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		this.cleanRawCounts();
	}
	
	
	private List<String> getStationDays2ThrowAway() {
		List<String> stationDays2ThrowAway = new Vector<String>();
		
		Iterator<Vector<RawCount>> rawCountStation_it = this.rawCounts.values().iterator();
		while (rawCountStation_it.hasNext()) {
			Vector<RawCount> rawCountsListPerStation = rawCountStation_it.next();
			
			Iterator<RawCount> rawCount_it = rawCountsListPerStation.iterator();
			while (rawCount_it.hasNext()) {
				RawCount rawCount = rawCount_it.next();
				
				if (rawCount.getVol1() < -0.5 || rawCount.getVol2() < -0.5) {
					if (!stationDays2ThrowAway.contains(rawCount.getId() + "_" + this.convert(rawCount))) {
						stationDays2ThrowAway.add(rawCount.getId() + "_" + this.convert(rawCount));
					}
				}
			}
		}
		return stationDays2ThrowAway;
	}
	
	// clean rawCounts. I.e., throw away days with data gaps
	private void cleanRawCounts() {		
		List<String> stationDays2ThrowAway = getStationDays2ThrowAway();
		log.info("StationDays to throw away: \t" + stationDays2ThrowAway.size());
		TreeMap<String, Vector<RawCount>> rawCountsFiltered = new TreeMap<String, Vector<RawCount>>();
		
		Iterator<Vector<RawCount>> rawCountStation_it = this.rawCounts.values().iterator();
		while (rawCountStation_it.hasNext()) {
			Vector<RawCount> rawCountsListPerStation = rawCountStation_it.next();
			
			Iterator<RawCount> rawCount_it = rawCountsListPerStation.iterator();
			while (rawCount_it.hasNext()) {
				RawCount rawCount = rawCount_it.next();
							
				if (!stationDays2ThrowAway.contains(rawCount.getId() + "_" + this.convert(rawCount))) {
					if (rawCountsFiltered.get(rawCount.getId()) == null) {
						rawCountsFiltered.put(rawCount.getId(), new Vector<RawCount>());
					}
					rawCountsFiltered.get(rawCount.getId()).add(rawCount);					
				}
			}
		}
		this.rawCounts = rawCountsFiltered;
	}

	public TreeMap<String, Vector<RawCount>> getRawCounts() {
		return rawCounts;
	}
	
	private String convert(RawCount rawCount) {
		String year = String.valueOf(rawCount.getYear());
		if (rawCount.getYear() < 2000) {
			year = "20" + rawCount.getYear();
		}
		String month = String.valueOf(rawCount.getMonth());
		if (rawCount.getMonth() < 10) {
			month = "0" + rawCount.getMonth();
		}
		String day = String.valueOf(rawCount.getDay());
		if (rawCount.getDay() < 10) {
			day = "0" + rawCount.getDay();
		}
		return year + month + day;
	}
}
