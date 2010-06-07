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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
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
public class DaHafas2VisumMapper7 {
	
	private static final Logger log = Logger
			.getLogger(DaHafas2VisumMapper6.class);
	
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
	private Map<Id, Map<Id, Id>> hafas2VisumMapUnmatched = null; 
	private Map<Id, Set<Id>> missedVisumStops = new TreeMap<Id, Set<Id>>();
	
	public DaHafas2VisumMapper7(){
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
	}
	
	public static void main(String[] args){
		DaHafas2VisumMapper7 mapper = new DaHafas2VisumMapper7();
		mapper.run();
	
	}
	
	public void run(){
		this.calcHafas2VisumMap();
		for(Entry<Id, Map<Id, Id>> e:  hafas2VisumMapUnmatched.entrySet()){
			System.out.println("line " + e.getKey());
			System.out.print("hafas \t");
			for(Entry<Id, Id> ee : e.getValue().entrySet()){
				System.out.print(ee.getKey() + "\t");
			}
			System.out.println();
			System.out.print("visum\t");
			for(Entry<Id, Id> ee : e.getValue().entrySet()){
				System.out.print(ee.getValue() + "\t");
			}
			System.out.println("");
			System.out.println("#####");
		}
		System.out.println("unmatched: " + hafas2VisumMapUnmatched.size());
		
		for(Entry<Id, Set<Id>> e : missedVisumStops.entrySet()){
			System.out.print(e.getKey() + "\t");
			for(Id id : e.getValue()){
				System.out.print(id + "\t");
			}
			System.out.println("");
			System.out.println("");
		}
	}
	
	public Map<Id, Id> getVisumHafasLineIds(){
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		return visumHafasLineIds;
	}
	
	public Map<Id, Id> getMatchedVisumHafasLineIds(){
		this.calcHafas2VisumMap();
		return visumHafasLineIds;
	}
	public Map<Id, Map<Id, Id>> getHafas2VisumMap(){
		if (this.hafas2VisumMap == null) this.calcHafas2VisumMap();
		return hafas2VisumMap;
	}
	
	private void calcHafas2VisumMap() {
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		this.hafas2VisumMap = new TreeMap<Id, Map<Id,Id>>();
		
		for (Entry<Id, Id> visumHafasLines : visumHafasLineIds.entrySet()){
			Map<Id, Id> temp = this.compareLines(visumHafasLines.getKey(), visumHafasLines.getValue());
			if (!(temp.equals(null)) && (temp.size()>1) && this.allVisumStopsContained(visumHafasLines.getKey(), temp)){
				this.hafas2VisumMap.put(visumHafasLines.getValue(), temp);
			}else if(temp.equals(null)){
				log.error("no matching found for " + visumHafasLines.getKey());
				this.removeToCheck(visumHafasLines.getKey(), visumHafasLines.getValue(), temp);
			}
			else if(!this.allVisumStopsContained(visumHafasLines.getKey(), temp)){
				log.error("not all VisumStops contained " + visumHafasLines.getKey());
				this.removeToCheck(visumHafasLines.getKey(), visumHafasLines.getValue(), temp);
			}
			else if(temp.size()<2){
				log.error("a line must have more than one stop " + visumHafasLines.getKey());
				this.removeToCheck(visumHafasLines.getKey(), visumHafasLines.getValue(), temp);
			}
		}
		
		if(!(this.visumHafasUnmatchedLineIds ==  null)){
			this.removeUnmatchedLines();
		}
		this.removeBugLines();
		this.addHafasRemove();
		
	}
	
	private void removeToCheck(Id visum, Id hafas, Map<Id, Id> hafas2Visum){
		if (this.visumHafasUnmatchedLineIds == null){
			this.visumHafasUnmatchedLineIds = new TreeMap<Id, Id>();
		}
		if(this.hafas2VisumMapUnmatched == null){
			this.hafas2VisumMapUnmatched = new TreeMap<Id, Map<Id,Id>>();
		}
		this.visumHafasUnmatchedLineIds.put(visum, hafas);
		if(!(hafas2Visum == null)){
			this.hafas2VisumMapUnmatched.put(hafas, hafas2Visum);
		}
	}
	
	private boolean allVisumStopsContained(Id visumLine, Map<Id, Id> hafas2Visum){
		boolean all = true;
		Set<Id> missed = null;
		
		
		for (TransitRoute route : visumSc.getTransitSchedule().getTransitLines().get(visumLine).getRoutes().values()){
			for(TransitRouteStop stop : route.getStops()){
				TransitStopFacility fac = stop.getStopFacility();
				if(!hafas2Visum.containsValue(fac.getId())){
					if (missed == null) missed = new TreeSet<Id>();
					missed.add(fac.getId());
					all = false;
				}
			}
		}
		
		if(!(missed == null)){
			this.missedVisumStops.put(visumLine, missed);
		}
		
		return all;
	}
	
	private void removeUnmatchedLines(){
		for(Entry<Id, Id> e : this.visumHafasUnmatchedLineIds.entrySet()){
			visumHafasLineIds.remove(e.getKey());
		}
	}
	
	private void removeBugLines(){
		Id remove;
		
		remove = new IdImpl("B-101");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
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
		
		remove = new IdImpl("B-X11");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-X76");
		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("U-1");
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
		Map<Id, Id> hafasVisumStops = new LinkedHashMap<Id, Id>();
		
		if(!(hafasSc.getTransitSchedule().getTransitLines().containsKey(hafas))){
			return null;
		}
		if(!(visumSc.getTransitSchedule().getTransitLines().containsKey(visum))){
			return null;
		}
		
		for(TransitRoute hafasRoute : hafasSc.getTransitSchedule().getTransitLines().get(hafas).getRoutes().values()){
			
			for (TransitRoute visumRoute : visumSc.getTransitSchedule().getTransitLines().get(visum).getRoutes().values()){
				Map<Id, Id> temp = this.compareHafas2VisumRouteByDist(visumRoute, hafasRoute, hafasVisumStops);
				if (!(temp == null)){
					hafasVisumStops = temp;
				}
			}
		}
		return hafasVisumStops;
	}
	
