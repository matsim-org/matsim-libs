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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class WobPlansFilter {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw078.taxiplans_alltrips.xml.gz");
		Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
		for (Person p : scenario.getPopulation().getPersons().values()){
			boolean copyPerson = false;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(TaxiModule.TAXI_MODE)){
						copyPerson = true;
					}
				}
			}
			if (copyPerson){
				Person p2 = pop2.getFactory().createPerson(p.getId());
				p2.addPlan(plan);
				pop2.addPerson(p2);
			}
		}
		new  PopulationWriter(pop2).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw078.onlyTaxiplans_alltrips.xml.gz");
		replaceCarLegsByTeleport(pop2);
		new  PopulationWriter(pop2).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw078.onlyTaxiplansCarTeleport_alltrips.xml.gz");
	}
	
	static void replaceCarLegsByTeleport(Population pop){
		for (Person p : pop.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(TransportMode.car))
							{
						Id<Link> start = ((Leg) pe).getRoute().getStartLinkId();
						Id<Link> end = ((Leg) pe).getRoute().getStartLinkId();
						((Leg) pe).setMode(TransportMode.ride);
						((Leg) pe).setRoute(new GenericRouteImpl(start, end));
					}
				}
			}
		}
	}

}
