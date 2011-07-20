/* *********************************************************************** *
 * project: org.matsim.*
 * RandomGraph.java
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
package playground.johannes.studies.netgeneration;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.johannes.socialnetworks.graph.generators.RandomGraphGenerator;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.social.io.SocialSparseVertexPool;
import playground.johannes.socialnetworks.statistics.LogNormalDistribution;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphFactory;

/**
 * @author illenberger
 *
 */
public class RandomGraph {

	/**
	 * @throws IOException 
	 * 
	 */
	public static void main(String args[]) throws IOException {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile("/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/locationChoice/data/plans.home.0.01.xml");
		Population population = scenario.getPopulation();
		
		Set<Person> persons = new HashSet<Person>(population.getPersons().values());
		SocialSparseGraphFactory factory = new SocialSparseVertexPool(persons, CRSUtils.getCRS(21781));
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(factory);
		
		
		long randomSeed = 815;
		
		double sigma = 1;
		double mu = 2.5;
		int k_max = 41;
		UnivariateRealFunction function = new LogNormalDistribution(sigma, mu, 1);
		RandomGraphGenerator generator = new RandomGraphGenerator(function, builder, randomSeed);
		SocialSparseGraph graph = (SocialSparseGraph) generator.generate(persons.size(), k_max);
		
		SocialGraphMLWriter writer = new SocialGraphMLWriter();
		writer.setPopulationFile("plans.home.0.01.xml");
		writer.write(graph, "/Volumes/cluster.math.tu-berlin.de/net/ils2/jillenberger/locationChoice/data/graph.rnd.graphml");
	}

}
