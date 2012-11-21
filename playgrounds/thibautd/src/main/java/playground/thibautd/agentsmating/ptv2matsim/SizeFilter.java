// /* *********************************************************************** *
//  * project: org.matsim.*
//  * SizeFilter.java
//  *                                                                         *
//  * *********************************************************************** *
//  *                                                                         *
//  * copyright       : (C) 2011 by the members listed in the COPYING,        *
//  *                   LICENSE and WARRANTY file.                            *
//  * email           : info at matsim dot org                                *
//  *                                                                         *
//  * *********************************************************************** *
//  *                                                                         *
//  *   This program is free software; you can redistribute it and/or modify  *
//  *   it under the terms of the GNU General Public License as published by  *
//  *   the Free Software Foundation; either version 2 of the License, or     *
//  *   (at your option) any later version.                                   *
//  *   See also COPYING, LICENSE and WARRANTY file                           *
//  *                                                                         *
//  * *********************************************************************** */
// package playground.thibautd.agentsmating.ptv2matsim;
// 
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// 
// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;
// import org.matsim.api.core.v01.Id;
// import org.matsim.api.core.v01.population.Person;
// import org.matsim.api.core.v01.population.Population;
// import org.matsim.core.config.Config;
// import org.matsim.core.population.PopulationWriter;
// 
// import playground.thibautd.householdsfromcensus.CliquesWriter;
// import playground.thibautd.cliquessim.population.Clique;
// import playground.thibautd.cliquessim.population.Cliques;
// import playground.thibautd.cliquessim.population.PopulationWithCliques;
// import playground.thibautd.cliquessim.population.PopulationWithJointTripsWriterHandler;
// import playground.thibautd.cliquessim.population.ScenarioWithCliques;
// import playground.thibautd.cliquessim.utils.JointControlerUtils;
// 
// /**
//  * Very simple utility, which creates a population without the cliques of more than 10 individuals.
//  * @author thibautd
//  */
// public class SizeFilter {
// 	private static final Log log =
// 		LogFactory.getLog(SizeFilter.class);
// 
// 	private final static int DEFAULT_MIN_SIZE = 1;
// 	private final static int DEFAULT_MAX_SIZE = 10;
// 
// 	public static void main(final String[] args) {
// 		String configFile = args[0];
// 
// 		int minSize, maxSize;
// 
// 		if (args.length >= 3) {
// 			minSize = Integer.parseInt( args[ 1 ] );
// 			maxSize = Integer.parseInt( args[ 2 ] );
// 		}
// 		else {
// 			minSize = DEFAULT_MIN_SIZE;
// 			maxSize = DEFAULT_MAX_SIZE;
// 		}
// 		
// 		Config config = JointControlerUtils.createConfig(configFile);
// 		ScenarioWithCliques scenario = JointControlerUtils.createScenario(config);
// 		Cliques populationOfCliques = scenario.getCliques();
// 		Population population = scenario.getPopulation();
// 
// 		Map<Id, ? extends Clique> cliques = populationOfCliques.getCliques();
// 		population = new PopulationWithCliques(scenario);
// 
// 		Map<Id, List<Id>> cliquesToWrite = new HashMap<Id, List<Id>>();
// 		int count = 0;
// 		for (Clique clique : cliques.values()) {
// 			int size = clique.getMembers().size();
// 			if (size <= maxSize && size >= minSize) {
// 				cliquesToWrite.put(clique.getId(), new ArrayList<Id>(clique.getMembers().keySet()));
// 
// 				for (Person person : clique.getMembers().values()) {
// 					population.addPerson(person);
// 				}
// 			}
// 			else {
// 				count++;
// 			}
// 		}
// 		log.info(count+" cliques removed");
// 
// 		String popFile = config.controler().getOutputDirectory() + "individuals-more-than"+minSize+"-less-than-"+maxSize+".xml.gz";
// 		PopulationWriter writer = (new PopulationWriter(population, scenario.getNetwork(), scenario.getKnowledges())) ;
// 		writer.setWriterHandler(new PopulationWithJointTripsWriterHandler(scenario.getNetwork(), scenario.getKnowledges()));
// 		writer.write(popFile);
// 
// 		String cliqueFile = config.controler().getOutputDirectory() + "cliques-more-than"+minSize+"-less-than-"+maxSize+".xml.gz";
// 		(new CliquesWriter(cliquesToWrite)).writeFile(cliqueFile);
// 	}
// }
// 
