/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package uam.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class UamDemandCreator {
	private final UniformRandom uniform = RandomUtils.getGlobalUniform();

	private Population generatePlans() {
		Population population = PopulationUtils.createPopulation(new PlansConfigGroup(), null);
		PopulationFactory populationFactory = population.getFactory();
		for (int i = 0; i < UamNetworkCreator.SELECTED_LINK_IDS.size(); i++) {
			Id<Link> fromHub = Id.createLinkId(UamNetworkCreator.HUB_LINK_ID_PREFIX + i);

			for (int j = 0; j < UamNetworkCreator.SELECTED_LINK_IDS.size(); j++) {
				if (i != j) {
					Id<Link> toHub = Id.createLinkId(UamNetworkCreator.HUB_LINK_ID_PREFIX + j);

					int trips = 1;
					double startTime = 0;
					double duration = 3600;
					for (int k = 0; k < trips; k++) {
						double departureTime = (int)uniform.nextDouble(startTime, startTime + duration);
						Id<Person> personId = Id.createPersonId("person_" + i + "_" + j + "_" + k);
						population.addPerson(createPerson(personId, fromHub, toHub, departureTime, populationFactory));
					}
				}
			}
		}
		return population;
	}

	private Person createPerson(Id<Person> id, Id<Link> fromHub, Id<Link> toHub, double departureTime,
			PopulationFactory populationFactory) {
		// act0
		Activity startAct = populationFactory.createActivityFromLinkId("start", fromHub);
		startAct.setEndTime(departureTime);

		// act1
		Activity endAct = populationFactory.createActivityFromLinkId("end", toHub);

		// leg
		Leg leg = populationFactory.createLeg(UamNetworkCreator.UAM_MODE);
		//		if (addEmptyRoute) {
		//			leg.setRoute(RouteUtils.createGenericRouteImpl(startAct.getLinkId(), endAct.getLinkId()));
		//		}
		//		leg.setDepartureTime(startAct.getEndTime());

		Plan plan = populationFactory.createPlan();
		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(endAct);

		Person person = populationFactory.createPerson(Id.createPersonId(id));
		person.addPlan(plan);
		return person;
	}

	public static void main(String[] args) {
		new PopulationWriter(new UamDemandCreator().generatePlans()).write("output/uam/uam_only_population.xml");
	}
}
