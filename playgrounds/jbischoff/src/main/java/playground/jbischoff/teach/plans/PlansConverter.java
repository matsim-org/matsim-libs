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

package playground.jbischoff.teach.plans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author  jbischoff
 * reads plans and converts their coordinate system. Implies that all activities have coordinates
 */
public class PlansConverter {

	public static void main(String[] args) {
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:32633", TransformationFactory.DHDN_GK4);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("C:/Users/Joschka/Desktop/test.xml");
		
		for (Person p : scenario.getPopulation().getPersons().values()){
			for (Plan plan : p.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Coord coord =((Activity) pe).getCoord();
						Coord newCoord = ct.transform(coord);
//						coord.setX(newCoord.getX());
//						coord.setY(newCoord.getY());
						((Activity) pe).setCoord(newCoord);
					}
				}
			}
		}
	new PopulationWriter(scenario.getPopulation()).write("C:/Users/Joschka/Desktop/testO.xml");	
		
	}

}
