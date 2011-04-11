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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.sna.graph.analysis.Degree;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

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

//	private double oldScore;

	private int accepts;

//	private SocialGraph graph;

	private Map<Person, SocialVertex> vertexMapping;

	public Controller(Population population, Network network, SocialGraph graph) {
//		this.graph = graph;
		vertexMapping = new HashMap<Person, SocialVertex>();
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}

		random = new Random();

		this.population = population;
		this.network = network;

		persons = new ArrayList<Person>(population.getPersons().values());

		eventManager = (EventsManagerImpl) EventsUtils.createEventsManager();
		VisitorTracker tracker = new VisitorTracker();
		eventManager.addHandler(tracker);

		scorer = new EventsToScore(population, new JointActivityScoringFunctionFactory(tracker, graph));
		eventManager.addHandler(scorer);

		travelTime = new TravelTimeCalculator(network, 3600, 86400, new TravelTimeCalculatorConfigGroup());

		sim = new PseudoSim();

		mutator = new EndTimeMutator();
	}

	public void run(int iterations, String outputDir) {
		PopulationWriter writer = new PopulationWriter(population, network);

		JointActivityScorer.totalJoinTime = 0;
		scorer.reset(0);
		sim.run(population, network, travelTime, eventManager);
		scorer.finish();
		double initialJoinTime = JointActivityScorer.totalJoinTime;

		accepts = 0;
		for(int i = 0; i < iterations+1; i++) {
			if(i % 10000 == 0)
				logger.info(String.format("Running iteration %1$s, accepted %2$s steps.", i, accepts));

			step();

			if(i % 100000 == 0 && i > 0) {
				logger.info("Dumping plans...");
				writer.write(String.format("%1$s/%2$s.plan.xml", outputDir, i));

				JointActivityScorer.totalJoinTime = 0;
				scorer.reset(0);
				sim.run(population, network, travelTime, eventManager);
				scorer.finish();

				double deltaJoinTime = initialJoinTime - JointActivityScorer.totalJoinTime;
				logger.info(String.format("Delta join time: total = %1$s, per person = %2$s", deltaJoinTime, Time.writeTime(deltaJoinTime/population.getPersons().size())));
			}

		}
		logger.info(String.format("Accepted %1$s steps.", accepts));
	}

	public void step() {


		Logger.getRootLogger().setLevel(Level.WARN);
		/*
		 * randomly select one plan
		 */
		boolean mutate = false;
		PlanImpl oldPlan = null;
		PlanImpl newPlan = null;;
		Person person = null;
		while (!mutate) {
			person = persons.get(random.nextInt(persons.size()));
			oldPlan = (PlanImpl) person.getSelectedPlan();
			newPlan = new PlanImpl(oldPlan.getPerson());
			newPlan.copyPlan(oldPlan);
			/*
			 * mutate
			 */
			mutate = mutator.mutatePlan(newPlan);
		}
		/*
		 * get affected persons, i.e. friends
		 */
		Population tmpPop = affectedPersons(person);
		/*
		 * simulate old state
		 */
		JointActivityScorer.totalJoinTime = 0;
		scorer.reset(0);
		sim.run(tmpPop, network, travelTime, eventManager);
		scorer.finish();
		double oldScore = avrScore(tmpPop);
		double oldJoinTime = JointActivityScorer.totalJoinTime;
		/*
		 * add new plan and simulate new state
		 */
		person.addPlan(newPlan);
		((PersonImpl) person).setSelectedPlan(newPlan);
		JointActivityScorer.totalJoinTime = 0;
		scorer.reset(0);
		sim.run(tmpPop, network, travelTime, eventManager);
		scorer.finish();
		double newscore = avrScore(tmpPop);
		double deltaJoinTime = oldJoinTime - JointActivityScorer.totalJoinTime;

		double delta = oldScore - newscore;
		/*
		 * select
		 */
		Logger.getRootLogger().setLevel(Level.DEBUG);
//		logger.info(String.format("Affected persons: %1$s.", tmpPop.getPersons().size()));
//		logger.info(String.format("Score of old plan: %1$s, score of new plan: %2$s.", oldPlan.getScore(), newPlan.getScore()));
//		logger.info(String.format("New score: %1$s, old score: %2$s, score diff = %3$s.", newscore, oldScore, delta));
//		logger.info(String.format("Delta join time: %1$s", deltaJoinTime));

		double p = 1/(1 + Math.exp(100*delta));
		if(random.nextDouble() < p) {
			/*
			 * accept
			 */
			person.getPlans().remove(oldPlan);
			((PersonImpl) person).setSelectedPlan(newPlan);

//			oldScore = newscore;

			accepts++;
//			logger.info("Accepted new plan.");
		} else {
			/*
			 * reject
			 */
			person.getPlans().remove(newPlan);
			((PersonImpl) person).setSelectedPlan(oldPlan);
//			logger.info("Rejected new plan.");
		}
	}

	private Population affectedPersons(Person person) {
		Population p = new PopulationImpl(null);
		p.addPerson(person);
		SocialVertex v = vertexMapping.get(person);
		for(SocialVertex neighbor : v.getNeighbours()) {
			p.addPerson(neighbor.getPerson().getPerson());
		}
		return p;
	}

	private double avrScore(Population pop) {
		double sum = 0;
		for(Person p : pop.getPersons().values()) {
			sum += p.getSelectedPlan().getScore();
		}
		return sum/pop.getPersons().size();
	}

	public static void main(String args[]) throws SAXException, ParserConfigurationException, IOException {
		String netFile = args[0];//"/Users/jillenberger/Work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch.xml";
		String facFile = args[1];//"/Users/jillenberger/Work/work/socialnets/sim/facilities.xml";
		String popFile = args[2];//"/Users/jillenberger/Work/work/socialnets/sim/plans.0.2.xml";
		String outputDir = args[3];
		String graphFile = args[4];
		int iters = Integer.parseInt(args[5]);

		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkReaderMatsimV1 netReader = new NetworkReaderMatsimV1(scenario);
		netReader.parse(netFile);

		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1((ScenarioImpl) scenario);
		facReader.parse(facFile);

		PopulationReaderMatsimV4 reader = new PopulationReaderMatsimV4(scenario);
		reader.readFile(popFile);

//		logger.info("Building random graph...");
//		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(CRSUtils.getCRS(21781));
//		SocialSparseGraph graph = builder.createGraph();
//		GeometryFactory geoFactory = new GeometryFactory();
//		for(Person person : scenario.getPopulation().getPersons().values()) {
//			SocialPerson sp = new SocialPerson((PersonImpl) person);
//			Coord home = ((Activity) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
//			builder.addVertex(graph, sp, geoFactory.createPoint(new Coordinate(home.getX(), home.getY())));
//		}

//		ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> generator = new ErdosRenyiGenerator<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(builder);
//		generator.setRandomDrawMode(true);
//		generator.generate(graph, 0.0004, 0);

		SocialSparseGraphMLReader graphReader = new SocialSparseGraphMLReader();
		SocialSparseGraph graph = graphReader.readGraph(graphFile, scenario.getPopulation());

		double k_mean = Degree.getInstance().distribution(graph.getVertices()).getMean();

		logger.info("k_mean = " + k_mean);

		Controller controller = new Controller(scenario.getPopulation(), scenario.getNetwork(), graph);
		controller.run(iters, outputDir);

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(String.format("%1$s/%2$s.plans.xml", outputDir, "final"));
	}
}
