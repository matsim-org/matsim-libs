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

package playground.jbischoff.taxibus.scenario.plans;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.zone.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * @author  jbischoff
 *
 */
public class WOBAgentFilter {
	Map<Id<Zone>,Zone> zones;
public static void main(String[] args){
	WOBAgentFilter f = new WOBAgentFilter();
	f.run();
}

private void run()
{
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new MatsimPopulationReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/scenario/input/tb.output_plans.xml.gz");
	new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../shared-svn/projects/vw_rufbus/scenario/input/networkptcc.xml");
	zones = Zones.readZones("../../../shared-svn/projects/vw_rufbus/scenario/input/zones/wob.xml", "../../../shared-svn/projects/vw_rufbus/scenario/input/zones/wob.shp");
	
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = scenario2.getPopulation();
	int i = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){

			boolean copyPerson = false;
			i++;
			if (i%10000 == 0) System.out.println(i);
			for (Plan plan : p.getPlans())
			{
				Id<Zone> lastActZone = null;
				Leg lastLeg = null;
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						
						Coord c = scenario.getNetwork().getLinks().get(((Activity) pe).getLinkId()).getCoord();
						Id<Zone> zoneAct = getZone(c);
						if (((Activity) pe).getType().startsWith("pt")){
							zoneAct = null;
							lastActZone = null;
						}
						
						 if ((zoneAct!=null)&&(lastActZone!=null)){
							if (zoneAct!=lastActZone){
							lastLeg.setMode("taxibus");
							lastLeg.setRoute(new GenericRouteImpl(lastLeg.getRoute().getStartLinkId(),lastLeg.getRoute().getEndLinkId()));
							copyPerson = true;
							
							}
							}
							else {
								lastActZone = zoneAct;
							}
							}
						
					
					
					if (pe instanceof Leg){
						lastLeg = (Leg) pe;
						}
					}
				 
			}
			if (copyPerson){
				pop2.addPerson(p);
			}
			
		
	}
	System.out.println(i + " persons found ; "+pop2.getPersons().size()+" persons copied");
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/tbonly.output_plans.xml.gz");
}

Id<Zone> getZone(Coord coord){
	for (Zone z : zones.values()){
		if (z.getMultiPolygon().contains(MGC.coord2Point(coord))) return z.getId();
	}
	return null;
}
}
