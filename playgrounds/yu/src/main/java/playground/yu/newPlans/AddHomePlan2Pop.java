/* *********************************************************************** *
 * project: org.matsim.*
 * AddHomePlan2Pop.java
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
package playground.yu.newPlans;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author yu
 * 
 */
public class AddHomePlan2Pop extends NewPopulation {
	private PopulationFactory populationFactory;
	private static String WALK = TransportMode.walk;

	public AddHomePlan2Pop(Network network, Population population,
			String outputPopulationFilename) {
		super(network, population, outputPopulationFilename);
		populationFactory = population.getFactory();
	}

	private Activity createHomeActivity(Activity referenceActivity) {
		Coord homeCoord = referenceActivity.getCoord();
		if (homeCoord == null) {
			homeCoord = network.getLinks().get(referenceActivity.getLinkId())
					.getCoord();
		}

		// if (linkId != null && network.getLinks().containsKey(linkId)) {
		// Activity homeAct = populationFactory.createActivityFromLinkId(
		// referenceActivity.getType(), linkId);
		// if (homeCoord != null) {
		// ((ActivityImpl) homeAct).setCoord(homeCoord);
		// }
		// return homeAct;
		// } else {// then, there must be coordinates for the activity
		//
		// if (homeCoord == null) {
		// throw new RuntimeException(
		// "There is not \"link\" or \"coord\" bei the first Activity:\t"
		// + referenceActivity.toString());
		// }

		return populationFactory.createActivityFromCoord(referenceActivity
				.getType(), homeCoord);

	}

	private Leg createDummyLegAtHome(String transportMode,
			Id firstActivityLinkId) {
		Leg leg = populationFactory.createLeg(transportMode);
		// leg.setTravelTime(10d);
		Route route = new GenericRouteImpl(firstActivityLinkId,
				firstActivityLinkId);
		route.setDistance(0d);
		leg.setRoute(route);
		return leg;
	}

	/**
	 * Creates a plan whereby the agents stay at home the whole day
	 */
	private void createHomePlan(Person person) {
		Plan homePlan = populationFactory.createPlan();

		// List<PlanElement> planElements = ;
		Activity firstAct = (Activity) person.getSelectedPlan()
				.getPlanElements().get(0);
		// Activity lastAct = (Activity) planElements.get(planElements.size() -
		// 1);

		Activity homeAct = createHomeActivity(firstAct);
		homeAct.setEndTime(21600d);
		homePlan.addActivity(homeAct);

		homePlan.addLeg(createDummyLegAtHome(WALK, firstAct.getLinkId()));

		Activity lastHomeAct = createHomeActivity(firstAct);
		lastHomeAct.setStartTime(21700d);
		homePlan.addActivity(lastHomeAct);

		person.addPlan(homePlan);
	}

	@Override
	protected void beforeWritePersonHook(Person person) {
		createHomePlan(person);
	}

	public static void main(final String[] args) {
		final String netFilename, inputPopFilename, outputPopFilename;
		if (args.length == 3) {
			netFilename = args[0];
			inputPopFilename = args[1];
			outputPopFilename = args[2];
		} else {
			netFilename = "../matsimTests/ParamCalibration/network.xml";
			inputPopFilename = "../matsimTests/ParamCalibration/general2/fixChoiceSetPop1000.xml.gz";
			outputPopFilename = "../matsimTests/ParamCalibration/general2/fixChoiceSetPop1000stayHome.xml.gz";
		}

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());

		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(inputPopFilename);

		AddHomePlan2Pop nsp = new AddHomePlan2Pop(network, population,
				outputPopFilename);
		nsp.run(population);
		nsp.writeEndPlans();
	}

}