	private boolean matchingPossible(TransitRoute visumRoute, TransitRoute hafasRoute){
		double first = this.getDistance(visumRoute.getStops().get(0).getStopFacility().getCoord(), 
				hafasRoute.getStops().get(0).getStopFacility().getCoord());
		double last = this.getDistance(visumRoute.getStops().get(0).getStopFacility().getCoord(), 
				hafasRoute.getStops().get(hafasRoute.getStops().size()-1).getStopFacility().getCoord());
		
		if((first<last) && (visumRoute.getStops().size()<= hafasRoute.getStops().size())){
			return true;
		}else{
			return false;
		}
	}

	private Map<Id, Id> compareHafas2VisumRouteByDist(TransitRoute visumRoute, TransitRoute hafasRoute, Map<Id, Id> hafasVisum) {
		if(!this.matchingPossible(visumRoute, hafasRoute)) return null;

		Map<Id, Id> temp = new HashMap<Id, Id>();
		
		ListIterator<TransitRouteStop> hafasIterator = hafasRoute.getStops().listIterator(this.getBestPossibleByDist(visumRoute, hafasRoute));
		ListIterator<TransitRouteStop> visumIterator = visumRoute.getStops().listIterator();
		TransitRouteStop hafasStop;
		TransitRouteStop visumStop;
		
		
		while(hafasIterator.hasNext() && visumIterator.hasNext()){
			hafasStop = hafasIterator.next();
			visumStop = visumIterator.next();
			
			temp.put(hafasStop.getStopFacility().getId(), visumStop.getStopFacility().getId());
			
		}
		temp = compareHafas2VisumRouteById(visumRoute, hafasRoute, temp);
		
		if(visumIterator.hasNext()){
			return null;
		}else{
			temp.putAll(hafasVisum);
			return temp;
		}
	}
	
	private Map<Id, Id> compareHafas2VisumRouteById(TransitRoute visumRoute, TransitRoute hafasRoute, Map<Id, Id> hafasVisum){
		Map<Id, Id> temp = hafasVisum;
		
		for(TransitRouteStop vStop : visumRoute.getStops()){
			if(!temp.containsValue(vStop.getStopFacility().getId())){
				for(TransitRouteStop hStop : hafasRoute.getStops()){
					if(this.checkFacsById(vStop.getStopFacility().getId(), hStop.getStopFacility().getId()) && !(temp.containsKey(hStop.getStopFacility().getId()))){
						temp.put(hStop.getStopFacility().getId(), vStop.getStopFacility().getId());
					}else if(this.checkFacsById(vStop.getStopFacility().getId(), hStop.getStopFacility().getId()) && temp.containsKey(hStop.getStopFacility().getId())){
						temp.put(new IdImpl(hStop.getStopFacility().getId().toString() + "_double"), vStop.getStopFacility().getId());
					}
				}
			}
		}
		
		
		
		return temp;
	}
	
	
	private int getBestPossibleByDist(TransitRoute visumRoute, TransitRoute hafasRoute){
		double avDist = Double.MAX_VALUE;
		int bestIteratorPosition = 0;
		
		List<TransitRouteStop> visumStops = visumRoute.getStops();
		List<TransitRouteStop> hafasStops = hafasRoute.getStops();
		
		ListIterator<TransitRouteStop> hafasIterator;
		TransitRouteStop hafasStop = null;
		TransitRouteStop visumStop;
		int hafasOffset = 0;
		
		
		while((visumStops.size()) <= (hafasStops.size() -hafasOffset)){
			hafasIterator = hafasStops.listIterator(hafasOffset);
			if (hafasIterator.hasNext()){
				hafasStop = hafasIterator.next();
			}
			
			double average = 0;
			for (ListIterator<TransitRouteStop> visumIterator = visumStops.listIterator(); visumIterator.hasNext();){
				visumStop = visumIterator.next();
				average += this.getDistance(visumStop.getStopFacility().getCoord(), hafasStop.getStopFacility().getCoord());
			}
			if(avDist >= (average / visumStops.size())){
				bestIteratorPosition = hafasOffset;
				avDist = average/visumStops.size();
			}
			hafasOffset++;
		}
		
		return bestIteratorPosition;
	}
	
	private double getDistance(Coord visum, Coord hafas){
		double xDif = visum.getX() - hafas.getX();
		double yDif = visum.getY() - hafas.getY();
		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
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
	
	public void calcAverageDifference(){
		if (hafas2VisumMap ==  null) this.calcHafas2VisumMap();

		double max = 0;
		double min = Double.MAX_VALUE;
		
		for (Entry<Id, Map<Id, Id>> e : hafas2VisumMap.entrySet()){
			double avDist = 0;
			int count = 0;
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
					avDist += this.getDistance(visumStop.getCoord(), hafasStop.getCoord());
					count++;
				}
				
				
			}
			avDist = avDist/count;
			if(avDist > max){
				max = avDist;
			}
			if(avDist < min){
				min = avDist;
			}
			System.out.println("average distance for line " + e.getKey() + ": " + avDist);
		}
		
		System.out.println("max av. dist = " + max + "; min av. dist = " + min);
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
	
	
}
