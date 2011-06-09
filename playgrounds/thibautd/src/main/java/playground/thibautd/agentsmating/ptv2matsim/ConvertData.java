/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertData.java
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

import java.io.BufferedReader;
import java.io.FileReader;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * Runs the ptv2matsim converter.
 *
 * @author thibautd
 */
public class ConvertData {
	private static final Logger log =
		Logger.getLogger(ConvertData.class);
	private static final double sampleRate = 0.1d;

	/**
	 * Usage: ConvertData ptvFile configFile
	 */
	public static void main(String[] args) {
		String ptvFile = args[0];
		String configFile = args[1];

		log.info("loading config file...");
		Config config = ConfigUtils.loadConfig(configFile);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);

		log.info("creating converter...");
		Converter ptv2matsim;
		try {
			ptv2matsim = new Converter(
					new BufferedReader(new FileReader(ptvFile)),
					scenario);
		} catch (Exception e) {
			throw new RuntimeException("exception while creating converter, "+
					"aborting...", e);
		}

		log.info("writing a "+sampleRate+" sample to file...");
		ptv2matsim.write(config.controler().getOutputDirectory(), sampleRate);
		
		log.info("success, exiting.");
	}
}

