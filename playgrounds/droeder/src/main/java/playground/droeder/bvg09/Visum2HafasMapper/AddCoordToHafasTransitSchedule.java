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
package playground.droeder.bvg09.Visum2HafasMapper;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class AddCoordToHafasTransitSchedule {
	
	private static final Logger log = Logger
			.getLogger(AddCoordToHafasTransitSchedule.class);
	
	private static String PATH_PREFIX = DaPaths.BVG09 + "BVG-Fahrplan_2008/Daten/1_Mo-Do/";
	private static String COORDFILE = PATH_PREFIX + "bfkoord";
//	private static String FILENAME = "D:/Berlin/BVG/berlin-bvg09/pt/nullfall_M44_344_U8/alldat";
	private static String INFILENAME = DaPaths.OUTPUT + "bvg09/transitSchedule-HAFAS.xml";
	private static String OUTFILENAME = DaPaths.OUTPUT + "bvg09/transitSchedule-HAFAS-Coord.xml";
	
	private ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private Map<Id, Coord> facilityCoord = new HashMap<Id, Coord>();
	
	public void addCoord(){
		sc.getConfig().scenario().setUseTransit(true);
		this.readSchedule(INFILENAME);
		this.readHafasCoord(COORDFILE);
		this.addCoordToHafasSchedule();
		this.writeSchedule(OUTFILENAME);
	}
	
	
	private void readSchedule(String fileName){
		TransitScheduleReader reader = new TransitScheduleReader(this.sc);
		reader.readFile(fileName);
	}
	
	private void readHafasCoord(String fileName){
		String line;
		Coord coord;
		
		try {
			BufferedReader reader = IOUtils.getBufferedReader(fileName);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split(" ");
					coord = new CoordImpl(Double.valueOf(columns[1]), Double.valueOf(columns[2]));
					facilityCoord.put(new IdImpl(columns[0]), coord);
				}
				line = reader.readLine();
			}while(!(line == null));
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	private void addCoordToHafasSchedule(){
		
		for (TransitStopFacility tsf : sc.getTransitSchedule().getFacilities().values()){
			if(facilityCoord.containsKey(tsf.getId())){
				Coord coord = facilityCoord.get(tsf.getId());
				tsf.getCoord().setXY(coord.getX(), coord.getY());
			}else{
				log.error("no coords for " + tsf.getId());
			}
		}
	}
	
	private void writeSchedule(String outputFile){
		TransitScheduleWriter writer = new TransitScheduleWriter(sc.getTransitSchedule());
		writer.writeFile(outputFile);
	}
	
	public static void main(String[] args){
		AddCoordToHafasTransitSchedule addCoordToHafas = new AddCoordToHafasTransitSchedule();
		addCoordToHafas.addCoord();
	}

}
