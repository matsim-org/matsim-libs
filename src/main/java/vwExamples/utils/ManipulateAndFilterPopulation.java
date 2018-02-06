/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.DoubleStream;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.router.TransitActsRemover;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author  saxer
 *
 */
public class ManipulateAndFilterPopulation {

	//Initialize SubsamplePopulation class
	Set<String> zones = new HashSet<>();
	Map<String, Geometry> zoneMap = new HashMap<>();
	String shapeFile = "D:\\Axer\\CEMDAP2\\cemdap-vw\\add_data\\shp\\wvi-zones.shp";
	static String serachMode = "pt";
	static String newMode = "drt";
	String shapeFeature = "NO";
	double pct = 0.01;
	String searchedActivityName = "home";
	
	//Constructor which reads the shape file for later use!
	public ManipulateAndFilterPopulation() {
	readShape(this.shapeFile,this.shapeFeature);	
	}
	
public static void main(String[] args) {

	ManipulateAndFilterPopulation manipulateAndFilterPopulation = new ManipulateAndFilterPopulation();
	//Create a Scenario
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	//Fill this Scenario with a population.
	new PopulationReader(scenario).readFile("D:\\Axer\\MatsimDataStore\\BaseCases\\vw208\\vw208.1.0.output_plans.xml.gz");
	String filteredPopDesination = "D:\\Axer\\MatsimDataStore\\BaseCases\\vw208\\vw208.1.0.output_plans_filtered.xml.gz";
	StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
	filteredPop.startStreaming(filteredPopDesination);
	
//	int i = 0;
//	double pct = 0.01;
	for (Person p : scenario.getPopulation().getPersons().values()){
//		double randValue = MatsimRandom.getRandom().nextDouble();
		
		//Check whether this person's home location is located within a releant zone 
		//If not, we will skip this person
		
		if (checkAgentLocationAndActivity(p,manipulateAndFilterPopulation.searchedActivityName,manipulateAndFilterPopulation.zoneMap) == false) continue;
		
		//Remove TransitActs
		new TransitActsRemover().run(p.getSelectedPlan());

		//If it is a relevant person, we assign certain legs with person's selected plans to a new mode
		for (PlanElement pe :p.getSelectedPlan().getPlanElements()){

			if (pe instanceof Leg) {
				if (((Leg)pe).getMode().equals(serachMode)) {
					
					//Write newMode into Leg
					((Leg)pe).setMode(newMode);
					//Remove route from leg
					((Leg)pe).setRoute(null);
					((Leg)pe).getAttributes().removeAttribute("trav_time");
				}
			}
			
		}
		

		
		filteredPop.writePerson(p);
	}

	filteredPop.closeStreaming();
}

public void readShape(String shapeFile, String featureKeyInShapeFile) {
	Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
	for (SimpleFeature feature : features) {
		String id =  feature.getAttribute(featureKeyInShapeFile).toString();
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		this.zones.add(id);
		this.zoneMap.put(id, geometry);
	}
}

public static boolean checkAgentLocationAndActivity(Person person,String searchedActivityName, Map<String, Geometry> zoneMap2) {
	boolean relevantAgent = false;
	
	
	for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
		if (pe instanceof Activity) {
			if (((Activity)pe).getType().contains(searchedActivityName)) {
				
				Activity activity = ((Activity)pe);
				Coord coord = activity.getCoord();
				if (isWithinZone(coord,zoneMap2)){
					relevantAgent= true;
					System.out.println("Relavent Agend: "+person.getId().toString());
					
				}
				
				}
			}
		}
	
	if (relevantAgent) return true;
	else return false;
	}
	


public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap){
	//Function assumes EPSG:25832
	
	boolean relevantCoord = false;
	for (String zone : zoneMap.keySet()) {
		Geometry geometry = zoneMap.get(zone);
		if(geometry.contains(MGC.coord2Point(coord))) relevantCoord=true;

	}
	if (relevantCoord) return true;
	else return false;
		
}





}


