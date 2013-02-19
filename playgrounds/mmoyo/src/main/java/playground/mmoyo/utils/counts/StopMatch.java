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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;


public class StopMatch {
	final String point = ".";

	public StopMatch (){
		
	}

	private void run(){
		//read new counts
		String tabCountFilePath= "../../input/newDemand/apr2012/cal192/input/bvg.run190.25pct.100.filtered.ptLineCounts.txt";
		TabularCount_reader tabCount_reader= new TabularCount_reader();
		try {
			tabCount_reader.readFile(tabCountFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
 
		TreeMap<String, List<String>> newRoute_stopsMap = new TreeMap <String, List<String>>();
		for (TabularCountRecord countRecord : tabCount_reader.getCountRecordMap().values()){
			//create a map of tabCounts   (lineId,list<CountRecord>)
			/*
			//this should be outside the for!!!! TreeMap<Id, List<CountRecord>> line_CountRecordMap = new TreeMap <Id, List<TabularCount_reader.CountRecord>>();
			if (!line_CountRecordMap.keySet().contains(countRecord.getLineId())){
				List<TabularCount_reader.CountRecord> countRecList = new ArrayList<TabularCount_reader.CountRecord>();
				line_CountRecordMap.put(countRecord.getLineId(), countRecList);
			}
			line_CountRecordMap.get(countRecord.getLineId()).add(countRecord);
			*/
			
			//create map of tabCounts (routeIdRóH,list<strStops> )
			String strRoute = countRecord.getLineId().toString() + this.point + countRecord.getDirection(); 
			//System.out.println(strRoute);
			
			if (!newRoute_stopsMap.keySet().contains(strRoute)){
				List<String> strStopList = new ArrayList<String>();
				newRoute_stopsMap.put(strRoute, strStopList);
			}
			newRoute_stopsMap.get(strRoute).add(countRecord.getStop());
		}
		
		//print "newRoute" stops
		/*
		for(Entry<String, List<String>> entry: newRoute_stopsMap.entrySet() ){
			String key = entry.getKey(); 
			List<String> value = entry.getValue();
			System.out.println(key + " " + value );
		}*/
		
		//read schedule
		final String SCHEDULEFILE = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(SCHEDULEFILE);
		
		//create map newStop- simTuple
		Map <Id, List<Tuple<String, Double>>> newStop_simTuple_Map = new TreeMap <Id, List<Tuple<String, Double>>>();

		
		String h = "H";
		String r = "R";
		String tab = "\t";
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				
				//first create list of pseudo stops
				List<String> pseudoOldStopIdList = new ArrayList<String>();
				for(TransitRouteStop stop : route.getStops()){
					String realStopId =  stop.getStopFacility().getId().toString();
					//int pseudoLenght = realStopId.length();
					//int pointIndex = realStopId.indexOf(point);
					//if ( pointIndex > -1 ){
					//	pseudoLenght = pointIndex; 
					//}
					//String strPseudoId = realStopId.substring(0, pseudoLenght);
					String strPseudoId = Real2PseudoId.convertRealIdtoPseudo(realStopId);
					pseudoOldStopIdList.add(strPseudoId);
				}
				//System.out.println(route.getId() + " " + pseudoOldStopIdList.toString());
				
				//compare stops from schedule vs counts
				for(Entry <String,List<String>> entry: newRoute_stopsMap.entrySet() ){
					String strNewRouteId = entry.getKey(); 
					List<String> strNewStopsList = entry.getValue();
				
					if ( (route.getId().toString().endsWith(h) && strNewRouteId.endsWith(h)) ||   (route.getId().toString().endsWith(r) && strNewRouteId.endsWith(r))   ){
				
						double similars=0;
						for(String pseudoOldStopId : pseudoOldStopIdList){
							if(strNewStopsList.contains(pseudoOldStopId)){
								similars++;
							}
						}
						double size = route.getStops().size();
						double similarity = similars/ size; 
						BigDecimal bigdec = new BigDecimal(similarity);
						bigdec = bigdec.setScale(2,BigDecimal.ROUND_UP);
						similarity = bigdec.doubleValue(); 
						
						if (similarity > 0.5){
							//System.out.println(route.getId() + arrow + strNewRouteId + arrow + similarity );
							if (!newStop_simTuple_Map.keySet().contains(route.getId())){
								List<Tuple<String, Double>> tupleList = new ArrayList<Tuple<String, Double>>();
								newStop_simTuple_Map.put(route.getId(), tupleList);
							}
							Tuple<String, Double> simTuple = new Tuple<String, Double>(strNewRouteId, similarity);
							newStop_simTuple_Map.get(route.getId()).add(simTuple);
						}
					} 
				}
			}
		}
		
		for(Entry <Id, List<Tuple<String, Double>>> entry: newStop_simTuple_Map.entrySet() ){
			Id key = entry.getKey(); 
			List<Tuple<String, Double>> value = entry.getValue();
			if (value.size()<2){
				//System.out.println(key + tab + value.get(0).getFirst() + tab + value.get(0).getSecond());	 
			}else{
				System.out.println(key);
				for(Tuple<String, Double> tuple : value){
					System.out.println(tab + tuple.getFirst() + tab + tuple.getSecond());	
				}
			}
		}
		
	}
	
	public static void main(String[] args) {
		StopMatch stopMatch = new StopMatch();
		stopMatch.run();
	}

}
