/* *********************************************************************** *
 * project: org.matsim.*
 * CorrectPlans.java
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
package playground.thibautd.agentsmating.ptv2matsim;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;

import playground.thibautd.householdsfromcensus.CliquesWriter;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithJointTripsWriterHandler;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;

/**
 * Corrects plans with an inconsistent mode chain.
 * @author thibautd
 */
public class CorrectPlans {
	private static final Logger log =
		Logger.getLogger(CorrectPlans.class);

	public static void main(final String[] args) {
		String configFile = args[0];

		CollectLogMessagesAppender appender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(appender);

		log.info("######################################################################");
		log.info("# Starting correction.");
		log.info("######################################################################");
		log.info("loading config file...");

		Controler controler = JointControlerUtils.createControler(configFile);
		Config config = controler.getConfig();

		try {
			File outputDir = new File(config.controler().getOutputDirectory());
			outputDir.mkdirs();
			IOUtils.initOutputDirLogging(
				config.controler().getOutputDirectory(),
				appender.getLogEvents());
		} catch (IOException e) {
			throw new RuntimeException("could not create log file", e);
		}

		PopulationWithCliques population = (PopulationWithCliques) controler.getPopulation();
		Correcter correcter = new Correcter(population);
		correcter.run();

		String cliqueFile = config.controler().getOutputDirectory()+"corrected_cliques.xml.gz";
		String planFile = config.controler().getOutputDirectory()+"corrected_population.xml.gz";

		log.info("writing corrected cliques to "+cliqueFile);
		(new CliquesWriter(correcter.getCliques())).writeFile(cliqueFile);

		log.info("writing corrected plans to "+planFile);
		PopulationWriter writer = (new PopulationWriter(
					population,
					controler.getScenario().getNetwork(),
					controler.getScenario().getKnowledges()));
		writer.setWriterHandler(new PopulationWithJointTripsWriterHandler(
					controler.getScenario().getNetwork(),
					controler.getScenario().getKnowledges()));
		writer.write(planFile);
		
		log.info("######################################################################");
		log.info("# success, exiting.");
		log.info("######################################################################");
		IOUtils.closeOutputDirLogging();
	}
}

