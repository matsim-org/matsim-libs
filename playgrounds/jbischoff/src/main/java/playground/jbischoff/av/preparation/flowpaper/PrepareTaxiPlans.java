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

package playground.jbischoff.av.preparation.flowpaper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */
public class PrepareTaxiPlans {

	public static void main(String[] args) {
		Random rnd = MatsimRandom.getLocalInstance();
		for (double d = 0.0; d <= 1.0; d = d + 0.10) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimPopulationReader(scenario).readFile("D:/runs-svn/avsim/prerun02_10pct_0.15fc/output_plans.xml.gz");
			Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			List<Person> taxiriders = new ArrayList<>();
			for (Person p : scenario.getPopulation().getPersons().values()) {
				Person p1 = pop2.getFactory().createPerson(p.getId());
				pop2.addPerson(p1);
				Plan plan = p.getSelectedPlan();
				p1.addPlan(plan);

				if (p1.getId().toString().endsWith("t")) {
					taxiriders.add(p1);
				}
			}
			long taxiAgents = Math.round(d * taxiriders.size());
			System.out.println(d + " a: " + taxiAgents);
			for (int i = 0; i < taxiAgents; i++) {
				Person p1 = taxiriders.remove(rnd.nextInt(taxiriders.size()));
				Plan taxiplan = p1.getSelectedPlan();
				Leg l = (Leg) taxiplan.getPlanElements().get(1);
				l.setMode("taxi");
				Id<Link> start = l.getRoute().getStartLinkId();
				Id<Link> end = l.getRoute().getEndLinkId();
				l.setRoute(new GenericRouteImpl(start, end));
			}
			Locale.setDefault(Locale.US);
			DecimalFormat df = new DecimalFormat("####0.00");

			new PopulationWriter(pop2).write("../../../shared-svn/projects/audi_av/scenario/flowpaper/taxiplans/fc0.15/plansCarsRoutesAV"
					+ df.format(d) + ".xml.gz");

		}

	}

}
