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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class IntermodalUamDemandCreator {
	private final UniformRandom uniform = RandomUtils.getGlobalUniform();
	private final double coordEpsilon;//[m]

	private final Network network;

	public IntermodalUamDemandCreator(Network network, double coordEpsilon) {
		this.network = network;
		this.coordEpsilon = coordEpsilon;
	}

	private Population generatePlans(int tripsPerHubPair) {
		Population population = PopulationUtils.createPopulation(new PlansConfigGroup(), null);
		PopulationFactory populationFactory = population.getFactory();
		for (int i = 0; i < UamNetworkCreator.SELECTED_LINK_IDS.size(); i++) {
			Id<Link> fromHub = Id.createLinkId(UamNetworkCreator.HUB_LINK_ID_PREFIX + i);
			Coord fromCoord = network.getLinks().get(fromHub).getCoord();

			for (int j = 0; j < UamNetworkCreator.SELECTED_LINK_IDS.size(); j++) {
				if (i != j) {
					Id<Link> toHub = Id.createLinkId(UamNetworkCreator.HUB_LINK_ID_PREFIX + j);
					Coord toCoord = network.getLinks().get(toHub).getCoord();

					int trips = tripsPerHubPair;
					double startTime = 0;
					double duration = 30 * 3600;
					for (int k = 0; k < trips; k++) {
						double departureTime = (int)uniform.nextDouble(startTime, startTime + duration);
						Id<Person> personId = Id.createPersonId("person_" + i + "_" + j + "_" + k);
						population.addPerson(createPerson(personId, randomizeCoord(fromCoord), randomizeCoord(toCoord),
								departureTime, populationFactory));
					}
				}
			}
		}
		return population;
	}

	private Coord randomizeCoord(Coord coord) {
		return new Coord(coord.getX() + uniform.nextDouble(-coordEpsilon, coordEpsilon),
				coord.getY() + uniform.nextDouble(-coordEpsilon, coordEpsilon));
	}

	private Person createPerson(Id<Person> id, Coord fromCoord, Coord toCoord, double departureTime,
			PopulationFactory populationFactory) {
		// act0
		Activity startAct = populationFactory.createActivityFromCoord("start", fromCoord);
		startAct.setEndTime(departureTime);

		// act1
		Activity endAct = populationFactory.createActivityFromCoord("end", toCoord);

		// leg
		Leg leg = populationFactory.createLeg(UamNetworkCreator.UAM_MODE);
		leg.setDepartureTime(startAct.getEndTime());

		Plan plan = populationFactory.createPlan();
		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(endAct);

		Person person = populationFactory.createPerson(Id.createPersonId(id));
		person.addPlan(plan);
		return person;
	}

	public static void main(String[] args) {
		int tripsPerHubPair = 10;
		int coordEpsilon = 2000;
		Network network = NetworkUtils.readNetwork("input/uam/uam_only_network.xml");

		new PopulationWriter(
				new IntermodalUamDemandCreator(network, coordEpsilon).generatePlans(tripsPerHubPair)).write(
				"output/uam/intermodal_uam_population_6x5x" + tripsPerHubPair + "_epsilon_" + coordEpsilon + ".xml");
	}
}
