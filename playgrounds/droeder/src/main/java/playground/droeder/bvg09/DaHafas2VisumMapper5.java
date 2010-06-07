/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.bvg09;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class DaHafas2VisumMapper5 {
	
	private static final Logger log = Logger
			.getLogger(DaHafas2VisumMapper5.class);
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl visumSc = new ScenarioImpl();
	private ScenarioImpl hafasSc = new ScenarioImpl();
	
	final Id CHECK = new IdImpl("CHECK");
	final Id REMOVE = new IdImpl("REMOVE");
	final Id ERROR = new IdImpl("ERROR");

	private SortedMap<Id, Id> visumHafasLineIds = null;
	private SortedMap<Id, Id> visumHafasUnmatchedLineIds = null;
	private Map<Id, Map<Id, Id>> hafas2VisumMap = null; 
	private Map<Id, Map<Id, Id>> hafas2VisumNeedToCheck = null;
	
	public DaHafas2VisumMapper5(){
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
	}
	
	public Map<Id, Id> getVisumHafasLineIds(){
		return visumHafasLineIds;
	}
	
	public Map<Id, Map<Id, Id>> getHafas2VisumMap(){
		if (this.hafas2VisumMap == null) this.calcHafas2VisumMap();
		return hafas2VisumMap;
	}
	
	public Map<Id, Id> getUnmatched(){
		if(visumHafasUnmatchedLineIds == null) this.calcHafas2VisumMap();
		return visumHafasUnmatchedLineIds;
	}
	
	private void calcHafas2VisumMap() {
		this.hafas2VisumMap = new TreeMap<Id, Map<Id,Id>>();
		
		for (Entry<Id, Id> visumHafasLines : visumHafasLineIds.entrySet()){
			Map<Id, Id> temp = this.compareLines(visumHafasLines.getKey(), visumHafasLines.getValue());
			if (!(temp ==  null) && temp.size()>1 && this.allVisumStopsContained(visumHafasLines.getKey(), temp) == true){
				this.hafas2VisumMap.put(visumHafasLines.getValue(), temp);
			}else{
				if (this.visumHafasUnmatchedLineIds == null){
					this.visumHafasUnmatchedLineIds = new TreeMap<Id, Id>();
				}
				this.visumHafasUnmatchedLineIds.put(visumHafasLines.getKey(), visumHafasLines.getValue());
			}
		}
		
		if(!(this.visumHafasUnmatchedLineIds ==  null)){
			this.removeUnmatchedLines();
		}
		this.addHafasRemove();
		this.removeBugLines();
		
	}
	
	private boolean allVisumStopsContained(Id visumLine, Map<Id, Id> hafas2Visum){
		
		for (TransitRoute route : visumSc.getTransitSchedule().getTransitLines().get(visumLine).getRoutes().values()){
			for(TransitRouteStop stop : route.getStops()){
				TransitStopFacility fac = stop.getStopFacility();
				if(!hafas2Visum.containsValue(fac.getId())){
					return false;
				}
			}
		}
		
		
		return true;
	}
	
	private void removeUnmatchedLines(){
		for(Entry<Id, Id> e : this.visumHafasUnmatchedLineIds.entrySet()){
			visumHafasLineIds.remove(e.getKey());
			hafas2VisumMap.remove(e.getValue());
//			log.error("removed " + e.toString());
		}
	}
	private void removeBugLines(){
		Id remove;
		
		remove = new IdImpl("B-135");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-195");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-222");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-320");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-395");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
	}
	
	private void addHafasRemove(){
		Map<Id, Id> tempMap;
		for (Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
			if(hafas2VisumMap.containsKey(e.getValue())){
				tempMap = hafas2VisumMap.get(e.getValue());
				for(TransitRoute hafasRoute: hafasSc.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values()){
					for (TransitRouteStop stop : hafasRoute.getStops()){
						if(!(tempMap.containsKey(stop.getStopFacility().getId()))){
							tempMap.put(stop.getStopFacility().getId(), REMOVE);
						}
					}
				}
				hafas2VisumMap.put(e.getValue(), tempMap);
			}
		}
	}

	private Map<Id, Id> compareLines(Id visum, Id hafas) {
		Map<Id, Id> hafasVisumStops = new HashMap<Id, Id>();
		
		if(!(hafasSc.getTransitSchedule().getTransitLines().containsKey(hafas))){
			return null;
		}
		
		for(TransitRoute hafasRoute : hafasSc.getTransitSchedule().getTransitLines().get(hafas).getRoutes().values()){
			for (TransitRoute visumRoute : visumSc.getTransitSchedule().getTransitLines().get(visum).getRoutes().values()){
				Map<Id, Id> temp = this.compareHafas2VisumRoute(visumRoute, hafasRoute, hafasVisumStops, hafas);
				if (!(temp == null)){
					hafasVisumStops = temp;
				}
			}
		}
		return hafasVisumStops;
	}
	

	private Map<Id, Id> compareHafas2VisumRoute(TransitRoute visumRoute, TransitRoute hafasRoute, Map<Id, Id> hafasVisum, Id hafasLine) {
		Map<Id, Id> temp = new HashMap<Id, Id>();
		
 		ListIterator<TransitRouteStop> visumIterator = visumRoute.getStops().listIterator();
 		ListIterator<TransitRouteStop>  hafasIterator = hafasRoute.getStops().listIterator();
 		
 		TransitStopFacility visumStop;
 		TransitStopFacility hafasStop = null;
 		
 		if (visumIterator.hasNext()){
 			visumStop = visumIterator.next().getStopFacility();
 		}else{
 			return null;
 		}
 		
 		
 		do{
 			if (hafasIterator.hasNext()){
 				hafasStop = hafasIterator.next().getStopFacility();
 			}else if (visumIterator.hasNext()){
 				hafasIterator = hafasRoute.getStops().listIterator(0);
 				visumStop = visumIterator.next().getStopFacility();
 			}else{
 				return null;
 			}
 			
 			
 		}while(!this.checkFacsById(visumStop.getId(), hafasStop.getId()));
 		
 		while (visumIterator.hasPrevious()){
 			visumStop = visumIterator.previous().getStopFacility();
 			if (hafasIterator.hasPrevious()){
 				hafasStop = hafasIterator.previous().getStopFacility();
 			}else{
 				return null;
 			}
 		}
 		temp.put(hafasStop.getId(), visumStop.getId());
 		
 		while(visumIterator.hasNext()){
 			if(hafasIterator.hasNext()){
 				hafasStop = hafasIterator.next().getStopFacility();
 				visumStop = visumIterator.next().getStopFacility();
 				if(this.checkFacsById(visumStop.getId(), hafasStop.getId())){
 					temp.put(hafasStop.getId(), visumStop.getId());
 				}else if(!hafasVisum.containsKey(hafasStop.getId())){
 					temp.put(hafasStop.getId(), visumStop.getId());
 				}
 			}
 			else{
 				return null;
 			}
 		}
 		
 		if(doubleMatching(temp, hafasVisum, hafasLine) == false){
 			temp.putAll(hafasVisum);
 			return temp;
 		}else{
 			return null;
 		}
		
	}
	
	
	private boolean doubleMatching(Map<Id, Id> temp, Map<Id, Id> all, Id hafasLine){
		
		for(Entry<Id, Id> entry : temp.entrySet()){
			if(all.containsKey(entry.getKey())&& !(all.get(entry.getKey()).equals(entry.getValue()))){
//				log.error("hafas line "+ hafasLine.toString() + " hafas stop " + entry.getKey() + " matched to visum " + entry.getValue()  + " and " + all.get(entry.getKey()));
				return true;
			}
		}
		
		return false;
	}
	

	private void readSchedule(String fileName, ScenarioImpl sc){
		TransitScheduleReader reader = new TransitScheduleReader(sc);
		try {
			reader.readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void getHafasStopsForVisum(){
		
		
		for (TransitLine visumLine : visumSc.getTransitSchedule().getTransitLines().values()){
			Map<Id, List<Id>> temp = new HashMap<Id, List<Id>>();
			for(TransitRoute visumRoute : visumLine.getRoutes().values()){
				for(TransitRouteStop visumStop : visumRoute.getStops()){
					List<Id> stops = new ArrayList<Id>();
					if(visumHafasLineIds.containsKey(visumLine.getId())){
						TransitLine hafasLine = hafasSc.getTransitSchedule().getTransitLines().get(visumHafasLineIds.get(visumLine.getId()));
						for(TransitRoute hafasRoute : hafasLine.getRoutes().values()){
							for(TransitRouteStop hafasStop : hafasRoute.getStops()){
								if(checkFacsById(visumStop.getStopFacility().getId(), hafasStop.getStopFacility().getId()) && !stops.contains(hafasStop.getStopFacility().getId())){
									stops.add(hafasStop.getStopFacility().getId());
								}
							}
						}
					}
					if(stops.size() > 1 ){
						temp.put(visumStop.getStopFacility().getId(), stops);
					}
				}
			}
			if (temp.size()>0){
				System.out.println("visumLine " + visumLine.getId());
				for(Entry<Id, List<Id>> lineStops : temp.entrySet()){
					System.out.println("visumStop " + lineStops.getKey().toString());
					for(Id id : lineStops.getValue()){
						System.out.print(id.toString() + "\t");
					}
					System.out.println();
				}
				System.out.println("#######################");
				
			}
		}
		
		
	}
	
	
	private void createHafasLineIdsFromVisum(){
		visumHafasLineIds = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(org.matsim.transitSchedule.api.TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
			createdHafasId = new StringBuffer();
			idToChar = line.getId().toString().split("");
			
			if(idToChar[1].equals("B")){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("U")){
				createdHafasId.append(idToChar[1]);
				createdHafasId.append(idToChar[3]);
				createdHafasId.append("   ");
			}else if(idToChar[1].equals("T") && idToChar[3].equals("M") ){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("T") && !(idToChar.equals("M")) ){
				createdHafasId.append(idToChar[3]);
				createdHafasId.append(idToChar[4]);
				createdHafasId.append("   ");
			}
			
			hafasId = createdHafasId.toString();
			if(createdHafasId.length()>0 && hafasSc.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				visumHafasLineIds.put(line.getId() , new IdImpl(hafasId));
			}else if (createdHafasId.length()>0){
				visumHafasLineIds.put(line.getId(), CHECK);
			}
		}
	}
	private double getDistance(Coord visum, Coord hafas){
		double xDif = visum.getX() - hafas.getX();
		double yDif = visum.getY() - hafas.getY();
		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
	}
	
	
	public void calcAverageDistance(){
		if (hafas2VisumMap ==  null) this.calcHafas2VisumMap();

		
		for (Entry<Id, Map<Id, Id>> e : hafas2VisumMap.entrySet()){
			double avDist = 0;
			int count = 0;
			double max = 0;
			double min = Double.MAX_VALUE;
			TransitStopFacility hafasStop;
			TransitStopFacility visumStop;
			for (Entry<Id, Id> ee : e.getValue().entrySet()){
				hafasStop = hafasSc.getTransitSchedule().getFacilities().get(ee.getKey());
				visumStop = visumSc.getTransitSchedule().getFacilities().get(ee.getValue());
				if(visumStop == null && hafasStop == null){
					log.warn("no visumstop and no hafasstop found for line " + e.getKey());
				}else if(visumStop == null){
					log.warn("no visumStop found for Hafasstop " + hafasStop.getId() + " on line " + e.getKey());
				}else if (hafasStop == null ){
					log.warn("no hafasStop found for visumstop " + visumStop.getId() + " on line " + e.getKey());
				}else{
					double dist = this.getDistance(visumStop.getCoord(), hafasStop.getCoord());
					avDist += dist;
					if(dist > max){
						max = dist;
					}
					if(dist < min){
						min = dist;
					}
					count++;
				}
				
			}
			
			avDist = avDist/count;
			System.out.println("for line " + e.getKey() + ": av=" + avDist + " min=" + min + " max=" + max);
		}
		
	}
	
	private boolean checkFacsById(Id vis, Id haf){
		boolean equal = false;
		
		String hafas = null;
		String visum =  vis.toString();
		if (visum.length() == 6){
			hafas = haf.toString().substring(2, haf.toString().length());
			visum = visum.substring(0, visum.length()-1);
		}else if(visum.length() == 5){
			hafas = haf.toString().substring(3, haf.toString().length());
			visum = visum.substring(0, visum.length()-1);
		}else if(visum.length() == 7){
			hafas = haf.toString().substring(1, haf.toString().length());
			visum = visum.substring(0, visum.length()-1);
		}
		
		if(visum.equals(hafas)){
			equal = true;
		}
		
		return equal;
	}
	
	public void test(){
		System.out.print(this.checkFacsById(new IdImpl("792040"), new IdImpl("9079204")));
		System.out.print(this.checkFacsById(new IdImpl("92025"), new IdImpl("9009202")));
		System.out.print(this.checkFacsById(new IdImpl("1000230"), new IdImpl("9100023")));
		System.out.print(this.checkFacsById(new IdImpl("611021"), new IdImpl("9061102")));
	}
	
	public static void main(String[] args){
		DaHafas2VisumMapper5 mapper = new DaHafas2VisumMapper5();
		mapper.calcAverageDistance();
		
//		for (Entry<Id, Map<Id, Id>> stops : mapper.getHafas2VisumMap().entrySet()){
//			System.out.println("hafasLineId: " + stops.getKey().toString());
//			
//			System.out.print("hafasStopId" + "\t");
//			for(Entry<Id, Id> e : stops.getValue().entrySet()){
//				System.out.print(e.getKey().toString() + "\t");
//			}
//			System.out.println("");
//			System.out.print("visumStopId" + "\t");
//			for(Entry<Id, Id> e : stops.getValue().entrySet()){
//				System.out.print(e.getValue().toString() + "\t");
//			}
//			System.out.println("");
//			System.out.println("");
//		}
		
//		for(Entry<Id, Id> e : mapper.getUnmatched().entrySet()){
//			System.out.println(e.toString());
//		}
		
		
//		mapper.getHafas2VisumMap();
//		for(Entry<Id, Id> e : mapper.getVisumHafasLineIds().entrySet()){
//			System.out.println(e.toString());
//		}
//		
//		System.out.println("##########");
//		System.out.println("unmatched");
//		
//		for(Entry<Id, Id> e : mapper.getUnmatched().entrySet()){
//			System.out.println(e.toString());
//		}
		
		
	}
}
