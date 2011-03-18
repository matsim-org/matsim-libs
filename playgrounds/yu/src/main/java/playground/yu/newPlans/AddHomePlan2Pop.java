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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.analysis.forBln.Analysis4Bln.ActTypeBln;

/**
 * @author yu
 * 
 */
public class AddHomePlan2Pop extends NewPopulation {
	private PopulationFactory populationFactory;
	private static String HOME_BERLIN = ActTypeBln.home.getActTypeName();

	public AddHomePlan2Pop(Network network, Population population,
			String outputPopulationFilename) {
		super(network, population, outputPopulationFilename);
		populationFactory = population.getFactory();
	}

	private Activity createHomeActivity(Activity firstActivity) {
		Id linkId = firstActivity.getLinkId();
		if (linkId != null && net.getLinks().containsKey(linkId)) {
			return populationFactory.createActivityFromLinkId(HOME_BERLIN,
					linkId);
		} else {// then, there must be coordinates for the activity
			Coord homeCoord = firstActivity.getCoord();
			if (homeCoord == null) {
				throw new RuntimeException(
						"There is not \"link\" or \"coord\" bei the first Activity:\t"
								+ firstActivity.toString());
			}
			return populationFactory.createActivityFromCoord(HOME_BERLIN,
					homeCoord);
		}
	}

	/**
	 * Creates a plan whereby the agents stay at home the whole day
	 */
	private void createHomePlan(Person person) {
		Plan homePlan = populationFactory.createPlan();
		Activity firstAct = (Activity) person.getSelectedPlan()
				.getPlanElements().get(0);

		Activity homeAct = createHomeActivity(firstAct);
		homeAct.setEndTime(21600d);
		homePlan.addActivity(homeAct);

		Leg leg = populationFactory.createLeg("walk");
		homePlan.addLeg(leg);

		Activity lastHomeAct = createHomeActivity(firstAct);
		lastHomeAct.setStartTime(22000d);
		homePlan.addActivity(lastHomeAct);

		person.addPlan(homePlan);
	}

	@Override
	protected void beforeWritePersonHook(Person person) {
		createHomePlan(person);
	}

	public static void main(final String[] args) {
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		final String netFilename = "../matsimTests/ParamCalibration/network.xml";
		final String inputPopFilename = "../matsimTests/ParamCalibration/40.plans.xml.gz";
		final String outputPopFilename = "../matsimTests/diverseRoutes/stayHomePop.xml";
		// new ScenarioLoader(
		// // "./test/yu/ivtch/config_for_10pctZuerich_car_pt_smallPlansl.xml"
		// // "../data/ivtch/make10pctPlans.xml"
		// "input/make10pctPlans.xml").loadScenario().getConfig();

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
