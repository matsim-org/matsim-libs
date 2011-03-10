/* *********************************************************************** *
 * project: org.matsim.*
 * PlanIdShuffle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.evacuation.planidshuffle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author laemmel
 * 
 */
public class PlanIdShuffle {
	public static void main(String[] args) {
		String in = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/plans/padang_plans_v20100707_EAF_flooding.xml.gz";
		String out = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/plans/padang_plans_shuffled_v20100707_EAF_flooding.xml.gz";
		String net = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/network/padang_net_evac_v20100317.xml.gz";
		ScenarioImpl sc1 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		ScenarioImpl sc2 = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc2).readFile(net);
		new MatsimNetworkReader(sc1).readFile(net);
		new PopulationReaderMatsimV4(sc1).readFile(in);
		List<Person> l = new ArrayList<Person>();
		for (Person p : sc1.getPopulation().getPersons().values()) {
			l.add(p);
		}

		Random rand = MatsimRandom.getRandom();
		rand.nextDouble();
		rand.nextDouble();
		Collections.shuffle(l, rand);

		int count = 0;
		PopulationFactory fac = sc2.getPopulation().getFactory();
		for (Person p : sc1.getPopulation().getPersons().values()) {

			Id id = new IdImpl(count++);
			Person pers = fac.createPerson(id);
			pers.addPlan(p.getSelectedPlan());
			sc2.getPopulation().addPerson(pers);

		}

		new PopulationWriter(sc2.getPopulation(), sc2.getNetwork()).write(out);
	}

}
