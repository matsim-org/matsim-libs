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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class DaHafas2VisumMapper1{
	
	private static final Logger log = Logger
			.getLogger(DaHafas2VisumMapper1.class);
	
	private static String PATH = DaPaths.OUTPUT + "bvg09/";
 	
	private static String VISUM = PATH + "intermediateTransitSchedule.xml";
	private static String HAFAS = PATH + "transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl visum = new ScenarioImpl();
	private ScenarioImpl hafas = new ScenarioImpl();
	
	private double beta = 1.0;
	
	private Map<Id, Id> visumHafasFacs;
	private Map<Id, Id> visumHafasLineIds = null;
	private Map<Id, Map<Id, Id>> linesHafasVisumAll = null;
	private Map<Id, Map<Id, Id>> linesHafasVisumNeedToCheck = null;
	
	
	final Id ERROR = new IdImpl("ERROR");
	final Id REMOVE = new IdImpl("REMOVE");
	
	public DaHafas2VisumMapper1(){
		visum.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUM, visum);
		hafas.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFAS, hafas);
	}

	public void writeMatchedFacsByCoordToTxt(){
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(PATH + "matchedFacsByCoord.txt");
			writer.write("visum" + "\t" + "hafas");
			writer.newLine();
			for(Entry<Id, Id> e : visumHafasFacs.entrySet()){
				writer.write(e.getKey().toString() + "\t" + e.getValue().toString());
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
	private double getDelta(Coord visum, Coord hafas){
		double delta;
		delta = Math.sqrt(Math.pow(visum.getX()-hafas.getX(), 2) + Math.pow(visum.getY()-hafas.getY(), 2));
		return delta;
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
				visumHafasLineIds.put(line.getId(), ERROR);
			}
		}
	}
	
	public Map<Id, Id> getVisumHafasLineIds(){
		if(visumHafasLineIds==null){
			this.createHafasLineIdsFromVisum();
		}
		return visumHafasLineIds;
	}
	
	private void createHafas2VisumLinesMapAll(){
		linesHafasVisumAll = new TreeMap<Id, Map<Id,Id>>();
		linesHafasVisumNeedToCheck = new TreeMap<Id, Map<Id,Id>>();
		HashMap<Id, Id> tempMap;
		HashMap<Id, Id> needToCheck;
		if(visumHafasLineIds==null){
			this.createHafasLineIdsFromVisum();
		}
		
		for (Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
			int i = 1;
			for(TransitRoute visumRoute : visum.getTransitSchedule().getTransitLines().get(e.getKey()).getRoutes().values() ){
				for (TransitRoute hafasRoute : hafas.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values() ){
					tempMap = null;
					if (visumRoute.getStops().size() == hafasRoute.getStops().size()){
						for (int ii = 0; ii < visumRoute.getStops().size(); ii++){
							if(this.checkFacsById(visumRoute.getStops().get(ii).getStopFacility().getId(), hafasRoute.getStops().get(ii).getStopFacility().getId()) == true){
								if (tempMap == null) tempMap = new HashMap<Id, Id>();
								tempMap.put(hafasRoute.getStops().get(ii).getStopFacility().getId(), visumRoute.getStops().get(ii).getStopFacility().getId());
							}
						}
					}
					if(!(tempMap == null)){
						linesHafasVisumAll.put(e.getValue(), tempMap);
					}
				}
			}
		}
		
		this.addHafasRemove();
	}
	
	private void addHafasRemove(){
		Map<Id, Id> tempMap;
		
		
		for (Entry<Id, Id> e : this.visumHafasLineIds.entrySet()){
			if(linesHafasVisumAll.containsKey(e.getValue())){
				tempMap = linesHafasVisumAll.get(e.getValue());
				for (TransitRoute hafasRoute : hafas.getTransitSchedule().getTransitLines().get(e.getValue()).getRoutes().values() ){
					for(TransitRouteStop stop : hafasRoute.getStops()){
						if(!(tempMap.containsKey(stop.getStopFacility().getId()))){
							tempMap.put(stop.getStopFacility().getId(), REMOVE);
						}
						
					}
				}
				linesHafasVisumAll.put(e.getValue(), tempMap);
			}
		}
	}
	
	public Map<Id, Map<Id, Id>>  getHafas2VisumMapAll(){
		if(linesHafasVisumAll== null) this.createHafas2VisumLinesMapAll();
		return linesHafasVisumAll;
	}
	public Map<Id, Map<Id, Id>>  getHafas2VisumNeedToCheck(){
		if(linesHafasVisumNeedToCheck== null) this.createHafas2VisumLinesMapAll();
		return linesHafasVisumNeedToCheck;
	}
	
	public boolean checkFacsById(Id vis, Id haf){
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
	
	
	
	
	public void matchFacsById(){
		Map<Id, Map<Id, Id>> hafas2visumMap = new HashMap<Id, Map<Id,Id>>();
		for(Entry<Id, Map<Id, Id>> e : hafas2visumMap.entrySet()){
			log.error(e.getKey().toString());
			
			for(Entry<Id, Id> ee: e.getValue().entrySet()){
				String hafas = null;
				String visum =  ee.getValue().toString();
				if (visum.length() == 6){
					hafas = ee.getKey().toString().substring(2, ee.getKey().toString().length()-1);
				}else if(visum.length() == 5){
					hafas = ee.getKey().toString().substring(3, ee.getKey().toString().length()-1);
				}else if(visum.length() == 7){
					hafas = ee.getKey().toString().substring(1, ee.getKey().toString().length()-1);
				}
				visum = visum.substring(0, visum.length()-2);
				if(visum.equals(hafas)){
					log.info(ee.getKey().toString() + " " + ee.getValue());
				}else{
					log.error(ee.getKey().toString() + " " + ee.getValue());
				}
			}
		}
	}
	
	public void matchFacsByCoord(){
		visumHafasFacs = new HashMap<Id, Id>();
		Coord visumCoord;
		Coord hafasCoord;
		
		for (TransitStopFacility fac: visum.getTransitSchedule().getFacilities().values()){
			double temp = 1000000000;
			visumCoord = fac.getCoord();
			for (TransitStopFacility fac2 : hafas.getTransitSchedule().getFacilities().values()){
				hafasCoord = fac2.getCoord();
				if(getDelta(visumCoord, hafasCoord)<beta && getDelta(visumCoord, hafasCoord)<temp){
					visumHafasFacs.put(fac.getId(), fac2.getId());
					temp = getDelta(visumCoord, hafasCoord);
				}
			}
		}
	}
	
	public static void main(String[] args){
		DaHafas2VisumMapper1 mapper = new DaHafas2VisumMapper1();
//		mapper.matchFacsByCoord();
//		mapper.writeMatchedFacsByCoordToTxt();
		
//		for(Entry<Id, Id> e : mapper.getVisumHafasLineIds().entrySet()){
//			System.out.println(e.getKey().toString() + " " + e.getValue().toString());
//		}
		for (Entry<Id, Map<Id, Id>> e:  mapper.getHafas2VisumMapAll().entrySet()){
			System.out.println("lineId: " + e.getKey().toString());
			for(Entry<Id, Id> ee : e.getValue().entrySet()){
				System.out.print(ee.getKey().toString() + "\t");
			}
			System.out.println("");
			for(Entry<Id, Id> ee : e.getValue().entrySet()){
				System.out.print(ee.getValue().toString() + "\t");
			}
			System.out.println("");
		}
		
		
//		System.out.println("####################################");
//		for (Entry<Id, Map<Id, Id>> e:  mapper.getHafas2VisumNeedToCheck().entrySet()){
//			System.out.println("lineId: " + e.getKey().toString());
//			for(Entry<Id, Id> ee : e.getValue().entrySet()){
//				System.out.print(ee.getKey().toString() + "\t");
//			}
//			System.out.println("");
//			for(Entry<Id, Id> ee : e.getValue().entrySet()){
//				System.out.print(ee.getValue().toString() + "\t");
//			}
//			System.out.println("");
//		}
	}
}
