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

package playground.jbischoff.av.preparation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.taxi.TaxiUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
public class WOBPlansModifier {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new MatsimPopulationReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw078.output_plansNoPTRoutes.xml.gz");
	Geometry geometry = ScenarioPreparator.readShapeFileAndExtractGeometry("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/zones/onezone.shp");
	int z = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){
		z++;
		if (z % 20000 == 0){System.out.println(z);}
		for (Plan plan : p.getPlans()){
			
			boolean previousActInArea = false;
			
			for (int i = 0;i < plan.getPlanElements().size();i = i+2){
			if (i == 0){
				Activity act0 = (Activity) plan.getPlanElements().get(0);
				if (geometry.contains(MGC.coord2Point(act0.getCoord()))) previousActInArea = true;
				else break;
			} else {
				Activity currentAct = (Activity) plan.getPlanElements().get(i);
				boolean currentActInArea = geometry.contains(MGC.coord2Point(currentAct.getCoord()));
				if (previousActInArea && currentActInArea){
					Leg leg = (Leg) plan.getPlanElements().get(i-1);
					if (leg.getMode().equals(TransportMode.car)){
						
						leg.setMode(TaxiUtils.TAXI_MODE);
						Id<Link> start = leg.getRoute().getStartLinkId();
						Id<Link> end = leg.getRoute().getEndLinkId();
						leg.setRoute(new GenericRouteImpl(start, end));
					}
				}
				previousActInArea = currentActInArea;
			}	
			}
			
		}
	}
	new PopulationWriter(scenario.getPopulation()).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw078.taxiplans.xml.gz");
}
}
