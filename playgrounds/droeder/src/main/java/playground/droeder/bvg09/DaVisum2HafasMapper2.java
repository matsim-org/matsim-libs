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
	
	final Id CHECK = new IdImpl("CHECK");
	final Id REMOVE = new IdImpl("REMOVE");
	final Id DOUBLE = new IdImpl("DOUBLE");

	private SortedMap<Id, Id> allVisumHafasLineIds = null;
	
	private Map<Id, Id> preVisum2HafasMap = null;
	
	private SortedMap<Id, Id> matchedVisumHafasLineIds = null;
	private Map<Id, Map<Id, Id>> visum2HafasMap = null;
	private Map<Id, List<Id>> matchedRoutes = null;
	
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
		mapper.test();
	}
	
	public void run(){
		this.preMatchStops();
	}
	
	public Map<Id, Map<Id, Id>> getVisum2HafasMap(){
		if(visum2HafasMap == null) this.calcVisum2HafasMap();
		return visum2HafasMap;
	}
	
	public Map<Id, Id> getPrematchedStops(){
		if(preVisum2HafasMap == null) this.preMatchStops();
		return preVisum2HafasMap;
	}
	
	public Map<Id, Id> getAllVisumHafasLineIds(){
		if (allVisumHafasLineIds == null) this.createHafasLineIdsFromVisum();
		return allVisumHafasLineIds;
	}
	
	public Map<Id, Id> getMatchedLines(){
		if (this.matchedVisumHafasLineIds == null) this.calcVisum2HafasMap();
		return this.matchedVisumHafasLineIds;
	}
	
	private void calcVisum2HafasMap() {
		this.preMatchStops();
		this.matchAllLines();
		
		// list all matched lines visum2Hafas
		this.matchedVisumHafasLineIds = new TreeMap<Id, Id>();
		for(Id id :  this.visum2HafasMap.keySet()){
			this.matchedVisumHafasLineIds.put(id, allVisumHafasLineIds.get(id));
		}
	}


	
	private void matchAllLines(){
		this.visum2HafasMap = new HashMap<Id, Map<Id,Id>>();
		matchedRoutes = new HashMap<Id, List<Id>>();
		int i = 0;
		for(Entry<Id, Id> e : this.allVisumHafasLineIds.entrySet()){
			if(hafasSc.getTransitSchedule().getTransitLines().containsKey(e.getValue())){
				i++;
				Map<Id, Id> temp = this.matchAllRoutes(e.getKey());
				if(!(temp == null)){
					log.info("matched visumLine " + e.getKey());
					this.visum2HafasMap.put(e.getKey(), temp);
				}else{
					log.error("not able to match visumLine " + e.getKey() + "! Only " + this.matchedRoutes.get(e.getKey()).size() + " of " + 
							visumSc.getTransitSchedule().getTransitLines().get(e.getKey()).getRoutes().size() + " routes are matched!");
				}
			}
		}
		log.info(this.visum2HafasMap.size() + " of " + i + " visumLines are matched (visum2Hafas)!");
	}
	
	private Map<Id, Id> matchAllRoutes(Id vLine){

		Map<Id, Id> temp = new HashMap<Id, Id>();
		List<Id> routes = new ArrayList<Id>();
		for(TransitRoute vRoute : visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().values()){
			Map<Id, Id> temp2 = this.searchHafasRouteForVisum(vRoute, allVisumHafasLineIds.get(vLine));
			if(!(temp2 == null)){
				temp.putAll(temp2);
				routes.add(vRoute.getId());
			}
		}
		
		matchedRoutes.put(vLine, routes);
		if (routes.size() ==  visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().size()){
			return temp;
		}else{
			return null;
		}
	}
	
	private Map<Id, Id> searchHafasRouteForVisum(TransitRoute vRoute, Id hLine){
		 Map<Integer, Tuple<Id, Id>> temp = new HashMap<Integer, Tuple<Id, Id>>();
		
		 // get prematched ids#
		 int  i = 0;
		 for(TransitRouteStop vStop : vRoute.getStops()){
			if(this.preVisum2HafasMap.containsKey(vStop.getStopFacility().getId())){
				temp.put(i, new Tuple<Id, Id>(vStop.getStopFacility().getId(), preVisum2HafasMap.get(vStop.getStopFacility().getId())));
			}else{
				temp.put(i, new Tuple<Id, Id>(vStop.getStopFacility().getId(), CHECK));
			}
			i++;
		 }
		 
		 
		 for(TransitRoute hRoute : hafasSc.getTransitSchedule().getTransitLines().get(hLine).getRoutes().values()){
			 Map<Id, Id> m = this.compareRoutes(vRoute, hRoute, temp);
			 if(!(m == null)){
				 return m;
			 }
		 }
		 return null;
		 
	}
	
	private Map<Id, Id> compareRoutes(TransitRoute vRoute, TransitRoute hRoute, Map<Integer, Tuple<Id, Id>> temp){
		Map<Id, Id> matched =  new HashMap<Id, Id>();
		
		ListIterator<TransitRouteStop> hIterator;
		ListIterator<TransitRouteStop> vIterator = vRoute.getStops().listIterator();
		boolean matching = false;
		TransitRouteStop vStop;
		TransitRouteStop hStop;
		
		do{
			if(vIterator.hasNext()){
				vStop = vIterator.next();
			}else{
				return null;
			}
			if(hRoute.getStops().size()> vIterator.previousIndex()){
				hIterator = hRoute.getStops().listIterator(vIterator.previousIndex());
			}else{
				return null;
			}
			while(hIterator.hasNext() && matching == false){
				hStop = hIterator.next();
				if(temp.get(vIterator.previousIndex()).getFirst().equals(vStop.getStopFacility().getId()) && 
						temp.get(vIterator.previousIndex()).getSecond().equals(hStop.getStopFacility().getId())){
					matching = true;
				}
			}
		}while (matching == false);
		
		int i = 0;
		if((vRoute.getStops().size() - vIterator.previousIndex()) > (hRoute.getStops().size() - hIterator.previousIndex())){
			return null;
		}else{
			hIterator = hRoute.getStops().listIterator(hIterator.previousIndex() - vIterator.previousIndex());
			vIterator = vRoute.getStops().listIterator();
			
			while(hIterator.hasNext() && vIterator.hasNext()){
				hStop = hIterator.next();
				vStop = vIterator.next();
				if(temp.get(vIterator.previousIndex()).getFirst().equals(vStop.getStopFacility().getId()) && 
						temp.get(vIterator.previousIndex()).getSecond().equals(hStop.getStopFacility().getId())) {
					matched.put(vStop.getStopFacility().getId(), hStop.getStopFacility().getId());
					i++;
				}else{
					matched.put(vStop.getStopFacility().getId(), hStop.getStopFacility().getId());
				}
			}
		}
		
		if( (0.75 < (1.0 * i / temp.size())) && (temp.size() == matched.size())){
			return matched;
		}else{
			return null;
		}
		
		
	}
	
