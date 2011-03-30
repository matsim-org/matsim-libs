/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityRandomizer2.java
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
package playground.johannes.socialnetworks.sim.locationChoice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author illenberger
 * 
 */
public class ActivityRandomizer2 implements PlanFilter {

	private final List<Link> links;

	private final Random random;

	private String typePredecate;

	private final ActivityMover mover;

	public ActivityRandomizer2(PopulationFactory factory, Network network, LeastCostPathCalculator router, long rndSeed) {
		this(factory, network, router, null, rndSeed);
	}

	public ActivityRandomizer2(PopulationFactory factory, Network network, LeastCostPathCalculator router, String type,
			long rndSeed) {
		mover = new ActivityMover(factory, router, network);
		random = new Random(rndSeed);
		this.typePredecate = type;

		links = new ArrayList<Link>(network.getLinks().values());
	}

	@Override
	public boolean apply(Plan plan) {
		boolean result = false;

		for (int i = 0; i < plan.getPlanElements().size(); i += 2) {
			Activity act = (Activity) plan.getPlanElements().get(i);

			if (validate(act)) {
				Link link = links.get(random.nextInt(links.size()));

				mover.moveActivity(plan, i, link.getId());

				result = true;
			}
		}

		return result;
	}

	private boolean validate(Activity act) {
		if (typePredecate != null) {
			if (typePredecate.equals(act.getType()))
				return true;
		} else {
			return true;
		}

		return false;
	}

	public static void main(String args[]) {
		Config config = new Config();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
//		loader.loadScenario();
//		Scenario scenario = loader.getScenario();

		TravelTime travelTime = new FreespeedTravelTimeCost(-6 / 3600.0, 0, 0);
		Dijkstra router = new Dijkstra(scenario.getNetwork(), (TravelCost) travelTime, travelTime);

		ActivityRandomizer2 randomizer = new ActivityRandomizer2(scenario.getPopulation().getFactory(),
				scenario.getNetwork(), router, "leisure", 4711);
		
		PlanFilterEngine.apply(scenario.getPopulation(), randomizer);
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(scenario.getConfig().getParam("popfilter", "outputPlansFile"));
	}
}
