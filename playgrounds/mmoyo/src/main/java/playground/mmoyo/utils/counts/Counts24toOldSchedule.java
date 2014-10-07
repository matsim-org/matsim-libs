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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.TabularRouteFile_reader;

public class Counts24toOldSchedule {
	final String point = ".";
	
	private void run(){
		//read relation old and new routes
		TabularRouteFile_reader tabularRouteFile_reader= new TabularRouteFile_reader();
		try {
			tabularRouteFile_reader.readFile("../../input/newDemand/apr2012/cal192/input/schedule-new24counts2.txt");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Map <Id<TransitRoute>, String> old_new_route_Map = tabularRouteFile_reader.getRouteMap(); 
		
		//read new counts
		String filePath= "../../input/newDemand/apr2012/cal192/input/bvg.run190.25pct.100.filtered.ptLineCounts.txt";
		TabularCount_reader tabCount_reader= new TabularCount_reader();
		try {
			tabCount_reader.readFile(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//create a map newRouteId_CountRecord
		TreeMap<String, List<TabularCountRecord>> strNewRouteId_CountRecordMap = new TreeMap <String, List<TabularCountRecord>>();
		for (TabularCountRecord countRecord : tabCount_reader.getCountRecordMap().values()){
			String strRoute = countRecord.getLineId().toString() + this.point + countRecord.getDirection();
			
			if (!strNewRouteId_CountRecordMap.keySet().contains(strRoute)){
				List<TabularCountRecord> countRecList = new ArrayList<TabularCountRecord>();
				strNewRouteId_CountRecordMap.put(strRoute, countRecList);
			}
			strNewRouteId_CountRecordMap.get(strRoute).add(countRecord);
		}
		/*for(Map.Entry <String, List<CountRecord>> entry: strNewRouteId_CountRecordMap.entrySet() ){
			String key = entry.getKey();
			List<CountRecord> value = entry.getValue();
			System.out.println(key + " " + value.toString());
		}
		System.exit(1);*/
		
		//read old Schedule
		final String SCHEDULEFILE = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(SCHEDULEFILE);
		
		Counts counts= new Counts(); //create new 24 hrs counts
		counts.setYear(2009);
		counts.setDescription("accumulated counts from https://svn.vsp.tu-berlin.de/repos/runs-svn/berlin-bvg09/bvg.run190.25pct/ITERS/it.100/bvg.run190.25pct.100.ptLineCounts.txt");
		for(TransitLine line: schedule.getTransitLines().values()){
			//if(!line.getId().toString().equals("B-100")){ continue;}
			
			for (TransitRoute route : line.getRoutes().values()){
				Id oldRouteId = route.getId();
				//System.out.println(oldRouteId.toString()); 
				String newRouteId = old_new_route_Map.get(oldRouteId);
				
				if(newRouteId!=null){
					for (TransitRouteStop stop : route.getStops()){
						//create oldPseudoStop
						Id stopFacilityId = stop.getStopFacility().getId();
						//String realStopId =  stopFacilityId.toString();
						//int pseudoLenght = realStopId.length();
						//int pointIndex = realStopId.indexOf(point);
						//if ( pointIndex > -1 ){
						//	pseudoLenght = pointIndex; 
						//}
						//String strOldPseudoStopId = realStopId.substring(0, pseudoLenght);
						String strOldPseudoStopId = Real2PseudoId.convertRealIdtoPseudo(stopFacilityId.toString());	
						
						//find equivalent in newStops
						List<TabularCountRecord> countRecordList = strNewRouteId_CountRecordMap.get(newRouteId);
						for (TabularCountRecord countRecord : countRecordList){
							if (strOldPseudoStopId.equals(countRecord.getStop())){
								System.out.println( route.getId()  + "\t" +  newRouteId +  "\t" + stopFacilityId + "\t" + countRecord.getCount());
							
								//create or invoke the count and sum its volume
								Count count = counts.getCounts().get(stopFacilityId);
								if(count == null){
									count = counts.createAndAddCount(stopFacilityId, stopFacilityId.toString());
									count.setCoord(stop.getStopFacility().getCoord());
									count.createVolume(1, 0.0);
								}
								
								double oldValue = count.getVolume(1).getValue(); 
								count.getVolume(1).setValue(oldValue + countRecord.getCount());
							}//if (strOldPseudoStopId
							
						}//for (CountRecord
						
					}//for (TransitRouteStop
					
				}else{
					//System.out.println(" no relation");
				}
			}//for (TransitRoute
		}//for(TransitLine
		
		//write counts
		CountsWriter counts_writer = new CountsWriter(counts);
		counts_writer.write("../../input/newDemand/apr2012/cal192/input/fromStopsNew_24hrs_counts.xml");
		
	}//private

	public static void main(String[] args) {
		Counts24toOldSchedule counts24toOldSchedule = new Counts24toOldSchedule();
		counts24toOldSchedule.run();
	}
}
