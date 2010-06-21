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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
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
public class DaVisum2HafasMapper2 {
	
	private static final Logger log = Logger
			.getLogger(DaVisum2HafasMapper2.class);
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl visumSc = new ScenarioImpl();
	private ScenarioImpl hafasSc = new ScenarioImpl();
	
	final Id NOTMATCHED = new IdImpl("notMatched");
	
	private Map<Id, Id> preVisum2HafasMap = null;
	
	private SortedMap<Id, Id> vis2HafLines = null;
	private Map<Id, Map<Id, Id>> vRoute2vis2hafStops = null;
	private Map<Id, Id> vis2hafRoutes = null;
	
	
	
	
	private final double distToMatch = 100.0; 

	public DaVisum2HafasMapper2(){
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
	}
	
	public static void main(String[] args){
		DaVisum2HafasMapper2 mapper = new DaVisum2HafasMapper2();
		mapper.run();
	}
	
	public void run(){
		this.createHafasLineIdsFromVisum();
		this.preMatchStops();
		this.matchLines();
	}
	
	public Map<Id, Id> getPrematchedStops(){
		if(preVisum2HafasMap == null) this.preMatchStops();
		return preVisum2HafasMap;
	}
	
	public Map<Id, Id> getVis2HafLines(){
		if (vis2HafLines == null) this.createHafasLineIdsFromVisum();
		return vis2HafLines;
	}
	
	private void matchLines(){
		
		for (Entry<Id, Id> lines : vis2HafLines.entrySet()){
			Map<Id, Id> matched = tryToMatchAllRoutes(lines.getKey(), lines.getValue());
			if(matched.size() != visumSc.getTransitSchedule().getTransitLines().get(lines.getKey()).getRoutes().size()){
				log.error("only " + matched.size() + " of " + visumSc.getTransitSchedule().getTransitLines().get(lines.getKey()).getRoutes().size() + " where matched");
			}else {
				log.info("all visum routes are matched!");
			}
			vis2hafRoutes.putAll(matched);
		}
		
	}
	
	private Map<Id, Id> tryToMatchAllRoutes(Id visLine, Id hafLine){
		Map<Id, Id> matchedRoutes = new HashMap<Id, Id>();
		
		for(TransitRoute visRoute :  visumSc.getTransitSchedule().getTransitLines().get(visLine).getRoutes().values()){
			Id matched = searchBestFittingRoute(visRoute, hafLine);
			if(matched == null){
				log.error("not able to match a HafasRoute to VisumRoute " + visRoute.getId() + " for visum Line " + visLine); 
			}else{
				log.info("matched hafasRoute " + matched + " to visRoute " + visRoute + " for visum Line " + visLine);
				matchedRoutes.put(visRoute.getId() , matched);
			}
		}
		
		
		return matchedRoutes;
	}
	
	private Id searchBestFittingRoute(TransitRoute visRoute, Id hafLine){
		Id routeId = null;
		double avDist = Double.POSITIVE_INFINITY;
		
		for(TransitRoute hafRoute : hafasSc.getTransitSchedule().getTransitLines().get(hafLine).getRoutes().values()){
			Map<Id, Id> temp = tryToMatchRoute(visRoute, hafRoute);
			if(!(temp == null)){
				double tempDist = getAvDist(temp);
				if(tempDist < avDist){
					avDist = tempDist;
					routeId = hafRoute.getId();
					vRoute2vis2hafStops.put(visRoute.getId(), temp);
				}
			}
		}
		
		return routeId;
	}
	
	private Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute){
		
		
		
		
		return null;
	}
	
	private void preMatchStops(){
		this.preVisum2HafasMap = new TreeMap<Id, Id>();
		
		// match by shortest dist and Id
		for(TransitStopFacility vStop : visumSc.getTransitSchedule().getFacilities().values()){
			Id next =  null;
			double shortest = Double.POSITIVE_INFINITY;
			for (TransitStopFacility hStop : hafasSc.getTransitSchedule().getFacilities().values()){
				double dist = this.getDist(vStop.getId(), hStop.getId());
				if(dist<shortest && this.checkFacsById(vStop.getId(), hStop.getId())){
					shortest = dist;
					next = hStop.getId();
				}
			}
			if (shortest<distToMatch && (!(next == null))){
				preVisum2HafasMap.put(vStop.getId(), next);
			}
		}
		
		//check if a hafasStop is matched to different VisumStops
		List<Id> v = new ArrayList<Id>();
		for(Id h : preVisum2HafasMap.values()){
			List<Id> temp = new ArrayList<Id>();
			for (Entry<Id, Id> e : preVisum2HafasMap.entrySet()){
				if(e.getValue().equals(h)) temp.add(e.getKey());
			}
			if (temp.size() > 1) v.addAll(temp);
		}
		
		// remove doublematched
		for (Id id : v){
			this.preVisum2HafasMap.remove(id);
		}
		
		System.out.println(preVisum2HafasMap.size());
	}
	
	private double getAvDist(Map<Id, Id> stops){
		double dist = 0;
		
		for (Entry<Id, Id> e : stops.entrySet()){
			dist += getDist(e.getKey(), e.getValue());
		}
		dist = dist/stops.size();

		return dist;
	}
	
	private double getDist(Id vStop, Id hStop){
		Coord v = visumSc.getTransitSchedule().getFacilities().get(vStop).getCoord();
		Coord h = hafasSc.getTransitSchedule().getFacilities().get(hStop).getCoord();
		
		double xDif = v.getX() - h.getX();
		double yDif = v.getY() - h.getY();
		
		return Math.sqrt(Math.pow(xDif, 2.0) + Math.pow(yDif, 2.0));
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
		vis2HafLines = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
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
				vis2HafLines.put(line.getId() , new IdImpl(hafasId));
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
	
	
	
//	private void test (){
//		System.out.println(checkFacsById(new IdImpl("533520"), new IdImpl("9053352")));
//		System.out.println(getDist(new IdImpl("533520"), new IdImpl("9053352")));
//	}
	
}
