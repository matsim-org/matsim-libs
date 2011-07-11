/* *********************************************************************** *
 * project: org.matsim.*
 * UnCar2Pt.java
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

package playground.yu.newPlans;

import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class UnCar2Pt extends NewPopulation {

	public UnCar2Pt(Network network, Population population,
			String outputPopulationFilename) {
		super(network, population, outputPopulationFilename);
	}

	@Override
	protected void beforeWritePersonHook(Person person) {
		List<? extends Plan> plans = person.getPlans();
		for (Plan plan : plans) {
			List<PlanElement> pes = plan.getPlanElements();
			for (PlanElement pe : pes) {
				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (!leg.getMode().equals("car")) {
						leg.setMode("pt");
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		String netFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml.gz"//
		, popFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/plans/baseplan_900s.xml.gz"//
		, newPopFilename = "D:/Daten/work/shared-svn/studies/countries/de/berlin/plans/baseplan_900s_car_pt.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);
		new MatsimPopulationReader(scenario).readFile(popFilename);

		Population pop = scenario.getPopulation();

		UnCar2Pt uc2p = new UnCar2Pt(scenario.getNetwork(), pop, newPopFilename);
		uc2p.run(pop);
		uc2p.writeEndPlans();
	}

}