//	private void checkAllLinesAfterPreMatch(){
//		
//		for(TransitLine vLine: visumSc.getTransitSchedule().getTransitLines().values()){
//			if (this.checkAllLineStopsMatched(vLine.getId()) ){
//				System.out.println("all stops matched by Id and Dist for visumLine :" + vLine.getId());
//			}
//		}
//	}
//	
//	private boolean checkAllLineStopsMatched(Id vLine){
//		
//		for(TransitRoute vRoute : visumSc.getTransitSchedule().getTransitLines().get(vLine).getRoutes().values()){
//			for(TransitRouteStop stop : vRoute.getStops()){
//				if(!preVisum2HafasMap.containsKey(stop.getStopFacility().getId())){
//					return false;
//				}
//			}
//		}
//		return true;
//	}
	
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
		allVisumHafasLineIds = new TreeMap<Id, Id>();
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
				allVisumHafasLineIds.put(line.getId() , new IdImpl(hafasId));
			}else if (createdHafasId.length()>0){
				allVisumHafasLineIds.put(line.getId(), CHECK);
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
	
	
	
	private void test (){
		System.out.println(checkFacsById(new IdImpl("533520"), new IdImpl("9053352")));
		System.out.println(getDist(new IdImpl("533520"), new IdImpl("9053352")));
	}
	
}
