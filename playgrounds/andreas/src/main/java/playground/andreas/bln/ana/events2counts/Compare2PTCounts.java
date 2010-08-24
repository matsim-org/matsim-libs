package playground.andreas.bln.ana.events2counts;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.Counts;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.transitSchedule.api.TransitSchedule;



public class Compare2PTCounts extends Events2PTCounts{
	
	static String inDir = "F:/counts2/";
	static String run1 = "767";
	static String run2 = "768";
	static String iteration = "500";
	

	public Compare2PTCounts(String outDir, String eventsInFile, String stopIDMapFile, String networkFile, String transitScheduleFile) throws IOException {
		super(outDir, eventsInFile, stopIDMapFile, networkFile, transitScheduleFile);
	}

	private final static Logger log = Logger.getLogger(Compare2PTCounts.class);
	
	
	public static void main(String[] args) {
		try {
			new Compare2PTCounts(Compare2PTCounts.inDir, inDir + Compare2PTCounts.run1 + "." + Compare2PTCounts.iteration + ".events.xml.gz", 
					Compare2PTCounts.inDir + "stopareamap.txt", 
					Compare2PTCounts.inDir + "network.xml.gz", 
					Compare2PTCounts.inDir + "transitSchedule.xml.gz").compare();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void compare() {		
			
			// compare counts 2 minus counts 1
			String parentDir = this.outDir;
			this.outDir = parentDir + Compare2PTCounts.run1 + "/";
			(new File(this.outDir)).mkdir();
			this.run();
			Map<Id, Map<Id, StopCountBox>> countsMap1 = this.getLine2StopCountMap();
			
			reset();
			this.eventsInFile = Compare2PTCounts.inDir + Compare2PTCounts.run2 + "." + Compare2PTCounts.iteration + ".events.xml.gz";
			this.transitSchedule = ReadTransitSchedule.readTransitSchedule(Compare2PTCounts.inDir + "network.xml.gz", Compare2PTCounts.inDir + "transitSchedule_long.xml.gz");
			this.outDir = parentDir + Compare2PTCounts.run2 + "/";
			(new File(this.outDir)).mkdir();
			this.run();
			Map<Id, Map<Id, StopCountBox>> countsMap2 = this.getLine2StopCountMap();
//			countsMap2.put(new IdImpl("344  "), null);
			
			this.outDir = parentDir + Compare2PTCounts.run2 + "-" + Compare2PTCounts.run1 + "/";
			(new File(this.outDir)).mkdir();
			
			createSimpleKMZ(countsMap1, countsMap2, this.transitSchedule);
			
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
			
			this.line2StopCountMap = mergedMap;
			this.check();
			this.dump();
						
			log.info("Finished");		
				
	}
	
	private void createSimpleKMZ(Map<Id, Map<Id, StopCountBox>> line2StopCountMap1, Map<Id, Map<Id, StopCountBox>> line2StopCountMap2, TransitSchedule transitSchedule) {
	
		HashMap<String, String> stringStopNameMap = new HashMap<String, String>();
		for (Entry<Id, String> stopEntry : this.stopID2NameMap.entrySet()) {
			stringStopNameMap.put(stopEntry.getKey().toString(), stopEntry.getValue());
		}
		
		Map<String, TreeSet<String>> stopID2lineIdMap = new HashMap<String, TreeSet<String>>();
		
		Map<Id, StopCountBox> stopCounts1 = new HashMap<Id, StopCountBox>();	
		for (Id lineId : line2StopCountMap1.keySet()) {			
			for (Entry<Id, StopCountBox> stopBox : line2StopCountMap1.get(lineId).entrySet()) {				
				Id stopId = new IdImpl(stopBox.getKey().toString().split("\\.")[0]);
				if(stopCounts1.get(stopId) == null){
					stopCounts1.put(stopId, stopBox.getValue());
				} else {
					for (int i = 0; i < 24; i++) {
						stopCounts1.get(stopId).accessCount[i] = stopCounts1.get(stopId).accessCount[i] + stopBox.getValue().accessCount[i];
						stopCounts1.get(stopId).egressCount[i] = stopCounts1.get(stopId).egressCount[i] + stopBox.getValue().egressCount[i];
					}
				}
				
				// add line to its stops
				if(stopID2lineIdMap.get(stopId.toString()) == null){
					stopID2lineIdMap.put(stopId.toString(), new TreeSet<String>());
				}				
				stopID2lineIdMap.get(stopId.toString()).add(lineId.toString());
			}
		}
		
		Map<Id, StopCountBox> stopCounts2 = new HashMap<Id, StopCountBox>();		
		for (Id lineId : line2StopCountMap2.keySet()) {			
			for (Entry<Id, StopCountBox> stopBox : line2StopCountMap2.get(lineId).entrySet()) {				
				Id stopId = new IdImpl(stopBox.getKey().toString().split("\\.")[0]);
				if(stopCounts2.get(stopId) == null){
					stopCounts2.put(stopId, stopBox.getValue());
				} else {
					for (int i = 0; i < 24; i++) {
						stopCounts2.get(stopId).accessCount[i] = stopCounts2.get(stopId).accessCount[i] + stopBox.getValue().accessCount[i];
						stopCounts2.get(stopId).egressCount[i] = stopCounts2.get(stopId).egressCount[i] + stopBox.getValue().egressCount[i];
					}
				}
				
				// add line to its stops
				if(stopID2lineIdMap.get(stopId.toString()) == null){
					stopID2lineIdMap.put(stopId.toString(), new TreeSet<String>());
				}				
				stopID2lineIdMap.get(stopId.toString()).add(lineId.toString());
			}
		}		
		
		Counts alightCounts = new Counts();
		Counts boardCounts = new Counts();
		
		for (StopCountBox stopCountBox : stopCounts1.values()) {

			Id stopId = new IdImpl(stopCountBox.stopId.toString().split("\\.")[0]);
			alightCounts.createCount(stopId, stopCountBox.realName);
			boardCounts.createCount(stopId, stopCountBox.realName);

			alightCounts.getCount(stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());
			boardCounts.getCount(stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());

			for (int i = 0; i < 24; i++) {
				alightCounts.getCount(stopId).createVolume(i, stopCountBox.egressCount[i]);
				boardCounts.getCount(stopId).createVolume(i, stopCountBox.accessCount[i]);
			}			

		}
		
		for (StopCountBox stopCountBox : stopCounts2.values()) {
			
			Id stopId = new IdImpl(stopCountBox.stopId.toString().split("\\.")[0]);
			alightCounts.createCount(stopId, stopCountBox.realName);
			boardCounts.createCount(stopId, stopCountBox.realName);

			alightCounts.getCount(stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());
			boardCounts.getCount(stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());

			for (int i = 0; i < 24; i++) {
				alightCounts.getCount(stopId).createVolume(i, stopCountBox.egressCount[i]);
				boardCounts.getCount(stopId).createVolume(i, stopCountBox.accessCount[i]);
			}			

		}
		
		
		List<CountSimComparison> boardCountSimCompList = new LinkedList<CountSimComparison>();
		List<CountSimComparison> alightCountSimCompList = new LinkedList<CountSimComparison>();
		for (Entry<Id, StopCountBox> stopEntry : stopCounts2.entrySet()) {
			
			StopCountBox stopCountBox1 = stopCounts1.get(stopEntry.getKey());
			if(stopCountBox1 == null){
				stopCountBox1 = new StopCountBox(stopEntry.getKey(), stopEntry.getValue().realName);
			}
			StopCountBox stopCountBox2 = stopCounts2.get(stopEntry.getKey());
			if(stopCountBox2 == null){
				stopCountBox2 = new StopCountBox(stopEntry.getKey(), stopEntry.getValue().realName);
			}
			for (int i = 0; i < 24; i++) {
				boardCountSimCompList.add(new CountSimComparisonImpl(stopEntry.getKey(), i + 1, stopCountBox1.accessCount[i], stopCountBox2.accessCount[i]));
				alightCountSimCompList.add(new CountSimComparisonImpl(stopEntry.getKey(), i + 1, stopCountBox1.egressCount[i], stopCountBox2.egressCount[i]));
			}			
			
		}		
		
		PtCountCountComparisonKMLWriter kmlWriter = new PtCountCountComparisonKMLWriter(boardCountSimCompList, alightCountSimCompList,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84),
				boardCounts, alightCounts, stringStopNameMap, stopID2lineIdMap, true);
		kmlWriter.setIterationNumber(0);
		kmlWriter.writeFile(this.outDir + "compare_withNames.kmz");
		
		kmlWriter = new PtCountCountComparisonKMLWriter(boardCountSimCompList, alightCountSimCompList,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84),
				boardCounts, alightCounts, stringStopNameMap, stopID2lineIdMap, false);
		kmlWriter.setIterationNumber(0);
		kmlWriter.writeFile(this.outDir + "compare_woNames.kmz");
		
	}
	
