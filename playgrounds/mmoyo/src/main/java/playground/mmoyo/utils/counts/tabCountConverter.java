/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mmoyo.utils.counts;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

/**
  * Reads a tabular file with 24hrs counts and converts it to standard counts format.
  */
public class tabCountConverter {
	
	public tabCountConverter(){
		
	}
	
	private void run(TransitSchedule schedule, Map<Integer, TabularCountRecord> countRecordMap, String outFile){
		Map <String, Double> stopsCountsMap = new TreeMap <String, Double>();
		for (TabularCountRecord countRecord : countRecordMap.values()){
			if(!stopsCountsMap.keySet().contains(countRecord.getStop())){
				stopsCountsMap.put(countRecord.getStop(), 0.0);	
			}
			double tmp = stopsCountsMap.get(countRecord.getStop());
			stopsCountsMap.put(countRecord.getStop(), tmp + countRecord.getCount());
		}
		

		TransitScheduleFactoryImpl  zoneFactory = new TransitScheduleFactoryImpl();
		
		//look for the name of the csId 
		//convert all facStops from schedule into stopZone
		Map <Id, TransitStopFacility> trstopToZoneStopMap = new TreeMap <Id, TransitStopFacility>();
		for(Entry<Id<TransitStopFacility>, TransitStopFacility> facEntry: schedule.getFacilities().entrySet() ){
			Id<TransitStopFacility> zoneId = convertRealIdtoPseudo(facEntry.getKey());
			TransitStopFacility stopZone = zoneFactory.createTransitStopFacility(zoneId, facEntry.getValue().getCoord(), false);
			stopZone.setName(facEntry.getValue().getName());
			
			//it was already validated that stop facility have the same name and coord. Then they can be converted into zones
			if(!trstopToZoneStopMap.keySet().contains(zoneId)){
				trstopToZoneStopMap.put(zoneId,stopZone);	
			}
		}
		
		//create counts
		Counts counts = new Counts();
		counts.setYear(2009);
		for(Map.Entry <String,Double> entry: stopsCountsMap.entrySet() ){
			Id<Link> countZoneId = Id.create(entry.getKey(), Link.class);
			TransitStopFacility stopZone = trstopToZoneStopMap.get(countZoneId);
			if(stopZone!=null){
				Count count = counts.createAndAddCount(countZoneId, "");
				//System.out.println(countZoneId);
				if(stopZone.getName()!=null){
					count.setCsId(stopZone.getName());	
				}
				count.setCoord(stopZone.getCoord());	
			
				//create the count 1 and the other 23 empty counts
				count.createVolume(1, entry.getValue());
				for(int i=2; i<25; i++){
					count.createVolume(i, 0.0);
				}
				
			}
		}
	
		CountsWriter countsWriter = new CountsWriter(counts);
		countsWriter.write(outFile);
	}
	
	
	  protected static Id<TransitStopFacility> convertRealIdtoPseudo(Id<TransitStopFacility> realId)
	  {
	    String strRealId = realId.toString();

	    int pseudoLenght = strRealId.length();
	    int pointIndex = strRealId.indexOf('.');
	    if (pointIndex > -1) {
	      pseudoLenght = pointIndex;
	    }
	    Id<TransitStopFacility> pseudoId = Id.create(strRealId.substring(0, pseudoLenght), TransitStopFacility.class);
	    return pseudoId;
	  }
	
	public static void main(String[] args)  {
		String tabFile = "../../bvg.run189.10pct.100.ptLineCounts.txt";
		String scheduleFile = "../../input/newDemand/transitSchedule.xml";
		
		//load data
		TransitSchedule schedule = new DataLoader().readTransitSchedule(scheduleFile);	
		
		TabularCount_reader reader = new TabularCount_reader();
		try {
			reader.readFile(tabFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file = new File(tabFile);
		String outFile = file.getParent()  + File.separatorChar + file.getName() + "aggregatedCountsFilteredNewSchedule.xml";
		new tabCountConverter().run(schedule, reader.getCountRecordMap(), outFile);
		
	}
	
	
}
