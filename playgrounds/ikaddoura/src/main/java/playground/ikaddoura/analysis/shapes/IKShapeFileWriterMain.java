/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.shapes;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class IKShapeFileWriterMain {
	
	private static final Logger log = Logger.getLogger(IKShapeFileWriterMain.class);

	static String netFile = "/Users/ihab/Desktop/berlin_network.xml";
	static String outputPath = "/Users/ihab/Desktop/analysis/";
	private Scenario scenario;
	private IKShapeFileWriter shapeFileWriter = new IKShapeFileWriter();
			
	public static void main(String[] args) throws IOException {
				
		IKShapeFileWriterMain analysis = new IKShapeFileWriterMain();
		analysis.run();
	}
	
	public void run() throws IOException {
		
		this.scenario = getScenario(netFile);
		
		File directory = new File(outputPath);
		directory.mkdirs();
		
		shapeFileWriter.writeShapeFileLines(this.scenario, outputPath + "network/", "network.shp");

		log.info("Done.");
	}

	
	private void writeShapeFiles_homeWorkCoord(List<Person> persons, String outputDir) {

		SortedMap<Id,Coord> homeCoordinates = new TreeMap<Id,Coord>();
		SortedMap<Id,Coord> workCoordinates = new TreeMap<Id,Coord>();
		
		homeCoordinates = getCoordinates(persons, "home");
		workCoordinates = getCoordinates(persons, "Work");
		
		String path = outputPath + outputDir + "/shapeFiles";
		File directory = new File(path);
		directory.mkdirs();
		
		if (persons.isEmpty()){
			// do nothing
		} else {
			shapeFileWriter.writeShapeFilePoints(scenario, homeCoordinates, path + "/homeCoordinates.shp");
			shapeFileWriter.writeShapeFilePoints(scenario, workCoordinates, path + "/workCoordinates.shp");	
		}
	}
	
	private SortedMap<Id, Coord> getCoordinates(List<Person> persons, String activity) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : persons){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals(activity)){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}
		
	private Scenario getScenario(String netFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

}
