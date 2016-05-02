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

package playground.jbischoff.csberlin.plans.preprocess;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.router.TransitActsRemover;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class AnalyseAgentsInArea {

	public static void main(String[] args) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/network.xml.gz");
		new MatsimPopulationReader(scenario).readFile("D:/runs-svn/berlin-bvg09/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.plans.selected.xml.gz");
		Geometry geo = JbUtils.readShapeFileAndExtractGeometry("../../../shared-svn/projects/bmw_carsharing/data/gis/mierendorffkiez.shp","id").get("1");
		int i = 0;
		for (Person p : scenario.getPopulation().getPersons().values()){
			i++;
			if (i%200000==0) System.out.println(i);
			Plan plan = p.getSelectedPlan();
			Activity act = (Activity) plan.getPlanElements().get(0);
			if (act.getType().contains("home")){
				Coord coord = act.getCoord();
				if (geo.contains(MGC.coord2Point(coord))){
					scenario2.getPopulation().addPerson(p);
				}
			}
			
		}
		for (Person p : scenario2.getPopulation().getPersons().values()){
			Plan plan = p.getSelectedPlan();
			new TransitActsRemover().run(plan);
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					Leg leg = (Leg) pe;
					if (leg.getMode().equals("car")){
						leg.setRoute(null);
					}
				}
			}
		}
		
		new PopulationWriter(scenario2.getPopulation()).writeFileV4("../../../shared-svn/projects/bmw_carsharing/data/scenario/mierendorff-plans.xml.gz");
	}

}
