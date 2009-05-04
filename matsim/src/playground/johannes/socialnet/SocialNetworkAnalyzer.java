/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnet;

import java.io.FileNotFoundException;
import java.io.IOException;

import gnu.trove.TDoubleDoubleHashMap;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;

import playground.johannes.socialnet.io.SNGraphMLReader;
import playground.johannes.statistics.WeightedStatistics;

/**
 * @author illenberger
 *
 */
public class SocialNetworkAnalyzer {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = Gbl.createConfig(new String[]{args[0]});
		ScenarioImpl data = new ScenarioImpl(config);
		Population population = data.getPopulation();
		SNGraphMLReader<Person> reader = new SNGraphMLReader<Person>(population);
		SocialNetwork<Person> socialnet = reader.readGraph(args[1]);
		
		String outputDir = args[2];
		
		WeightedStatistics.writeHistogram(SocialNetworkStatistics.getEdgeLengthDegreeCorrelation(socialnet), args[2]+"/d_k.hist.txt");

	}

}
