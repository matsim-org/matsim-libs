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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public abstract class AbstractDaVisum2HafasMapper {
	
	protected static final Logger log = Logger.getLogger(DaVisum2HafasMapper1.class);
	
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private String UNMATCHED = PATH + "unmatchedLines.txt";
	private String MATCHED = PATH + "matchedRoutes.txt";
	
	private ScenarioImpl visumSc = null;
	private ScenarioImpl hafasSc = null;
	
	final Id NOTMATCHED = new IdImpl("notMatched");
	
	protected Map<Id, Id> preVisum2HafasMap = null;
	
	private SortedMap<Id, Id> vis2HafLines = null;
	private Map<Id, Map<Id, Id>> vRoute2vis2hafStops = null;
	private Map<Id, Id> vis2hafRoutes = null;
	private Map<Id, Id> vRoute2vLine = null;
	private Collection<Id> unmatched = null;
	
	protected final double distToMatch;


	public AbstractDaVisum2HafasMapper(double dist2Match){
		this.visumSc = new ScenarioImpl();
		this.hafasSc = new ScenarioImpl();
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visumSc);
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafasSc);
		this.createHafasLineIdsFromVisum();
		this.distToMatch = dist2Match;
	}
	
//	public static void main(String[] args){
//		DaVisum2HafasMapper2 mapper = new DaVisum2HafasMapper2();
//		mapper.run();
//	}
	
	public void run(){
		this.createHafasLineIdsFromVisum();
		this.preMatchStops();
		this.matchLines();
		this.analyseResults();
//		this.unmatched2txt();
//		this.matched2txt();

	}
	
	public Map<Id, Id> getPrematchedStops(){
		if(preVisum2HafasMap == null) this.preMatchStops();
		return preVisum2HafasMap;
	}
	
	public Map<Id, Id> getVis2HafLines(){
		if (vis2HafLines == null) this.createHafasLineIdsFromVisum();
		return vis2HafLines;
	}
	
	public Map<Id, Map<Id, Id>> getVisRoute2Vis2HafStops(){
		if (vRoute2vis2hafStops == null) this.run();
		return vRoute2vis2hafStops;
	}
	
	public Collection<Id> getUnmatchedLines(){
		if(unmatched == null) this.run();
		return unmatched;
	}
	
	public TransitSchedule getVisumTransit(){
		return visumSc.getTransitSchedule();
	}
	
	public TransitSchedule getHafasTransit(){
		return hafasSc.getTransitSchedule();
	}
	
	public Map<Id, Id> getVisum2HafasRoute(){
		if(this.vis2hafRoutes == null) this.run();
		return vis2hafRoutes;
	}
	
	private void matchLines(){
		vis2hafRoutes = new HashMap<Id, Id>();
		vRoute2vis2hafStops = new HashMap<Id, Map<Id,Id>>();
		vRoute2vLine = new HashMap<Id, Id>();
		unmatched = new ArrayList<Id>();
		
		for (Entry<Id, Id> lines : vis2HafLines.entrySet()){
			Map<Id, Id> matched = tryToMatchAllRoutes(lines.getKey(), lines.getValue());
			if(matched.containsValue(null)){
				log.error("not all visum routes are matched for visum Line " + lines.getKey() + "!");
				unmatched.add(lines.getKey());
			}else {
				log.info("all visum routes are matched for visum Line " + lines.getKey() + "!");
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
				matchedRoutes.put(visRoute.getId(), matched);
			}else{
				log.info("matched hafasRoute " + matched + " to visRoute " + visRoute.getId() + " for visum Line " + visLine);
				matchedRoutes.put(visRoute.getId() , matched);
				vRoute2vLine.put(visRoute.getId(), visLine);
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
					return routeId;
				}else{
					vRoute2vis2hafStops.put(visRoute.getId(), temp);
				}
			}
		}
		return routeId;
	}
	
	protected abstract Map<Id, Id> tryToMatchRoute(TransitRoute visRoute, TransitRoute hafRoute);
	
	private void analyseResults(){
		
//		int routes = 0;
//		for(TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
//			routes += line.getRoutes().size();
//		}
//		log.info("###visum-scenario### lines:"+ visumSc.getTransitSchedule().getTransitLines().size() + 
//				" routes:" + routes + " stops:" + visumSc.getTransitSchedule().getFacilities().size());
//		
//		routes = 0;
//		for(TransitLine line : hafasSc.getTransitSchedule().getTransitLines().values()){
//			routes += line.getRoutes().size();
//		}
//		log.info("###hafas-scenario### lines:"+ hafasSc.getTransitSchedule().getTransitLines().size() + 
//				" routes:" + routes + " stops:" + hafasSc.getTransitSchedule().getFacilities().size());
//		
//		int h = 0, v = 0, l = 0;
//		for(Entry<Id, Id> e : vis2HafLines.entrySet()){
//			v += visumSc.getTransitSchedule().getTransitLines().get(e.getKey()).getRoutes().size();
//			h += hafasSc.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().size();
//			l++;
////			if(visumSc.getTransitSchedule().getTransitLines().containsKey(e.getKey()) && hafasSc.getTransitSchedule().getTransitLines().containsKey(e.getValue())){
////			}
//		}
//		log.info("lines in both scenarios:" + l + " Routes v:" + v + " h:" + h);
		
		Map<Id, Id> temp;
		int all = 0;
		int part = 0;
		int no = 0;
		
		for(Id id : vis2HafLines.keySet()) {
			TransitLine vLine = visumSc.getTransitSchedule().getTransitLines().get(id);
			double avOfAv = 0;
			int i = 0;
			for (Id vRoute : vLine.getRoutes().keySet()){
				double av = 0;
				temp = vRoute2vis2hafStops.get(vRoute);
				if(!(temp == null)){
					av = getAvDist(temp);
					i++;
					log.info("average distance of matched Stops for route " + vRoute + " on line " + vLine.getId() + " is " + av + " m!");
				}else{
					log.warn("route " + vRoute + " on line " + vLine.getId() + " was not matched!");
				}
				avOfAv +=av;
			}

			avOfAv = avOfAv/i;
			if(i == 0){
				log.error("no route was matched for line " +id + "!");
				no++;
			}else if(i<vLine.getRoutes().size()){
				log.warn(i + " of " + vLine.getRoutes().size() + " routes where matched for route " + vLine.getId() + 
						"! The average of the average distance for all matched routes is " + avOfAv + " m!");
				part++;
			}else{
				log.info("all routes where matched for route " + vLine.getId() + 
						"! The average of the average distance for all matched routes is " + avOfAv + " m!");
				all++;
			}
		}
		
		log.info(all + " lines complete matched"); 
		log.info(part + " lines matched partially"); 
		log.info(no + " lines not matched");
		
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
		for(Entry<Id, Id> one : preVisum2HafasMap.entrySet()){
			List<Id> temp = new ArrayList<Id>();
			for (Entry<Id, Id> two : preVisum2HafasMap.entrySet()){
				if(two.getValue().equals(one.getValue()) && !(two.getKey().equals(one.getKey()))) {
					temp.add(two.getKey());
				}
			}
			if (temp.size() > 1) v.addAll(temp);
		}
		
		// remove doublematched
		for (Id id : v){
			this.preVisum2HafasMap.remove(id);
		}
		
	}
	
	protected double getAvDist(Map<Id, Id> stops){
		double dist = 0;
		
		for (Entry<Id, Id> e : stops.entrySet()){
			dist += getDist(e.getKey(), e.getValue());
		}
		dist = dist/stops.size();

		return dist;
	}
	
	protected double getDist(Id vStop, Id hStop){
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
		visum = visum.substring(0, visum.length()-1);
		if(visum.equals(hafas)){
			equal = true;
		}
		
		return equal;
	}
	
	private void unmatched2txt(){
		BufferedWriter writer;
		
		try {
			writer = IOUtils.getBufferedWriter(UNMATCHED);
			
			for(Id id : unmatched){
				writer.write("visumLine: " + id);
				writer.newLine();
				for(TransitRoute route : visumSc.getTransitSchedule().getTransitLines().get(id).getRoutes().values()){
					writer.write(route.getId() + "\t");
					for(TransitRouteStop stop : route.getStops()){
						writer.write(stop.getStopFacility().getId() + "\t");
					}
					writer.newLine();
				}
				
				for(TransitRoute route : hafasSc.getTransitSchedule().getTransitLines().get(vis2HafLines.get(id)).getRoutes().values()){
					writer.write(route.getId() + "\t");
					for(TransitRouteStop stop : route.getStops()){
						writer.write(stop.getStopFacility().getId() + "\t");
					}
					writer.newLine();
				}
				writer.newLine();
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void matched2txt(){
		BufferedWriter writer;
		
		try {
			writer = IOUtils.getBufferedWriter(MATCHED);
			
			for(Entry<Id, Id> e : vRoute2vLine.entrySet()){
				if(!(e.getValue() == null) ){
					writer.write(e.getKey() + "\t");
					for(TransitRouteStop stop : visumSc.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().get(e.getKey()).getStops()){
						writer.write(stop.getStopFacility().getId().toString() + "\t");
					}
					writer.newLine();
					
					writer.write(vis2hafRoutes.get(e.getKey()) + "\t");
					for(TransitRouteStop stop : hafasSc.getTransitSchedule().getTransitLines().get(vis2HafLines.get(e.getValue())).getRoutes().get(vis2hafRoutes.get(e.getKey())).getStops()){
						writer.write(stop.getStopFacility().getId().toString() + "\t");
					}
					writer.newLine();
					writer.newLine();
				}
			}
			
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 *  transforms matched positions of stops to Id-pairs and check if the order of stops is possible
	 *  returns null if not
	 * @param matched
	 * @param vis
	 * @param haf
	 * @return
	 */
	public Map<Id, Id> position2Id(Map<Integer, Integer> matched, TransitRoute vis, TransitRoute haf){
		HashMap<Id, Id> id2Id = new HashMap<Id, Id>();
		Integer v = -1;
		Integer h = -1;
		
		if(matched == null) return null;
		
		for(Entry<Integer, Integer> e : matched.entrySet()){
			if(e.getKey()> v && e.getValue() > h){
				v = e.getKey();
				h = e.getValue();
			}else{
				return null;
			}
			id2Id.put(vis.getStops().get(e.getKey()).getStopFacility().getId(), haf.getStops().get(e.getValue()).getStopFacility().getId());
		}
		
		if(id2Id.size() == vis.getStops().size()){
			return id2Id;
		}else{
			return null;
		}
	}
	
	
	
}
