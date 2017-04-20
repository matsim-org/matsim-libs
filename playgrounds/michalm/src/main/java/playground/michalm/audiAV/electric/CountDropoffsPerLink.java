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

package playground.michalm.audiAV.electric;

import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.CompactCSVWriter;
import org.matsim.core.config.*;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.collect.Maps;

public class CountDropoffsPerLink {

	public static void main(String[] args) {
		String dir = "../../../shared-svn/projects/audi_av/scenario/";
		String networkFile = dir + "networkc.xml.gz";
		String plansFile = dir + "plans.xml.gz";
		String dropoffCountsFile = dir + "JAIHC_paper/dropoffs_per_link.txt";

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new PopulationReader(scenario).readFile(plansFile);

		// for each leg, get the destination link
		Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
		Map<Id<Link>, MutableInt> counts = Maps.newHashMapWithExpectedSize(links.size());
		for (Id<Link> id : links.keySet()) {
			counts.put(id, new MutableInt());
		}

		int allLegs = 0;
		int taxiLegs = 0;
		Map<Id<Person>, ? extends Person> persons = scenario.getPopulation().getPersons();
		for (Person p : persons.values()) {
			List<PlanElement> planElements = p.getSelectedPlan().getPlanElements();
			if (planElements.size() != 3) {
				throw new RuntimeException();
			}
			if (((Leg)planElements.get(1)).getMode().equals("taxi")) {
				counts.get(((Activity)planElements.get(2)).getLinkId()).increment();
				taxiLegs++;
			}
			allLegs++;
		}

		try (CompactCSVWriter writer = new CompactCSVWriter(IOUtils.getBufferedWriter(dropoffCountsFile))) {
			for (Entry<Id<Link>, MutableInt> e : counts.entrySet()) {
				writer.writeNext(e.getKey() + "", e.getValue() + "");
			}
		}

		System.out.println("#taxi legs = " + taxiLegs + "; #all legs = " + allLegs);
	}
}
