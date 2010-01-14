/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationSplitter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.christoph.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

/*
 * Creates Knowledge Entries for multiple CostFactors in a single Iteration,
 * what provides a major speed up. The Dijkstra Algorithm has to be run only once
 * per route instead of once per combination of route and costfactor.
 */
public class PopulationSplitter {

	private final static Logger log = Logger.getLogger(PopulationSplitter.class);

	private ScenarioImpl scenario;
	private NetworkLayer network;
	private ActivityFacilitiesImpl facilities;
	private PopulationImpl population;

	private int personsPerSplitFile = 65536;
	private int fileCounter = 0;

//	private String outputFileName = "mysimulations/kt-zurich/output/plans_split_";
//	private String networkFile = "mysimulations/kt-zurich/input/network.xml";
//	private String populationFile = "mysimulations/kt-zurich/input/plans.xml";
//	private String facilitiesFile = null;

	private String outputFileName = "mysimulations/switzerland/output/plans_split_";
	private String networkFile = "mysimulations/switzerland/input/network.xml";
	private String populationFile = "mysimulations/switzerland/input/plans.xml.gz";
	private String facilitiesFile = "mysimulations/switzerland/input/facilities.xml.gz";

//	private String outputFileName = "mysimulations/kt-zurich-cut/output/plans_split_";
//	private String networkFile = "mysimulations/kt-zurich-cut/mapped_network.xml.gz";
//	private String populationFile = "mysimulations/kt-zurich-cut/plans_10.xml.gz";
//	private String facilitiesFile = "mysimulations/kt-zurich-cut/facilities.zrhCutC.xml.gz";

	public static void main(String[] args)
	{
		new PopulationSplitter();
	}

	public PopulationSplitter()
	{
		this.scenario = new ScenarioImpl();
		loadNetwork();
		if (facilitiesFile != null) loadFacilities();
		loadPopulation();

		log.info("Network size: " + network.getLinks().size());
		log.info("Population size: " + population.getPersons().size());

		Population pop = new ScenarioImpl().getPopulation();

		int i = 0;
		for (Person p : this.population.getPersons().values())
		{
			pop.addPerson(p);
			i++;
			if (i == personsPerSplitFile)
			{
				i = 0;
				writePopulation(pop, scenario.getNetwork());
				pop.getPersons().clear();
			}
		}
		writePopulation(pop, scenario.getNetwork());
		pop.getPersons().clear();

		System.out.println(pop.getPersons().size());
	}

	private void loadNetwork()
	{
		network = scenario.getNetwork();

		new MatsimNetworkReader(network).readFile(networkFile);

		log.info("Loading Network ... done");
	}

	private void loadFacilities()
	{
		facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(facilities).readFile(facilitiesFile);

		log.info("Loading Facilities ... done");
	}

	private void loadPopulation()
	{
		new MatsimPopulationReader(scenario).readFile(populationFile);
		log.info("Loading Population ... done");
	}

	private void writePopulation(Population pop, Network net)
	{
		fileCounter++;

		log.info("Writing split File.");
		new PopulationWriter(pop, net).writeFile(outputFileName + fileCounter + ".xml.gz");
	}
}
