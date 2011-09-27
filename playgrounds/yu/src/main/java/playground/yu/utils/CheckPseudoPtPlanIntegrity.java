/* *********************************************************************** *
 * project: org.matsim.*
 * CheckPseudoPtPlansIntegrity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.utils;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * if a {@code Plan} with pseudo pt {@code Leg} does NOT contain enough
 * information about pseudo pt {@code Leg}, e.g. distance, or the travel time of
 * a pseudo pt {@code Route} is not reliable any longer (e.g. because of change
 * of {@code Leg} {@code TransportMode}), this class will set the corresponding
 * beeline distance and -Infinity traveltime to the {@code Route}
 *
 * @author yu
 *
 */
public class CheckPseudoPtPlanIntegrity {
	public static Plan check(Network network, Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getMode().equals(TransportMode.pt)) {
					Route route = leg.getRoute();
					if (Double.isNaN(route.getDistance())) {
						leg.setTravelTime(Double.NEGATIVE_INFINITY);
						route.setTravelTime(Double.NEGATIVE_INFINITY);
						route.setDistance(CalculateLegBeelineDistance
								.getBeelineDistance(network, leg));
						// route.setTravelTime(144);
						leg.setRoute(route);
					}
				}
			}
		}
		return plan;
	}

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario)
				.readFile("test/input/2car1ptRoutes/net2.xml");
		new MatsimPopulationReader(scenario)
				.readFile("test/input/2car1ptRoutes/preparePop/1.xml");

		Network net = scenario.getNetwork();
		Population pop = scenario.getPopulation();

		for (Person person : pop.getPersons().values()) {
			Set<Plan> tmpPlans = new HashSet<Plan>();
			for (Plan plan : person.getPlans()) {
				tmpPlans.add(CheckPseudoPtPlanIntegrity.check(net, plan));
			}
			person.getPlans().clear();
			for (Plan plan : tmpPlans) {
				person.addPlan(plan);
			}
		}

		new PopulationWriter(pop, net)
				.write("test/output/2car1ptRoutes/preparePop/1dist.xml");
	}
}
