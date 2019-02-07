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

package ft.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


import ft.cemdap4H.cemdapPreProcessing.WOBDemandGeneratorCensus;
import playground.vsp.demandde.cemdap.LogToOutputSaver;
import playground.vsp.demandde.cemdap.output.Cemdap2MatsimUtils;
import playground.vsp.demandde.corineLandcover.GeometryUtils;

/**
 * @author saxer
 *
 */
public class CountAgentsWork {
	public static void main(String[] args) throws IOException {
		
		String shapefile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp";
		String zoneIdTag = "NO";
		String vwID="350";
		Point  actpoint = null;
		
		Map<String,SimpleFeature> zones = new HashMap<>();
		for (SimpleFeature feature: ShapeFileReader.getAllFeatures(shapefile)) {
			String shapeId = String.valueOf(feature.getAttribute(zoneIdTag)) ;
		zones.put(shapeId,feature);
		}
		SimpleFeature targetzone = zones.get(vwID); 
		
		int workCounter = 0;
		// Create a Scenario
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// Fill this Scenario with a population.
		new PopulationReader(scenario).readFile("E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\1\\plans.xml.gz");

	

		for (Person person : scenario.getPopulation().getPersons().values()) {

//			 String schoolLoc = (String)
//			 person.getAttributes().getAttribute("locationOfSchool");
//			 if (!schoolLoc.equals("-99"))
//			 {
//			 studentCounter++;
//			 }
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			for (PlanElement planElement : planElements) {				if (planElement instanceof Activity) {					Activity act = (Activity) planElement;					if (act.getType().startsWith("work")) {						actpoint = org.matsim.core.utils.geometry.geotools.MGC.coord2Point(act.getCoord());						
						
						boolean ingeom = ((Geometry) targetzone.getDefaultGeometry()).contains(actpoint);						if (ingeom) {
						
						workCounter++;						break;}					}				}			}			System.out.println (workCounter);		}
		

}}

	

