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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author  jbischoff
 *
 */
public class WOBPlansPreparator {
public static void main(String[] args) {
	Config config = ConfigUtils.createConfig();
	Scenario sc = ScenarioUtils.createScenario(config);


	
	new MatsimPopulationReader(sc).readFile("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw079.taxiplans_noCars.xml.gz");
	for (Person p : sc.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		for (int i = 1; i<plan.getPlanElements().size();i=i+2){
		PlanElement pe = plan.getPlanElements().get(i);
		{
			if (pe instanceof Leg){
				Leg leg = (Leg) pe;
				if (leg.getMode().equals("ride")){
					Route route = leg.getRoute();
					Activity pr = (Activity) plan.getPlanElements().get(i-1);
					Activity next = (Activity) plan.getPlanElements().get(i+1);
					route.setDistance(CoordUtils.calcEuclideanDistance(pr.getCoord(), next.getCoord()));
					route.setTravelTime(route.getDistance()/8.33);
				} else if
				 (leg.getMode().equals("pt")){
					Activity pr = (Activity) plan.getPlanElements().get(i-1);
					Activity next = (Activity) plan.getPlanElements().get(i+1);
					Route route = new GenericRouteImpl(pr.getLinkId(), next.getLinkId());
					route.setDistance(CoordUtils.calcEuclideanDistance(pr.getCoord(), next.getCoord()));
					route.setTravelTime(route.getDistance()/6);
					leg.setRoute(route);
				}else if
				 (leg.getMode().equals("walk")){
					Activity pr = (Activity) plan.getPlanElements().get(i-1);
					Activity next = (Activity) plan.getPlanElements().get(i+1);
					Route route = new GenericRouteImpl(pr.getLinkId(), next.getLinkId());
					route.setDistance(CoordUtils.calcEuclideanDistance(pr.getCoord(), next.getCoord()));
					route.setTravelTime(route.getDistance()/1.38);
					leg.setRoute(route);

				}else if
				 (leg.getMode().equals("bike")){
					
					Activity pr = (Activity) plan.getPlanElements().get(i-1);
					Activity next = (Activity) plan.getPlanElements().get(i+1);
					Route route = new GenericRouteImpl(pr.getLinkId(), next.getLinkId());
					route.setDistance(CoordUtils.calcEuclideanDistance(pr.getCoord(), next.getCoord()));
					route.setTravelTime(route.getDistance()/4.00);
					leg.setRoute(route);

				}
			}
		}
	}
	

}
	new PopulationWriter(sc.getPopulation()).write("../../../shared-svn/projects/vw_rufbus/av_simulation/demand/plans/vw079.taxiplans_noCarsR.xml.gz");
}
}

