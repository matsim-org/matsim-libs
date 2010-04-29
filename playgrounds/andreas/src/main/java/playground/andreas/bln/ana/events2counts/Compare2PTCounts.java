package playground.andreas.bln.ana.events2counts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;



public class Compare2PTCounts extends Events2PTCounts{
	
	String inDir;

	public Compare2PTCounts(String inDir, String countsOutFile, String eventsInFile, String stopIDMapFile, String networkFile, String transitScheduleFile) throws IOException {
		super(inDir, countsOutFile, eventsInFile, stopIDMapFile, networkFile, transitScheduleFile);
		this.inDir = inDir;
	}

	private final static Logger log = Logger.getLogger(Compare2PTCounts.class);
	
	
	public static void main(String[] args) {
		String inDir = "e:/_out/countsTest/";
		try {
			new Compare2PTCounts(inDir, "countsFile.txt", "0.events.xml.gz", "stopareamap.txt", "network.xml", "transitSchedule.xml").compare();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void compare() {
		
		
			
			// compare counts 2 minus counts 1
			String parentDir = this.outDir;
			this.outDir = parentDir + "count1/";
			this.run();
			Map<Id, Map<Id, StopCountBox>> countsMap1 = this.getLine2StopCountMap();
			
			reset();
			this.eventsInFile = this.inDir + "10.events.xml.gz";
			this.transitSchedule = ReadTransitSchedule.readTransitSchedule(this.inDir + "network.xml", this.inDir + "transitSchedule.xml");
			this.outDir = parentDir + "count2/";
			this.run();
			Map<Id, Map<Id, StopCountBox>> countsMap2 = this.getLine2StopCountMap();
//			countsMap2.put(new IdImpl("344  "), null);
			
			TreeSet<Id> unionOfLineIds = new TreeSet<Id>();
			unionOfLineIds.addAll(countsMap1.keySet());
			unionOfLineIds.addAll(countsMap2.keySet());
			
			Map<Id, Map<Id, StopCountBox>> mergedMap = new HashMap<Id, Map<Id,StopCountBox>>();
			
			for (Id lineId : unionOfLineIds) {
				
				if(countsMap1.get(lineId) != null){
					if(countsMap2.get(lineId) != null){
						// both != null -> compare 2 minus 1
						mergedMap.put(lineId, compareMapEntries(countsMap1.get(lineId), countsMap2.get(lineId)));
						
					} else {
						// 2 == null -> take inverted 1
						invertMapEntries(countsMap1.get(lineId));
						mergedMap.put(lineId, countsMap1.get(lineId));
					}
				} else {
					if(countsMap2.get(lineId) != null){
						// 1 == null -> take 2
						mergedMap.put(lineId, countsMap2.get(lineId));
					} else {
						// both == null -> take none
						log.warn("No counts data for line " + lineId);
					}
				}				
			}
			
			this.outDir = parentDir + "count2-1/";
			this.line2StopCountMap = mergedMap;
			this.dump();
						
			log.info("Finished");
			
			
		
				
	}
	
	private void reset() {
		this.line2MainLinesMap = null;
		this.line2StopCountMap = new HashMap<Id, Map<Id,StopCountBox>>();
		this.vehID2LineMap = null;
		
	}

	private Map<Id, StopCountBox> compareMapEntries(Map<Id, StopCountBox> countsMap1, Map<Id, StopCountBox> countsMap2) {
		TreeSet<Id> unionOfStopIds = new TreeSet<Id>();
		unionOfStopIds.addAll(countsMap1.keySet());
		unionOfStopIds.addAll(countsMap2.keySet());
		
		Map<Id, StopCountBox> mergedMap = new HashMap<Id,StopCountBox>();
		
		for (Id stopId : unionOfStopIds) {
			
			if(countsMap1.get(stopId) != null){
				if(countsMap2.get(stopId) != null){
					// both != null -> compare 2 minus 1
					for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
						countsMap1.get(stopId).accessCount[i] = countsMap2.get(stopId).accessCount[i] - countsMap1.get(stopId).accessCount[i];
						countsMap1.get(stopId).egressCount[i] = countsMap2.get(stopId).egressCount[i] - countsMap1.get(stopId).egressCount[i];
					}
					mergedMap.put(stopId, countsMap1.get(stopId));
					
				} else {
					// 2 == null -> take inverted 1
					for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
						countsMap1.get(stopId).accessCount[i] *= -1;
						countsMap1.get(stopId).egressCount[i] *= -1;
					}
					mergedMap.put(stopId, countsMap1.get(stopId));
				}
			} else {
				if(countsMap2.get(stopId) != null){
					// 1 == null -> take 2
					mergedMap.put(stopId, countsMap2.get(stopId));
				} else {
					// both == null -> take none
					log.warn("No counts data for line " + stopId);
				}
			}
		}
		
		return mergedMap;
		
	}

	private void invertMapEntries(Map<Id, StopCountBox> routeMap){
		
		for (Entry<Id, StopCountBox> routeEntry : routeMap.entrySet()) {
			for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
				routeEntry.getValue().accessCount[i] *= -1;
				routeEntry.getValue().egressCount[i] *= -1;
			}
		}
		
	}
}
