/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class AddBorderCrossingTraffic {

	private Population oldPlans;
	private final static Logger log = Logger.getLogger(AddBorderCrossingTraffic.class);

	public void run(Population plans, NetworkImpl network) {
		this.init(network);
		this.assignTTA(plans);
	}

	private void init(NetworkImpl network) {
		ScenarioImpl oldScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		oldScenario.setNetwork(network);
		this.oldPlans = oldScenario.getPopulation();
		final PopulationReader plansReader = new MatsimPopulationReader(oldScenario);
		plansReader.readFile("input/plans/bordercrossing/plans.xml.gz");
	}

	private void assignTTA(Population plans) {

		int cnt = 0;

		for (Person person : this.oldPlans.getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			List<? extends PlanElement> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Activity act = (Activity)actslegs.get(j);


				//if (act.getType().startsWith("tta") || act.getType().startsWith("shop")) {
				if (act.getType().startsWith("tta")) {
					String id = "20" + person.getId().toString();
                    ((PersonImpl) person).setId(Id.create(id, Person.class));
                    ((PersonImpl) person).createDesires("tta");
					((PersonImpl) person).getDesires().putActivityDuration("tta", 8 * 3600);
					plans.addPerson(person);
					cnt++;

					break;
				}
			}
		}
		log.info("Added " + cnt + " persons crossing the border");
	}
}
