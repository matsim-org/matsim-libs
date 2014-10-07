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
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

public class Count_Line_Comparator {

	public Count_Line_Comparator(){
		
	}
	
	void run2(final TransitSchedule schedule, Map<Integer, TabularCountRecord> countRecordMap){
		
		Set<Id<TransitStopFacility>> stopsIdSet = schedule.getFacilities().keySet();
		for (TabularCountRecord tabularCountRecord : countRecordMap.values()){
			Id<TransitStopFacility> tabStopId = Id.create(tabularCountRecord.getStop(), TransitStopFacility.class);
			if (!stopsIdSet.contains(tabStopId)){
				System.out.println(tabStopId +  " not found");
			}			
		}
		
	}
	
	void run(final TransitSchedule schedule, Map<Integer, TabularCountRecord> countRecordMap){

		//create map of routes and their stop ids
		Map <Id, List<String>> route_stops_Map = new TreeMap <Id, List<String>>();
		for (TransitLine line: schedule.getTransitLines().values()){
			for (TransitRoute route: line.getRoutes().values()){
				List<String> stopIdList = new ArrayList<String>();
				for(TransitRouteStop stop : route.getStops()){
					String strStopId = stop.getStopFacility().getId().toString();
					String pseudoStopId = Real2PseudoId.convertRealIdtoPseudo(strStopId);
					stopIdList.add(pseudoStopId);
				}
				route_stops_Map.put(route.getId(), stopIdList);
			}
		}
		
		String h = "H";
		String r = "R";
		
		Id<TransitLine> line100Id = Id.create("100-B-100", TransitLine.class);
		for (TabularCountRecord tabCountRecord: countRecordMap.values()){
			TransitLine line = schedule.getTransitLines().get(tabCountRecord.getLineId());
			if(!line.getId().equals(line100Id)){
				return;
			} 
			
			boolean stopInLine = false;
			
			for (TransitRoute route : line.getRoutes().values()){
				
				System.out.println( route.getId() + " " + tabCountRecord.getDirection() + " 		" + (route.getId().toString().endsWith(r) && (tabCountRecord.getDirection() == r.charAt(0))));
				
				if (
					route.getId().toString().endsWith(h) && (tabCountRecord.getDirection() == h.charAt(0))   
					||
					route.getId().toString().endsWith(r) && (tabCountRecord.getDirection() == r.charAt(0)) 
					){ 

					stopInLine = stopInLine || route_stops_Map.get(route.getId()).contains( tabCountRecord.getStop() );
				}
				
			}
			
			if (!stopInLine){
				System.out.println(tabCountRecord.getStop() + " not in " + line.getId());
			}
		}
		
	}
	
	public static void main(String[] args) {
		String tabCountDataFile = "../../input/newDemand/bvg.run189.10pct.100.ptLineCounts.txt";
		String scheduleFile = "../../input/newDemand/rev554B-bvg00-0.1sample_transitSchedule.xml.gz";

		//read data
		DataLoader loader = new DataLoader();
		TransitSchedule schedule = loader.readTransitSchedule(scheduleFile);
		
		TabularCount_reader tabCount_reader= new TabularCount_reader();
		try {
			tabCount_reader.readFile(tabCountDataFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Count_Line_Comparator().run2(schedule, tabCount_reader.getCountRecordMap()); 
	}

}
