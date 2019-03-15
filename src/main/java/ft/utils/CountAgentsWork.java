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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

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

			for (PlanElement planElement : planElements) {

				if (planElement instanceof Activity) {
					Activity act = (Activity) planElement;
					if (act.getType().startsWith("work")) {
						actpoint = org.matsim.core.utils.geometry.geotools.MGC.coord2Point(act.getCoord());						
						
						boolean ingeom = ((Geometry) targetzone.getDefaultGeometry()).contains(actpoint);
						if (ingeom) {
						
						workCounter++;
						break;}
					}
				}
			}
			System.out.println (workCounter);
		}
		


}
}

	

