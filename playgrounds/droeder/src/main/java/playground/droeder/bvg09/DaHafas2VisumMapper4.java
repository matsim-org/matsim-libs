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
import java.util.ListIterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
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
public class DaHafas2VisumMapper4 {
	
	private static final Logger log = Logger
			.getLogger(DaHafas2VisumMapper1.class);
	
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
	
	public DaHafas2VisumMapper4(){
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
	}
	
	public Map<Id, Id> getVisumHafasLineIds(){
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		return visumHafasLineIds;
	}
	
	public Map<Id, Map<Id, Id>> getHafas2VisumMap(){
		if (this.hafas2VisumMap == null) this.calcHafas2VisumMap();
		return hafas2VisumMap;
	}
	
	private void calcHafas2VisumMap() {
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		this.hafas2VisumMap = new TreeMap<Id, Map<Id,Id>>();
		
		for(Entry<Id, Id> e : visumHafasLineIds.entrySet()){
			hafas2VisumMap.put(e.getValue(), this.matchLines(e.getKey(), e.getValue()));
		}
//		this.addHafasRemove();
		
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

	private Map<Id, Id> matchLines(Id visum, Id hafas) {
		Map<Id, Id> hafasVisumStops = new LinkedHashMap<Id, Id>();
		
		if(!(hafasSc.getTransitSchedule().getTransitLines().containsKey(hafas))){
			return null;
		}
		
		for (TransitRoute visumRoute : visumSc.getTransitSchedule().getTransitLines().get(visum).getRoutes().values()){
			Map<Id, Id> temp = this.matchHafasLine2VisumRoute(visumRoute, hafasSc.getTransitSchedule().getTransitLines().get(hafas));
			if (!(temp == null)){
				hafasVisumStops.putAll(temp);
			}
		}
		return hafasVisumStops;
	}
	

	private Map<Id, Id> matchHafasLine2VisumRoute(TransitRoute visumRoute, TransitLine hafasLine) {
		Map<Id, Id> matched = null;
		
		for(TransitRoute hafasRoute :  hafasLine.getRoutes().values()){
			Map<Id, Id> temp = this.matchRoutes(visumRoute, hafasRoute);
			if(!(temp == null)){
				for(Entry<Id, Id> e : temp.entrySet()){
					if (matched.containsKey(e.getKey())){
						if(!(matched.get(e.getKey()) == e.getValue())){
							log.error("hafasStop " + e.getKey().toString() + " is already matched to visumStop " +matched.get(e.getKey()) 
									+ ". visumStop " + e.getValue() + "not matched");
						}
					}else{
						if(matched == null) matched = new HashMap<Id, Id>();
						matched.put(e.getKey(), e.getValue());
					}
				}
			}
		}
		return matched;
	}
	
	private Map<Id, Id> matchRoutes(TransitRoute visumRoute, TransitRoute hafasRoute){
		ListIterator<TransitRouteStop> hafasIterator = hafasRoute.getStops().listIterator();
		ListIterator<TransitRouteStop> visumIterator = visumRoute.getStops().listIterator();
		Map<Id, Id> matched = new HashMap<Id, Id>();
		boolean firstStop = true;
		
		
		TransitStopFacility visumStop;
		TransitStopFacility hafasStop;
		
		
		if (visumIterator.hasNext()){
			visumIterator.next();
		}else{
			return null;
		}
		
		
		do{
			if(firstStop == false){
				visumIterator.next();
			}
			visumStop = visumRoute.getStops().get(visumIterator.previousIndex()).getStopFacility();
			boolean matchedStop = false;
			do{
				if(hafasIterator.hasNext()){
					hafasIterator.next();
				}
				hafasStop = hafasRoute.getStops().get(hafasIterator.previousIndex()).getStopFacility();
				if(checkFacsById(visumStop.getId(), hafasStop.getId())){
					matched.put(hafasStop.getId(), visumStop.getId());
				}
			}while(hafasIterator.hasNext() && matchedStop == false  );
			
			
			firstStop = false;
		}while (visumIterator.hasNext() && hafasIterator.hasNext());
		
		
		if(! (visumRoute.getStops().size() == matched.size()) ){
			return null;
		}else{
			return matched;
		}
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
	
	private boolean checkFacsById(Id vis, Id haf){
		boolean equal = false;
		
		String hafas = null;
		String visum =  vis.toString();
		if (visum.length() == 6){
			hafas = haf.toString().substring(2, haf.toString().length()-1);
		}else if(visum.length() == 5){
			hafas = haf.toString().substring(3, haf.toString().length()-1);
		}else if(visum.length() == 7){
			hafas = haf.toString().substring(1, haf.toString().length()-1);
		}
		visum = visum.substring(0, visum.length()-2);
		if(visum.equals(hafas)){
			equal = true;
		}
		
		return equal;
	}
	
	
	private Map<Id, Id> prematchLines(Id visumLine, Id hafasLine) {
		Map<Id, Id> hafasVisumStops = new LinkedHashMap<Id, Id>();
		
		if(!(hafasSc.getTransitSchedule().getTransitLines().containsKey(hafasLine))){
			return null;
		}
		
		for(TransitRoute hafasRoute : hafasSc.getTransitSchedule().getTransitLines().get(hafasLine).getRoutes().values()){
			for (TransitRoute visumRoute : visumSc.getTransitSchedule().getTransitLines().get(visumLine).getRoutes().values()){
				Map<Id, Id> temp = this.compareHafas2VisumRoute(visumRoute, hafasRoute, hafasVisumStops);
				if (!(temp == null)){
					hafasVisumStops = temp;
				}
			}
		}
		return hafasVisumStops;
	}
	

	private Map<Id, Id> compareHafas2VisumRoute(TransitRoute visumRoute, TransitRoute hafasRoute, Map<Id, Id> hafasVisum) {
		Map<Id, Id> temp = hafasVisum;
		
		if(visumRoute.getStops().size() == hafasRoute.getStops().size()){
			for (int ii = 0; ii < visumRoute.getStops().size(); ii++){
				Id visumStop = visumRoute.getStops().get(ii).getStopFacility().getId();
				Id hafasStop = hafasRoute.getStops().get(ii).getStopFacility().getId();
				if(this.checkFacsById(visumStop, hafasStop) == true){
					if(temp.containsKey(hafasStop) && !temp.get(hafasStop).equals(visumStop)){
						log.error("hafasStop " + hafasStop.toString() + " already matched to visumStop " + temp.get(hafasStop).toString() + "! can't match visumStop " + visumStop.toString());
					}else{
						temp.put(hafasStop, visumStop);
					}
				}
			}
			return temp;
		}else{
			return null;
		}
	}
	
	
	public static void main(String[] args){
		DaHafas2VisumMapper4 mapper = new DaHafas2VisumMapper4();
		
		for (Entry<Id, Map<Id, Id>> stops : mapper.getHafas2VisumMap().entrySet()){
			System.out.println("hafasLineId: " + stops.getKey().toString());
			
			System.out.print("hafasStopId" + "\t");
			for(Entry<Id, Id> e : stops.getValue().entrySet()){
				System.out.print(e.getKey().toString() + "\t");
			}
			System.out.println("");
			System.out.print("visumStopId" + "\t");
			for(Entry<Id, Id> e : stops.getValue().entrySet()){
				System.out.print(e.getValue().toString() + "\t");
			}
			System.out.println("");
			System.out.println("");
		}
		
		
	}
}
