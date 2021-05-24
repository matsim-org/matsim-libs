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

package playground.vsp.andreas.utils.pop;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingDeprecated;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.vsp.andreas.bvgAna.mrieser.PersonFilterSelectedPlan;

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

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		log.info("Reading network from " + networkFile);
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		final Population plans = (Population) sc.getPopulation();
		StreamingDeprecated.setIsStreaming(plans, true);
		StreamingDeprecated.addAlgorithm(plans, new PersonFilterSelectedPlan());
		
		final StreamingPopulationWriter plansWriter = new StreamingPopulationWriter();
		plansWriter.startStreaming(outPlansFile);
		StreamingDeprecated.addAlgorithm(plans, plansWriter);
		MatsimReader plansReader = new PopulationReader(sc);		

		log.info("Reading plans file from " + inPlansFile);
		plansReader.readFile(inPlansFile);
		PopulationUtils.printPlansCount(plans) ;
		plansWriter.closeStreaming();
		log.info("Plans written to " + outPlansFile);
		log.info("Finished");
	}	

	public static void main(final String[] args) {
		FilterSelectedPlan.filterSelectedPlans(args);
	}
}
