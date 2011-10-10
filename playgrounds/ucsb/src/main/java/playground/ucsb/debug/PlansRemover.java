/* *********************************************************************** *
 * project: org.matsim.*
 * PlansRemover.java
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

package playground.ucsb.debug;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.ucsb.UCSBUtils;

public class PlansRemover {

	private final static Logger log = Logger.getLogger(PlansRemover.class);

	public static void main(String[] args) throws FileNotFoundException, IOException {

		args = new String[] {
				"D:/balmermi/documents/eclipse/output/ucsb/debug/plans.xml.gz",
				"D:/balmermi/documents/eclipse/output/ucsb/debug/network.xml.gz",
				"D:/balmermi/documents/eclipse/output/ucsb/debug/pids.txt",
				"D:/balmermi/documents/eclipse/output/ucsb/debug"
		};

		if (args.length != 4) {
			log.error("PlansRemover inputPlansfile inputNetworkfile inputPidFile outputBase");
			System.exit(-1);
		}
		String inputPlansfile = args[0];
		String inputNetworkfile = args[1];
		String inputPidFile = args[2];
		String outputBase = args[3];

		log.info("inputPlansfile: "+inputPlansfile);
		log.info("inputNetworkfile: "+inputNetworkfile);
		log.info("inputPidFile: "+inputPidFile);
		log.info("outputBase: "+outputBase);

		Set<Id> pids = UCSBUtils.parseObjectIds(inputPidFile);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(scenario).parse(inputNetworkfile);

		PopulationImpl pop = (PopulationImpl)scenario.getPopulation();
		pop.setIsStreaming(true);
		pop.addAlgorithm(new PersonRemovePlans(pids));
		PopulationWriter writer = new PopulationWriter(pop,scenario.getNetwork());
		writer.startStreaming(outputBase+"/plans.tmp.xml.gz");
		pop.addAlgorithm(writer);
		new MatsimPopulationReader(scenario).readFile(inputPlansfile);
		pop.printPlansCount();
		writer.closeStreaming();
		
		pop.getPersons().clear();
		pop.setIsStreaming(false);
		new MatsimPopulationReader(scenario).readFile(outputBase+"/plans.tmp.xml.gz");
		Set<Id> pidsToRemove = new HashSet<Id>();
		for (Person p : pop.getPersons().values()) {
			if (p.getPlans().isEmpty()) { pidsToRemove.add(p.getId()); }
		}
		for (Id pid : pidsToRemove) {
			pop.getPersons().remove(pid);
		}
		new PopulationWriter(pop,scenario.getNetwork()).write(outputBase+"/plans.debug.xml.gz");
	}
}
