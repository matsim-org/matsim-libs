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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
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
	String zonePrefix = "1";
	static String serachMode = "pt";
	static String newMode = "drt";
	String shapeFeature = "NO";
	static double samplePct = 0.01; //Global sample ratio
	static double replancementPct = 1.0; //Ratio of mode substitution 
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
	new TransitScheduleReader(scenario).readFile("D:\\Axer\\MatsimDataStore\\BaseCases\\vw208\\vw208.1.0.output_transitSchedule.xml.gz");
	String filteredPopDesination = ("D:\\Axer\\MatsimDataStore\\WOB_BS_DRT\\BS\\input\\population\\vw208_sampleRate"+samplePct+"replaceRate_"+replancementPct+"_"+serachMode+"_"+newMode+".xml.gz");
	StreamingPopulationWriter filteredPop = new StreamingPopulationWriter();
	filteredPop.startStreaming(filteredPopDesination);
	


	
	Map<Id<TransitLine>,TransitLine> transitLines = scenario.getTransitSchedule().getTransitLines();
	
//	
//	for ( Id<TransitLine> id : transitLines.keySet() ) 
//	{
//	System.out.println(id.toString() + "Dies ist die ID");
//	
//	}
	

	for (Person p : scenario.getPopulation().getPersons().values())
	{


		//Sample a certain percentage of the hole population
		if (MatsimRandom.getRandom().nextDouble() < samplePct) 
		{

				Plan plan = p.getSelectedPlan();
			
				//Check whether this person's home location is located within a relevant zone, whereas the zone needs to fit with the zonePrefix
				//Otherwise, we do not touch this person
				//if (checkAgentLocationAndActivity(p,manipulateAndFilterPopulation.searchedActivityName,manipulateAndFilterPopulation.zoneMap,manipulateAndFilterPopulation.zonePrefix)) 
				 
				//{
					
					//Modify only a certain percentage of relevant Agents
					if (MatsimRandom.getRandom().nextDouble() < replancementPct) 
					{
						
						new TransitActsRemover().run(plan);
				
				
						//If it is a relevant person, we assign certain legs with person's selected plans to a new mode
						
						for (PlanElement pe : plan.getPlanElements())
						{
				
							if (pe instanceof Leg) 
							{
								Leg leg = ((Leg)pe);
								if (leg.getMode().equals(serachMode)) 
								{
									
									if (checkAgentLegWithinZone(plan, leg,manipulateAndFilterPopulation.zoneMap,manipulateAndFilterPopulation.zonePrefix)) 
									{
									
//									if (getPtTransportMode(leg,transitLines).equals("bus"))
//										{
											System.out.println("Replaced pt leg with " + newMode);
											//Write newMode into Leg
											leg.setMode(newMode);
											//Remove route from leg
											//leg.setRoute(null);
											leg.getAttributes().removeAttribute("trav_time");
											//System.out.println(leg);
											
//										}
										
									}
								}
							}
							
						
							
							
						}
						PersonUtils.removeUnselectedPlans(p);
						
					}
				//}

			
			
		}
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

public static boolean checkAgentLocationAndActivity(Person person,String searchedActivityName, Map<String, Geometry> zoneMap2, String zonePrefix) {
	boolean relevantAgent = false;
	
	
	for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
		if (pe instanceof Activity) {
			if (((Activity)pe).getType().contains(searchedActivityName)) {
				
				Activity activity = ((Activity)pe);
				Coord coord = activity.getCoord();
				if (isWithinZone(coord,zoneMap2,zonePrefix)){
					relevantAgent= true;
					
					
				}
				
				}
			}
		}
	
	if (relevantAgent) {
		System.out.println("Relavent Agend: "+person.getId().toString());
		return true;
	}
	else return false;
	}


	
public static boolean checkAgentLegWithinZone(Plan plan, Leg leg, Map<String, Geometry> zoneMap2, String zonePrefix) {
	boolean prevActInZone = false;
	boolean nextActInZone = false;
	
	Activity prevAct = PopulationUtils.getPreviousActivity(plan, leg);
	Activity nextAct = PopulationUtils.getNextActivity(plan, leg);
		
	
	if(isWithinZone(prevAct.getCoord(), zoneMap2,zonePrefix)) prevActInZone= true;
	if(isWithinZone(nextAct.getCoord(), zoneMap2,zonePrefix)) nextActInZone= true;
	
	
	if ((prevActInZone == true) && (nextActInZone == true) ) {
		System.out.println("Leg in Zone: "+plan.getPerson().getId().toString());
		return true;
	}
	else return false;
	}


public static String getPtTransportMode(Leg leg, Map<Id<TransitLine>,TransitLine> transitLines) {
	//Initialize variables 
	String transportMode = null;
	String transitLineID = null;

	//We could get the PtTransportMode only if a route is already stored in agent's plan
	if (leg.getRoute() != null) 
	{
	String routeDescription = leg.getRoute().getRouteDescription();
	String[] routeDescriptionElements =  routeDescription.split("===");
	transitLineID = routeDescriptionElements[2];
	
//	System.out.println(transitLineID);
	
	Id<TransitLine> transitLineIDDummy =  Id.create(transitLineID, TransitLine.class);
	
	if (transitLineID != null)
	{
		transportMode = transitLines.get(transitLineIDDummy).getRoutes().entrySet().iterator().next().getValue().getTransportMode();

	}
	
	}
	else {
	    throw new RuntimeException("Public Transport Route is missing in agente's plan.");
	}
	
	return transportMode; 
	
	
	}

public static boolean isWithinZone(Coord coord, Map<String, Geometry> zoneMap, String zonePrefix){
	//Function assumes EPSG:25832
	
	boolean relevantCoord = false;
	for (String zone : zoneMap.keySet()) {
		
		//If the zone does not fit to the require zonePrefix
		if(zone.startsWith(zonePrefix)==false) continue;
		Geometry geometry = zoneMap.get(zone);
		if(geometry.contains(MGC.coord2Point(coord))) relevantCoord=true;

	}
	if (relevantCoord) return true;
	else return false;
		
}

//public static boolean getPtMode(Id<Link> startLink) {
//	
//	boolean prevActInZone = false;
//	boolean nextActInZone = false;
//	
//	Activity prevAct = PopulationUtils.getPreviousActivity(plan, leg);
//	Activity nextAct = PopulationUtils.getNextActivity(plan, leg);
//	
//	if (leg.getRoute() != null) {
//	Id<Link> startLink = leg.getRoute().getStartLinkId();
//	Id<Link> endLink = leg.getRoute().getEndLinkId();
//	}
//	
//	
//	
//	
//	if(isWithinZone(prevAct.getCoord(), zoneMap2,zonePrefix)) prevActInZone= true;
//	if(isWithinZone(nextAct.getCoord(), zoneMap2,zonePrefix)) nextActInZone= true;
//	
//	
//	if ((prevActInZone == true) && (nextActInZone == true) ) {
//		System.out.println("Relavent Agent: "+plan.getPerson().getId().toString());
//		return true;
//	}
//	else return false;
//	}





}


