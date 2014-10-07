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
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.utils.DataLoader;

public class NewCounts_OldScheduleMatch {

	public static void main(String[] args) {
		//read schedule
		final String SCHEDULEFILE = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(SCHEDULEFILE);
		
		//read counts
		String filePath= "../../input/newDemand/apr2012/cal192/input/bvg.run190.25pct.100.filtered.ptLineCounts.txt";
		TabularCount_reader tabCount_reader= new TabularCount_reader();
		try {
			tabCount_reader.readFile(filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//create a map of tabCounts
		TreeMap<Id<TransitLine>, List<TabularCountRecord>> line_CountRecordMap = new TreeMap <>();
		for (TabularCountRecord countRecord : tabCount_reader.getCountRecordMap().values()){
			if (!line_CountRecordMap.keySet().contains(countRecord.getLineId())){
				List<TabularCountRecord> countRecList = new ArrayList<TabularCountRecord>();
				line_CountRecordMap.put(countRecord.getLineId(), countRecList);
			}
			line_CountRecordMap.get(countRecord.getLineId()).add(countRecord);
		}
			
		//create map
		//NewCounts_OldScheduleMatch match = new NewCounts_OldScheduleMatch();
		//System.err.println("Read relation from a test file!!");
		//System.exit(1);
		//match.createMap(); 
	
		String h = "H";
		String r = "R";
		String point = ".";
		Map <String, String> linesMap = new TreeMap <String, String>();
		for(TransitLine line :schedule.getTransitLines().values()){
			Id<TransitLine> lineId = line.getId();
			String newStopId = linesMap.get(lineId.toString()); 
			if (newStopId !=null){
				Id<TransitStopFacility> idnewStopId = Id.create(newStopId, TransitStopFacility.class); 
				List<TabularCountRecord> countList= line_CountRecordMap.get(idnewStopId);

				for (TransitRoute route: line.getRoutes().values()){
					//create list of pseudostops
					List<String> pseudoStopIdList = new ArrayList<String>();
					for(TransitRouteStop stop : route.getStops()){
						String realStopId =  stop.getStopFacility().getId().toString();
						String strPseudoId = Real2PseudoId.convertRealIdtoPseudo(realStopId);
						pseudoStopIdList.add(strPseudoId);
					}
					
					int similar=0;
					if(route.getId().toString().endsWith(h)){ //find routes with H
						for (TabularCountRecord countRecord : countList){
							if (h.charAt(0) ==(countRecord.getDirection())){
								if (pseudoStopIdList.contains(countRecord.getStop())){
									//System.out.println(route.getId());
									similar++;
								} 
							}
						}
					}else if (route.getId().toString().endsWith(r)){ //find routes with R
						for (TabularCountRecord countRecord : countList){
							if (r.charAt(0) ==(countRecord.getDirection())){
								if (pseudoStopIdList.contains(countRecord.getStop())){
									//System.out.println(route.getId());
									similar++;
								} 
							}
						}
					}
					double s= similar;
					double si=route.getStops().size();
					double percentage = (s/si)*100;
					System.out.println(route.getId()  + " " + route.getStops().size() + " " + similar + " %" + percentage);
				}
			}
		}
		
	}
	
}
