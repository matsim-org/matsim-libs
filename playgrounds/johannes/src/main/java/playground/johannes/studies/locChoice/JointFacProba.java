/* *********************************************************************** *
 * project: org.matsim.*
 * JointFacProba.java
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
package playground.johannes.studies.locChoice;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TDoubleIntHashMap;
import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.math.Discretizer;
import org.matsim.contrib.sna.math.LinearDiscretizer;
import org.matsim.contrib.sna.util.ProgressLogger;
import org.matsim.contrib.sna.util.TXTWriter;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.social.SocialEdge;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;



/**
 * @author illenberger
 *
 */
public class JointFacProba {

	private static final Logger logger = Logger.getLogger(JointFacProba.class);
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = new Config();
		config.addCoreModules();

		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(config.getParam("facilities", "inputFacilitiesFile"));
		
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph(config.getParam("socialnets", "graphfile"), scenario.getPopulation());
		
		Network network = scenario.getNetwork();
		Population pop = scenario.getPopulation();
		ActivityFacilities facilities = scenario.getActivityFacilities();

		Map<Person, TObjectDoubleHashMap<ActivityFacility>> probas = new HashMap<Person, TObjectDoubleHashMap<ActivityFacility>>();
		
		logger.info("Filtering facilities...");
		
		Set<ActivityFacility> lFacilities = new HashSet<ActivityFacility>();
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			boolean isLeisure = false;
			for(ActivityOption option : facility.getActivityOptions().values()) {
				if(option.getType().equalsIgnoreCase("leisure")) {
					isLeisure = true;
					break;
				}
			}
			
			if(isLeisure) {
				if(Math.random() < 0.2)
					lFacilities.add(facility);
			}
		}
		
//		SocialVertex ego = graph.getVertices().iterator().next();
//		ego = graph.getVertices().iterator().next();
//		SocialVertex alter = ego.getNeighbours().get(1);
//		
//		Set<Person> persons = new HashSet<Person>();
//		persons.add(ego.getPerson().getPerson());
//		persons.add(alter.getPerson().getPerson());
		
		logger.info("Calculating probas...");
		
		ProgressLogger.init(pop.getPersons().size(), 1, 5);
		for (Person person : pop.getPersons().values()) {
			if (Math.random() < 0.2) {
				TObjectDoubleHashMap<ActivityFacility> proba = new TObjectDoubleHashMap<ActivityFacility>();
				Link source = network.getLinks().get(
						((Activity) person.getSelectedPlan().getPlanElements().get(0)).getLinkId());
				Coord c1 = source.getCoord();

				for (ActivityFacility facility : lFacilities) {
					Coord c2 = facility.getCoord();
					double d = CoordUtils.calcDistance(c1, c2);
					proba.put(facility, Math.pow(d, -1));
				}
				probas.put(person, proba);

			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		
		TDoubleDoubleHashMap hist = new TDoubleDoubleHashMap();
		TDoubleIntHashMap counts = new TDoubleIntHashMap();
		Discretizer disc = new LinearDiscretizer(1000);
		
		logger.info("Calculating joint probas...");
		
		ProgressLogger.init(graph.getEdges().size(), 1, 5);
		
		for(SocialEdge edge : graph.getEdges()) {
			SocialVertex ego = edge.getVertices().getFirst();
			SocialVertex alter = edge.getVertices().getSecond();
			
			TObjectDoubleHashMap<ActivityFacility> p1 = probas.get(ego.getPerson().getPerson());
			TObjectDoubleHashMap<ActivityFacility> p2 = probas.get(alter.getPerson().getPerson());

			if (p1 != null && p2 != null) {
				Link source = network.getLinks()
						.get(((Activity) ego.getPerson().getPerson().getSelectedPlan().getPlanElements().get(0))
								.getLinkId());
				Coord c1 = source.getCoord();
				for (ActivityFacility fac : lFacilities) {
					double p_join = p1.get(fac) * p2.get(fac);
					Coord c2 = fac.getCoord();
					double d = disc.discretize(CoordUtils.calcDistance(c1, c2));

					hist.adjustOrPutValue(d, p_join, p_join);
					counts.adjustOrPutValue(d, 1, 1);
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.termiante();
		
		logger.info("Averaging...");
		TDoubleDoubleIterator it = hist.iterator();
		for(int i = 0; i < hist.size(); i++) {
			it.advance();
			double d = it.key();
			double p = it.value();
			int count = counts.get(d);
			it.setValue(p/(double)count);
		}
		
		TXTWriter.writeMap(hist, "d", "p", config.getParam("controler", "outputDirectory") + "/p_join.txt");
		
		logger.info("Done.");
	}

}
