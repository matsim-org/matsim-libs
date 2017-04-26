/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.csberlin.evaluation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarUsageEvaluator {
public static void main(String[] args) {
//	Geometry geo = JbUtils.readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/gis/klaus.shp","id").get("0");
	Geometry geo = JbUtils.readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/gis/mierendorffkiez.shp","id").get("1");
	
	Set<Id<Person>> firstCarUsers = new HashSet<>();
	Set<Id<Person>> secondCarUsers = new HashSet<>();
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new PopulationReader(scenario).readFile("D:/runs-svn/bmw_carsharing/run22/ITERS/it.0/run22.0.plans.xml.gz");
	int persons = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){
		
		Plan plan = p.getSelectedPlan();
		Activity hAct = (Activity) plan.getPlanElements().get(0);
		if (!geo.contains(MGC.coord2Point(hAct.getCoord()))){
			continue;
		}
		persons++;
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Leg){
				if (((Leg) pe).getMode().equals("car")||((Leg) pe).getMode().equals("freefloating")){
					firstCarUsers.add(p.getId());
					break;
				}
			} 
		}
	}
	
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario2).readFile("D:/runs-svn/bmw_carsharing/run22/ITERS/it.150/run22.150.plans.xml.gz");
	for (Person p : scenario2.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		Activity hAct = (Activity) plan.getPlanElements().get(0);
		if (!geo.contains(MGC.coord2Point(hAct.getCoord()))){
			continue;
		}
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Leg){
				if (((Leg) pe).getMode().equals("car")||((Leg) pe).getMode().equals("freefloating")){
					secondCarUsers.add(p.getId());
					break;
				}
			} 
		}
	}
	int oldcarowners = 0;
	int newcarowners = 0;
	for (Id<Person> p  : firstCarUsers){
		if (!secondCarUsers.contains(p)){
			oldcarowners++;
		}
	}
	
	for (Id<Person> p  : secondCarUsers){
	if (!firstCarUsers.contains(p)){
		newcarowners++;
	}
	}
	
	System.out.println("Overall persons living in area: "+persons);
	System.out.println("Users first case: "+firstCarUsers.size());
	System.out.println("Users second case: "+secondCarUsers.size());
	System.out.println("less owners: "+oldcarowners);
	System.out.println("new owners: "+newcarowners);
}
}
