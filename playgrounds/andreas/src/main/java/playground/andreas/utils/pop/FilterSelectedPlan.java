/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.utils.pop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.population.algorithms.PersonFilterSelectedPlan;

/**
 * Filter selected plans
 *
 * @author aneumann
 *
 */
public class FilterSelectedPlan {
	
	private static final Logger log = Logger.getLogger(FilterSelectedPlan.class);

	public static void filterSelectedPlans(final String[] args) {
		
		log.info("Starting...");
		
		String networkFile = args[0];//"F:/server/run771/network_modified_20100806_added_BBI_AS_cl.xml.gz";
		String inPlansFile = args[1];//"F:/server/run771/output/it.1000/1000.plans.xml.gz";
		String outPlansFile = args[2];//"F:/server/run771/output/it.1000/1000.plans_selected.xml.gz";

		ScenarioImpl sc = new ScenarioImpl();
		
		log.info("Reading network from " + networkFile);
		new MatsimNetworkReader(sc).readFile(networkFile);

		final PopulationImpl plans = (PopulationImpl) sc.getPopulation();
		plans.setIsStreaming(true);
		plans.addAlgorithm(new PersonFilterSelectedPlan());
		
		final PopulationWriter plansWriter = new PopulationWriter(plans, sc.getNetwork());
		plansWriter.startStreaming(outPlansFile);
		plans.addAlgorithm(plansWriter);
		PopulationReader plansReader = new MatsimPopulationReader(sc);		

		log.info("Reading plans file from " + inPlansFile);
		plansReader.readFile(inPlansFile);
		plans.printPlansCount();
		plansWriter.closeStreaming();
		log.info("Plans written to " + outPlansFile);
		log.info("Finished");
	}	

	public static void main(final String[] args) {
		FilterSelectedPlan.filterSelectedPlans(args);
	}
}
