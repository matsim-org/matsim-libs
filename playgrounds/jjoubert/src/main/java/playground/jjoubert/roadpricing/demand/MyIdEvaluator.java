/* *********************************************************************** *
 * project: org.matsim.*
 * MyIdEvaluator.java
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

package playground.jjoubert.roadpricing.demand;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;

public class MyIdEvaluator {
	private final static Logger log = Logger.getLogger(MyIdEvaluator.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Getting the agent IDs from " + args[0]);
		
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader pr = new MatsimPopulationReader(sc);
		try {
			pr.parse(args[0]);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Integer minId = Integer.MAX_VALUE;
		Integer maxId = Integer.MIN_VALUE;
		for(Id id : sc.getPopulation().getPersons().keySet()){
			Integer i = Integer.parseInt(id.toString());
			minId = Math.min(minId, i);
			maxId = Math.max(maxId, i);
		}
		
		log.info("Minimum Id: " + minId);
		log.info("Maximum Id: " + maxId);
		log.info("----------------------------------");
		log.info("           Completed");
		log.info("==================================");

		
	}

}

