/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
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
package playground.johannes.socialnetworks.sim.interaction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.contrib.sna.graph.generators.ErdosRenyiGenerator;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialPerson;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * @author illenberger
 *
 */
public class Controller {
	
	private static final Logger logger = Logger.getLogger(Controller.class);

	private Population population;
	
	private List<Person> persons;
	
	private Random random;
	
	private EndTimeMutator mutator;
	
	private PseudoSim sim;
	
	private Network network;
	
	private TravelTime travelTime;
	
	private EventsManagerImpl eventManager;
	
	private EventsToScore scorer;
	
	private double oldScore;
	
	private int accepts;
	
	public Controller(Population population, Network network, SocialGraph graph) {
		random = new Random();
		
		this.population = population;
		this.network = network;
	
		persons = new ArrayList<Person>(population.getPersons().values());
		
		eventManager = new EventsManagerImpl();
		VisitorTracker tracker = new VisitorTracker();
		eventManager.addHandler(tracker);
		
		scorer = new EventsToScore(population, new JointActivityScoringFunctionFactory(tracker, graph));
		eventManager.addHandler(scorer);
		
		travelTime = new TravelTimeCalculator(network, 3600, 86400, new TravelTimeCalculatorConfigGroup());
		
		sim = new PseudoSim();
		
		mutator = new EndTimeMutator();
	}
	
	public void run(int iterations) {
		sim.run(population, network, travelTime, eventManager);
		scorer.finish();
		oldScore = scorer.getAveragePlanPerformance();
		
		accepts = 0;
		for(int i = 0; i < iterations; i++) {
			logger.info("Running iteration " + i);
			step();
		}
		logger.info(String.format("Accepted %1$s steps.", accepts));
	}
	
	public void step() {
		Logger.getRootLogger().setLevel(Level.WARN);
		/*
		 * randomly select one plan
		 */
		Person person = persons.get(random.nextInt(persons.size()));
		PlanImpl plan = (PlanImpl) person.getSelectedPlan();
		PlanImpl copy = new PlanImpl(plan.getPerson());
		copy.copyPlan(plan);
		person.addPlan(copy);
		copy.setSelected(true);
		plan.setSelected(false);
		/*
		 * mutate
		 */
		mutator.mutatePlan(copy, "leisure");
		/*
		 * simulate
		 */
		sim.run(population, network, travelTime, eventManager);
		/*
		 * evaluate
		 */
		scorer.finish();
		double newscore = scorer.getAveragePlanPerformance();
		double delta = oldScore - newscore;
		/*
		 * select
		 */
		Logger.getRootLogger().setLevel(Level.DEBUG);
		double p = 1/(1 + Math.exp(delta));
		if(random.nextDouble() < p) {
			/*
			 * accept
			 */
			person.getPlans().remove(plan);
			copy.setSelected(true);
			
			oldScore = newscore;
			
			accepts++;
			logger.info("Accepted new plan.");
		} else {
			/*
			 * reject
			 */
			person.getPlans().remove(copy);
			plan.setSelected(true);
			logger.info("Rejected new plan.");
		}
	}
	
	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		String netFile = args[0];//"/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml";
		String facFile = args[1];//"/Users/jillenberger/Work/work/socialnets/sim/facilities.xml";
		String popFile = args[2];//"/Users/jillenberger/Work/work/socialnets/sim/plans.0.2.xml";
		int iters = Integer.parseInt(args[4]);
		Scenario scenario = new ScenarioImpl();
		
		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse(netFile);

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);

		logger.info("Building random graph...");
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(21781));
		SocialSparseGraph graph = builder.createGraph();
		GeometryFactory geoFactory = new GeometryFactory();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			SocialPerson sp = new SocialPerson((PersonImpl) person);
			Coord home = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			builder.addVertex(graph, sp, geoFactory.createPoint(new Coordinate(home.getX(), home.getY())));
		}
		
		ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
		generator.setRandomDrawMode(true);
		generator.generate(graph, 0.0004, 0);
		
		double k_mean = new Degree().distribution(graph.getVertices()).mean();
		logger.info("k_mean = " + k_mean);
		
		Controller controller = new Controller(scenario.getPopulation(), scenario.getNetwork(), graph);
		controller.run(iters);
		
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(args[3]);
	}
}
