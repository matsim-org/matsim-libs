package playground.anhorni.counts;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

public class Cleaner {
	
	private final static Logger log = Logger.getLogger(Cleaner.class);
	//TreeMap<String, Vector<RawCount>> rawCounts = new TreeMap<String, Vector<RawCount>>();
		
	/*
	 * Identify the days of the station for which at least one volume (vol1 oder vol2) is smaller than -0.5
	 */
	private List<String> getStationDays2ThrowAway(TreeMap<String, Vector<RawCount>> rawCounts_) {
		List<String> stationDays2ThrowAway = new Vector<String>();
		
		Iterator<Vector<RawCount>> rawCountStation_it = rawCounts_.values().iterator();
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
	
	/* 
	 *  Clean rawCounts. I.e., throw away the days with data gaps (identified by getStationDays2ThrowAway)
	 *  Have to do it in 2 steps due to concurrency problems
	 */
	public TreeMap<String, Vector<RawCount>> cleanRawCounts(TreeMap<String, Vector<RawCount>> rawCounts_) {
		
		List<String> stationDays2ThrowAway = getStationDays2ThrowAway(rawCounts_);
		log.info("	Total number of days to throw away: \t" + stationDays2ThrowAway.size());
		TreeMap<String, Vector<RawCount>> rawCountsFiltered = new TreeMap<String, Vector<RawCount>>();
		
		int counter = 0;
		int nextMsg = 1;
		Iterator<Vector<RawCount>> rawCountStation_it = rawCounts_.values().iterator();
		log.info("		Number of stations to be cleaned: " + rawCounts_.values().size());
		while (rawCountStation_it.hasNext()) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" 			station # " + counter);
			}
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
		return rawCountsFiltered;
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
