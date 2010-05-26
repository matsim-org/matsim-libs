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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetworkReader;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class DaHafas2VisumMapper2 {
	
	private static final Logger log = Logger
			.getLogger(DaHafas2VisumMapper1.class);
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl visum = new ScenarioImpl();
	private ScenarioImpl hafas = new ScenarioImpl();
	
	final Id CHECK = new IdImpl("CHECK");
	final Id REMOVE = new IdImpl("REMOVE");
	final Id ERROR = new IdImpl("ERROR");

	private SortedMap<Id, Id> visumHafasLineIds;
	private Map<Id, Map<Id, Id>> hafas2VisumMap; 
	private SortedMap<Id, Map<Id, Id>> visumLinesVisum2HafasStops;

	private VisumNetwork vNetwork;

	private String InVisumNetFile;
	
	
	
	public DaHafas2VisumMapper2(){
		visum.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visum);
		hafas.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafas);
		this.createHafasLineIdsFromVisum();
		
	}
	
	public Map<Id, Id> getVisumHafasLineIds(){
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		return visumHafasLineIds;
	}
	
	public Map<Id, Map<Id, Id>> getHafas2VisumMap(){
		this.calcHafas2VisumMap();
		return hafas2VisumMap;
	}
	
	public SortedMap<Id, Map<Id, Id>> getVisum2HafasMap(){
		this.collectAllVisumStops();
		this.calcVisum2HafasStopsByLineLength();
		return visumLinesVisum2HafasStops;
	}
	
	private void calcHafas2VisumMap(){
		hafas2VisumMap = new TreeMap<Id, Map<Id,Id>>();
		Map<Id, Id> tempMap;
		
		this.collectAllVisumStops();
		this.calcVisum2HafasStopsByLineLength();

		for (Entry<Id, Map<Id, Id>> e : visumLinesVisum2HafasStops.entrySet()){
			if(!e.getValue().containsValue(CHECK)){
				tempMap = new HashMap<Id, Id>();
				for (Entry<Id, Id> ee : e.getValue().entrySet()){
//					if (tempMap.containsKey(ee.getValue())){
//						tempMap = null;
//						break;
//					}
					tempMap.put(ee.getValue(), ee.getKey());
				}
				if(!(tempMap ==  null)){
					hafas2VisumMap.put(visumHafasLineIds.get(e.getKey()), tempMap);
				}
			}
		}
		this.addHafasRemove();
		this.removeUnusedLines();
		this.removeBugLines();
	}
	
	/*
	 * should be removed, just for tests
	 */
	
	private void removeBugLines(){
		Id remove;
		
//		remove = new IdImpl("B-320");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
		
//		remove = new IdImpl("B-125");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		remove = new IdImpl("B-131");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		remove = new IdImpl("B-147");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		remove = new IdImpl("B-155");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		remove = new IdImpl("B-158");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		remove = new IdImpl("B-160");
//		hafas2VisumMap.remove(visumHafasLineIds.get(remove));
//		visumHafasLineIds.remove(remove);
//		
//		
	}
	
	private void removeUnusedLines(){
		SortedMap<Id, Id> tempMap = new TreeMap<Id, Id>();
		for(Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
			if(hafas2VisumMap.containsKey(e.getValue())){
				tempMap.put(e.getKey(), e.getValue());
			}
		}
		visumHafasLineIds = tempMap;
	}
	
	private void addHafasRemove(){
		Map<Id, Id> tempMap;
		for (Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
			if(hafas2VisumMap.containsKey(e.getValue())){
				tempMap = hafas2VisumMap.get(e.getValue());
				for(TransitRoute hafasRoute: hafas.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values()){
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
	
	private void calcVisum2HafasStopsByLineLength(){
		
		for (Entry<Id, Id> lines: visumHafasLineIds.entrySet()){
			for (TransitRoute hafasRoute : hafas.getTransitSchedule().getTransitLines().get(lines.getValue()).getRoutes().values()){
				for(TransitRoute visumRoute : visum.getTransitSchedule().getTransitLines().get(lines.getKey()).getRoutes().values()){
					if(visumRoute.getStops().size() == hafasRoute.getStops().size()){
						for (int ii = 0; ii < visumRoute.getStops().size(); ii++){
							if(this.checkFacsById(visumRoute.getStops().get(ii).getStopFacility().getId(), hafasRoute.getStops().get(ii).getStopFacility().getId()) == true){
								visumLinesVisum2HafasStops.get(lines.getKey()).put(visumRoute.getStops().get(ii).getStopFacility().getId(), 
										hafasRoute.getStops().get(ii).getStopFacility().getId());
							}else{
								visumLinesVisum2HafasStops.get(lines.getKey()).put(visumRoute.getStops().get(ii).getStopFacility().getId(), CHECK);
							}
						}
					}
					
				}
			}
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
	
	private void readVisumNetwork()  {
		vNetwork = new VisumNetwork();
		log.info("reading visum network.");
		try {
			new VisumNetworkReader(vNetwork).read(InVisumNetFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void createHafasLineIdsFromVisum(){
		visumHafasLineIds = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(org.matsim.transitSchedule.api.TransitLine line : visum.getTransitSchedule().getTransitLines().values()){
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
			if(createdHafasId.length()>0 && hafas.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				visumHafasLineIds.put(line.getId() , new IdImpl(hafasId));
			}else if (createdHafasId.length()>0){
				visumHafasLineIds.put(line.getId(), CHECK);
			}
		}
	}
	
	
	private void collectAllVisumStops(){
		visumLinesVisum2HafasStops = new TreeMap<Id, Map<Id, Id>>();
		for(Entry<Id, TransitLine> e: visum.getTransitSchedule().getTransitLines().entrySet()){
			visumLinesVisum2HafasStops.put(e.getKey(), this.collectLineStops(e.getValue()));
		}
	}
	
	private HashMap<Id, Id> collectLineStops(TransitLine visumLine){
		HashMap<Id, Id> temp = new HashMap<Id, Id>();
		for(TransitRoute route : visumLine.getRoutes().values()){
			for (TransitRouteStop stop : route.getStops()){
				temp.put(stop.getStopFacility().getId(), CHECK);
			}
		}
		return temp;
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
	
	public static void main(String[] args){
		DaHafas2VisumMapper2 mapper = new DaHafas2VisumMapper2();
		
		for (Entry<Id, Map<Id, Id>> stops : mapper.getHafas2VisumMap().entrySet()){
//			if (stops.getKey().equals(new IdImpl("112  "))){
				System.out.println("hafasLineId: " + stops.getKey().toString()  + " size" + stops.getValue().size());
				
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
//			}
		}
		
//		for (Entry<Id, Map<Id, Id>> stops : mapper.getVisum2HafasMap().entrySet()){
////			if(stops.getKey().equals(new IdImpl("B-112"))){
//				System.out.println("visumLineId: " + stops.getKey().toString() + " size" + stops.getValue().size());
//				
//				System.out.print("visumStopId" + "\t");
//				for(Entry<Id, Id> e : stops.getValue().entrySet()){
//					System.out.print(e.getKey().toString() + "\t");
//				}
//				System.out.println("");
//				System.out.print("hafasStopId" + "\t");
//				for(Entry<Id, Id> e : stops.getValue().entrySet()){
//					System.out.print(e.getValue().toString() + "\t");
//				}
//				System.out.println("");
//				System.out.println("");
////			}
//		}
		
	}
}
