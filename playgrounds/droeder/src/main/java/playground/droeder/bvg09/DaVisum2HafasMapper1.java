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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class DaVisum2HafasMapper1 {
	
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
	private Map<Id, Map<Id, Id>> visum2HafasMap = null; 
	
	public DaVisum2HafasMapper1(){
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
	
	public Map<Id, Map<Id, Id>> getVisum2HafasMap(){
		if (this.visum2HafasMap == null) this.calcVisum2HafasMap();
		return visum2HafasMap;
	}
	
	private void calcVisum2HafasMap() {
		if (visumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		this.visum2HafasMap = new TreeMap<Id, Map<Id,Id>>();
		
		for (Entry<Id, Id> visumHafasLines : visumHafasLineIds.entrySet()){
			Map<Id, Id> temp = this.compareLines(visumHafasLines.getKey(), visumHafasLines.getValue());
			if (!temp.equals(null) && temp.size()>1 ){
				this.visum2HafasMap.put(visumHafasLines.getKey(), temp);
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
		this.removeBugLines();
//		this.addHafasRemove();
		
	}
	
//	private boolean allVisumStopsContained(Id visumLine, Map<Id, Id> hafas2Visum){
//		
//		for (TransitRoute route : visumSc.getTransitSchedule().getTransitLines().get(visumLine).getRoutes().values()){
//			for(TransitRouteStop stop : route.getStops()){
//				TransitStopFacility fac = stop.getStopFacility();
//				if(!hafas2Visum.containsValue(fac.getId())){
//					return false;
//				}
//			}
//		}
//		
//		
//		return true;
//	}
	
	private void removeUnmatchedLines(){
		for(Entry<Id, Id> e : this.visumHafasUnmatchedLineIds.entrySet()){
			visumHafasLineIds.remove(e.getKey());
		}
	}
	private void removeBugLines(){
		Id remove;
		
		remove = new IdImpl("B-104");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-106");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-110");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-112");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-118");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
		
		remove = new IdImpl("B-120");
		visum2HafasMap.remove(visumHafasLineIds.get(remove));
		visumHafasUnmatchedLineIds.put(remove, visumHafasLineIds.get(remove));
		visumHafasLineIds.remove(remove);
	}
	
//	private void addHafasRemove(){
//		Map<Id, Id> tempMap;
//		for (Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
//			if(visum2HafasMap.containsKey(e.getValue())){
//				tempMap = visum2HafasMap.get(e.getValue());
//				for(TransitRoute hafasRoute: hafasSc.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values()){
//					for (TransitRouteStop stop : hafasRoute.getStops()){
//						if(!(tempMap.containsKey(stop.getStopFacility().getId()))){
//							tempMap.put(stop.getStopFacility().getId(), REMOVE);
//						}
//					}
//				}
//				visum2HafasMap.put(e.getValue(), tempMap);
//			}
//		}
//	}

	private Map<Id, Id> compareLines(Id visum, Id hafas) {
		Map<Id, Id> visumHafasStops = new LinkedHashMap<Id, Id>();
		
		if(!(hafasSc.getTransitSchedule().getTransitLines().containsKey(hafas))){
			return null;
		}
		
		for(TransitRoute hafasRoute : hafasSc.getTransitSchedule().getTransitLines().get(hafas).getRoutes().values()){
			for (TransitRoute visumRoute : visumSc.getTransitSchedule().getTransitLines().get(visum).getRoutes().values()){
				Map<Id, Id> temp = this.compareHafas2VisumRoute(visumRoute, hafasRoute, visumHafasStops);
				if (!(temp == null)){
					visumHafasStops = temp;
				}
			}
		}
		return visumHafasStops;
	}
	
	public void unmatchedToTxt(){
		Map<Id, Collection<Id>> temp = new HashMap<Id, Collection<Id>>();
		
		for(Entry<Id, Id> e : this.visumHafasUnmatchedLineIds.entrySet()){
			Collection<Id> v = new TreeSet<Id>();
			Collection<Id> h = new TreeSet<Id>();
			for(TransitRoute visum : visumSc.getTransitSchedule().getTransitLines().get(e.getKey()).getRoutes().values()){
				for (TransitRouteStop fac : visum.getStops()){
					if(!v.contains(fac.getStopFacility().getId())){
						v.add(fac.getStopFacility().getId());
					}
				}
			}
			temp.put(e.getKey(), v);
			for(TransitRoute hafas : hafasSc.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values()){
				for (TransitRouteStop fac : hafas.getStops()){
					if(!h.contains(fac.getStopFacility().getId())){
						h.add(fac.getStopFacility().getId());
					}
				}
			}
			temp.put(e.getValue(), h);
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(PATH + "/unmatchedLines.txt");
			for(Entry<Id, Id> e : visumHafasUnmatchedLineIds.entrySet()){
				writer.write("visumLine " + e.getKey() + " hafasLine " + e.getValue());
				writer.newLine();
				writer.write("visum \t");
				for(Id id : temp.get(e.getKey())){
					writer.write(id + "\t");
				}
				writer.newLine();
				writer.write("hafas \t");
				for(Id id : temp.get(e.getValue())){
					writer.write(id + "\t");
				}
				writer.newLine();
				writer.newLine();
			}
			writer.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	

	private Map<Id, Id> compareHafas2VisumRoute(TransitRoute visumRoute, TransitRoute hafasRoute, Map<Id, Id> hafasVisum) {
		Map<Id, Id> temp = hafasVisum;
		
		
		if(visumRoute.getStops().size() == hafasRoute.getStops().size()){
			Coord firstVisum = visumRoute.getStops().get(0).getStopFacility().getCoord();
			Coord firstHafas = hafasRoute.getStops().get(0).getStopFacility().getCoord();
			Coord lastHafas  = hafasRoute.getStops().get(hafasRoute.getStops().size()-1).getStopFacility().getCoord();
			
			if(this.getDistance(firstVisum, firstHafas) < this.getDistance(firstVisum, lastHafas)){
				for (int ii = 0; ii < visumRoute.getStops().size(); ii++){
					temp.put(visumRoute.getStops().get(ii).getStopFacility().getId(), hafasRoute.getStops().get(ii).getStopFacility().getId());
				}
			}
			return temp;
		}else{
			return null;
		}
	}
	
	private double getDistance(Coord visum, Coord hafas){
		double xDelta = visum.getX() - hafas.getX();
		double yDelta = visum.getY() - hafas.getY();
		return Math.sqrt(Math.pow(xDelta, 2.0) + Math.pow(yDelta, 2.0));
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
	
	
	public static void main(String[] args){
		DaVisum2HafasMapper1 mapper = new DaVisum2HafasMapper1();
		
		for (Entry<Id, Map<Id, Id>> stops : mapper.getVisum2HafasMap().entrySet()){
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
		
		for(Entry<Id, Id> e : mapper.getVisumHafasLineIds().entrySet()){
			System.out.println(e.getKey() + " " + e.getValue());
		}
		
		mapper.unmatchedToTxt();
		
		
	}
}