	private void createKMZ(Map<Id, Map<Id, StopCountBox>> line2StopCountMap1, Map<Id, Map<Id, StopCountBox>> line2StopCountMap2, TransitSchedule transitSchedule) {
		
		Counts alightCounts = new Counts();
		Counts boardCounts = new Counts();
		
		for (Entry<Id, Map<Id, StopCountBox>> lineEntry : line2StopCountMap2.entrySet()) {
			for (StopCountBox stopCountBox : lineEntry.getValue().values()) {
				
				alightCounts.createCount(stopCountBox.stopId, stopCountBox.realName);
				boardCounts.createCount(stopCountBox.stopId, stopCountBox.realName);
				
				alightCounts.getCount(stopCountBox.stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());
				boardCounts.getCount(stopCountBox.stopId).setCoord(transitSchedule.getFacilities().get(stopCountBox.stopId).getCoord());
				
				for (int i = 0; i < stopCountBox.accessCount.length; i++) {
					alightCounts.getCount(stopCountBox.stopId).createVolume(i, stopCountBox.egressCount[i]);
					boardCounts.getCount(stopCountBox.stopId).createVolume(i, stopCountBox.accessCount[i]);
				}				
			}			
		}
		
		List<CountSimComparison> boardCountSimCompList = new LinkedList<CountSimComparison>();
		List<CountSimComparison> alightCountSimCompList = new LinkedList<CountSimComparison>();
		for (Id lineId : line2StopCountMap2.keySet()) {
			for (Entry<Id, StopCountBox> stopCountBoxEntry : line2StopCountMap2.get(lineId).entrySet()) {
				StopCountBox stopCountBox1 = line2StopCountMap1.get(lineId).get(stopCountBoxEntry.getKey());
				if(stopCountBox1 == null){
					stopCountBox1 = new StopCountBox(stopCountBoxEntry.getKey(), stopCountBoxEntry.getValue().realName);
				}
				StopCountBox stopCountBox2 = line2StopCountMap2.get(lineId).get(stopCountBoxEntry.getKey());
				if(stopCountBox2 == null){
					stopCountBox2 = new StopCountBox(stopCountBoxEntry.getKey(), stopCountBoxEntry.getValue().realName);
				}
				for (int i = 0; i < 24; i++) {
//					Id tempId = new IdImpl(lineId + " - " + stopCountBox.stopId + " - " + stopCountBox.realName);
//					Id tempId = stopCountBox2.stopId;
//					if(stopCountBox1.accessCount[i] != 0){
//						log.info("");
//					}
					boardCountSimCompList.add(new CountSimComparisonImpl(stopCountBoxEntry.getKey(), i + 1, stopCountBox1.accessCount[i], stopCountBox2.accessCount[i]));
					alightCountSimCompList.add(new CountSimComparisonImpl(stopCountBoxEntry.getKey(), i + 1, stopCountBox1.egressCount[i], stopCountBox2.egressCount[i]));
				}
			}
			
		}
		
		
		PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(boardCountSimCompList, alightCountSimCompList,
				TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84), boardCounts, alightCounts);
		kmlWriter.setIterationNumber(0);
		kmlWriter.writeFile(this.outDir + "out.kmz");
		
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
					for (int i = 0; i < StopCountBox.slots; i++) {
						countsMap1.get(stopId).accessCount[i] = countsMap2.get(stopId).accessCount[i] - countsMap1.get(stopId).accessCount[i];
						countsMap1.get(stopId).egressCount[i] = countsMap2.get(stopId).egressCount[i] - countsMap1.get(stopId).egressCount[i];
					}
					mergedMap.put(stopId, countsMap1.get(stopId));
					
				} else {
					// 2 == null -> take inverted 1
					for (int i = 0; i < StopCountBox.slots; i++) {
						countsMap1.get(stopId).accessCount[i] = -countsMap1.get(stopId).accessCount[i];
						countsMap1.get(stopId).egressCount[i] = -countsMap1.get(stopId).egressCount[i];
					}
					mergedMap.put(stopId, countsMap1.get(stopId));
				}
			} else {
				if(countsMap2.get(stopId) != null){
					// 1 == null -> take 2
					mergedMap.put(stopId, countsMap2.get(stopId));
				} else {
					// both == null -> take none
					log.warn("No counts data for stop " + stopId);
				}
			}
		}
		
		return mergedMap;
		
	}

	private void invertMapEntries(Map<Id, StopCountBox> routeMap){
		
		for (Entry<Id, StopCountBox> routeEntry : routeMap.entrySet()) {
			for (int i = 0; i < StopCountBox.slots; i++) {
				routeEntry.getValue().accessCount[i] *= -1;
				routeEntry.getValue().egressCount[i] *= -1;
			}
		}
		
	}
}
