/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationFilter.java
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
package playground.johannes.socialnetworks.utils;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.SAXException;

/**
 * @author illenberger
 *
 */
public class PopulationFilter {

	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(args[0]);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();
		
//		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
//		netReader.parse(args[0]);
		
//		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
//		reader.readFile(args[1]);
		
		double f = Double.parseDouble(args[1]);
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork(), f).write(scenario.getConfig().getParam("plans", "outputPlansFile"));
	}
}
