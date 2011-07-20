/* *********************************************************************** *
 * project: org.matsim.*
 * Simulator.java
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.graph.social.SocialGraph;
import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.sim.analysis.ActivityDistanceTask;
import playground.johannes.socialnetworks.sim.analysis.ActivityDurationTask;
import playground.johannes.socialnetworks.sim.analysis.ActivityLoadTask;
import playground.johannes.socialnetworks.sim.analysis.ActivityStartTimeTask;
import playground.johannes.socialnetworks.sim.analysis.DepartureTimeTaks;
import playground.johannes.socialnetworks.sim.analysis.JoinTimeAnalyzerTask;
import playground.johannes.socialnetworks.sim.analysis.LegDurationTask;
import playground.johannes.socialnetworks.sim.analysis.LegLoadTask;
import playground.johannes.socialnetworks.sim.analysis.Trajectory;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzer;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.socialnetworks.sim.analysis.TrajectoryEventsBuilder;
import playground.johannes.socialnetworks.sim.analysis.VisitorAnalyzerTask;
import playground.johannes.socialnetworks.sim.interaction.JointActivityScorer;
import playground.johannes.socialnetworks.sim.interaction.JointActivityScoringFunctionFactory;
import playground.johannes.socialnetworks.sim.interaction.PseudoSim;
import playground.johannes.socialnetworks.sim.interaction.VisitorTracker;
import playground.johannes.socialnetworks.sim.locationChoice.ActivityChoiceRndAlterHome;
import playground.johannes.socialnetworks.sim.locationChoice.ActivityMover;
import playground.johannes.socialnetworks.sim.locationChoice.NegatedGibbsPlanSelector;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class Simulator {
	
	private static final Logger logger = Logger.getLogger(Simulator.class);

	private String outputRootDir;

	private Random random;

	private ScenarioImpl scenario;

	private PseudoSim pseudoSim;

	private EventsManagerImpl eventManager;

	private JointActivityScoringFunctionFactory scoringFacotry;

	private EventsToScore scorer;

	private StrategyManager strategyManager;
	
	private NegatedGibbsPlanSelector selector;

	private TravelTime travelTime;
	
	private TrajectoryAnalyzerTask analyzerTask;
	
	private TrajectoryEventsBuilder trajectoryBuilder;
	
	private Map<Plan, Trajectory> trajectories;
	
	private List<Person> personList;
	
	private Map<Person, SocialVertex> vertexMapping;
	
	private final DistanceCalculator distanceCalculator = new CartesianDistanceCalculator();

	private final int srid = 21781;

	public static void main(String args[]) {
		Config config = new Config();
		config.addCoreModules();

		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		
		int iterations = (int) Double.parseDouble(config.getParam("controler", "lastIteration"));
		Simulator sim = new Simulator(config);
		sim.run(iterations);
	}
	
	public Simulator(Config config) {
		/*
		 * create scenario
		 */
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		outputRootDir = config.getParam("controler", "outputDirectory");
		random = new Random(Long.parseLong(config.getParam("global", "randomSeed")));
//		controlerIO = new ControlerIO(output);
		/*
		 * load data
		 */
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile(config.getParam("facilities", "inputFacilitiesFile"));
		
		SocialSparseGraphMLReader reader = new SocialSparseGraphMLReader();
		SocialGraph graph = reader.readGraph(config.getParam("socialnets", "graphfile"), scenario.getPopulation());
		
		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();
		/*
		 * initialize simulation and event listeners
		 */
		pseudoSim = new PseudoSim();
//		SimulationConfigGroup simConfig = new SimulationConfigGroup();
//		simConfig.setFlowCapFactor(0.1);
//		simConfig.setStorageCapFactor(0.1);
//		simConfig.setRemoveStuckVehicles(false);
//		simConfig.setStuckTime(600);
//		simConfig.setSnapshotFormat("otfvis");
//		simConfig.setSnapshotPeriod(900);
//		config.addSimulationConfigGroup(simConfig);
//		SimulationConfigGroup simConfig = ((SimulationConfigGroup)scenario.getConfig().getModule("simulation"));
//		simConfig.setSnapshotPeriod(Double.POSITIVE_INFINITY);
//		simConfig.setSnapshotFormat("");

		eventManager = (EventsManagerImpl) EventsUtils.createEventsManager();
		
		VisitorTracker tracker = new VisitorTracker();
		eventManager.addHandler(tracker);
		
		travelTime = new TravelTimeCalculator(network, 900, 100000, new TravelTimeCalculatorConfigGroup());
		eventManager.addHandler((EventHandler) travelTime);
		/*
		 * initialize scoring
		 */
		double beta = Double.parseDouble(config.getParam("socialnets", "beta_join"));
		scoringFacotry = new JointActivityScoringFunctionFactory(tracker, graph, (PlanCalcScoreConfigGroup) config.getModule("planCalcScore"), beta);
		scorer = new EventsToScore(population, scoringFacotry);
		eventManager.addHandler(scorer);
		/*
		 * initialize strategy manager
		 */
		strategyManager = new StrategyManager();
		strategyManager.setPlanSelectorForRemoval(null);
		strategyManager.setMaxPlansPerAgent(1);
		/*
		 * initialize plan removal selector
		 */
		selector = new NegatedGibbsPlanSelector(((PlanCalcScoreConfigGroup) config.getModule("planCalcScore")).getBrainExpBeta(), random);
		/*
		 * initialize strategy
		 */
		PlanStrategy changeLocation = new PlanStrategyImpl(new KeepSelected());
		/*
		 * get desired arrival times and durations from initial plans
		 */
		Map<Person, Double> desiredArrivalTimes = new HashMap<Person, Double>();
		Map<Person, Double> desiredDurations = new HashMap<Person, Double>();
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity act = (Activity) plan.getPlanElements().get(2);
			desiredArrivalTimes.put(person, act.getStartTime());
			desiredDurations.put(person, act.getEndTime() - act.getStartTime());
		}
		
		TravelCost travelCost = new TravelCost() {
			@Override
			public double getLinkGeneralizedTravelCost(Link link, double time) {
				return travelTime.getLinkTravelTime(link, time);
			}
		};
		
		LeastCostPathCalculator router = new Dijkstra(network, travelCost, travelTime);
		ActivityMover mover = new ActivityMover(population.getFactory(), router, network);
		
		changeLocation.addStrategyModule(new ActivityChoiceRndAlterHome(graph, mover, random, desiredArrivalTimes, desiredDurations));
		
//		changeLocation.addStrategyModule(new ActivityChoiceRndAlterActivity(graph, network, mover, random, desiredArrivalTimes, desiredDurations));
//		changeLocation.addStrategyModule(new ActivityChoiceRndFacility(((ScenarioImpl)scenario).getActivityFacilities(), network, mover, random, desiredArrivalTimes, desiredDurations));

		strategyManager.addStrategy(changeLocation, 1.0);
		/*
		 * initialize analysis tools
		 */
		trajectoryBuilder = new TrajectoryEventsBuilder(population);
		eventManager.addHandler(trajectoryBuilder);
		trajectories = new HashMap<Plan, Trajectory>();
		/*
		 * initialize analyzer task
		 */
		analyzerTask = new TrajectoryAnalyzerTaskComposite();
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new DepartureTimeTaks());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new LegDurationTask());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new LegLoadTask());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new ActivityStartTimeTask());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new ActivityDurationTask());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new ActivityLoadTask());
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new ActivityDistanceTask(network, srid, distanceCalculator));
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new VisitorAnalyzerTask(scoringFacotry));
		((TrajectoryAnalyzerTaskComposite) analyzerTask).addTask(new JoinTimeAnalyzerTask(scoringFacotry));
		/*
		 * do some data caching
		 */
		personList = new ArrayList<Person>(scenario.getPopulation().getPersons().values());
		
		vertexMapping = new HashMap<Person, SocialVertex>();
		for(SocialVertex vertex : graph.getVertices()) {
			vertexMapping.put(vertex.getPerson().getPerson(), vertex);
		}
	}
	
	public void run(int iterations) {
		Population population = scenario.getPopulation();
		/*
		 * do the initial run with the full population
		 */
		eventManager.resetHandlers(0);
		scorer.reset(0);
		runMobsim(0, population);
		scorer.finish();
		collectTrajectories(population);
		synchronizePlansWithTrajectory();
		/*
		 * start iterating
		 */
		for(int iter = 1; iter < iterations; iter++) {
			if(iter % 10 == 0)
				logger.info(String.format("Simulating iteration %1$s...", iter));
			
			Logger.getRootLogger().setLevel(Level.WARN);
			/*
			 * do a single simulation step
			 */
			step(iter);
			
			Logger.getRootLogger().setLevel(Level.ALL);
			if(iter % 100 == 0) {
				/*
				 * simulate with the full population and analyze the sample
				 */
				logger.info("Drawing sample...");
				drawSample(population, iter);
				analyze(population, iter);
			}
			
		}
	}

	private void step(int it) {
		/*
		 * create temporarily population
		 */
		Population tmpPop = new PopulationImpl(scenario);

		Set<Person> tmpPersons = new HashSet<Person>();
		Set<Person> tmpAlters = new HashSet<Person>();
		Set<Person> tmpEgos = new HashSet<Person>();
		/*
		 * draw egos that are independently, i.e. do not share any alter
		 */
		while (tmpEgos.size() < 100) {
			Person ego = personList.get(random.nextInt(personList.size()));
			if (!tmpPersons.contains(ego)) {
				boolean valid = false;

				Set<Person> alters = new HashSet<Person>();
				SocialVertex v = vertexMapping.get(ego);
				/*
				 * check if the alter is not already in temporarily population
				 * or an alter of an already drawn ego
				 */
				for (SocialVertex neighbor : v.getNeighbours()) {
					Person alterPerson = neighbor.getPerson().getPerson();
					if (tmpPersons.contains(alterPerson)) {
						valid = false;
						break;
					} else {
						valid = true;
						alters.add(alterPerson);
					}
				}
				/*
				 * ego and alters are valid
				 */
				if (valid) {
					tmpPersons.add(ego);
					tmpPersons.addAll(alters);
					tmpAlters.addAll(alters);
					tmpEgos.add(ego);
				}
			}
		}
		/*
		 * add egos to temporarily population
		 */
		for (Person person : tmpEgos)
			tmpPop.addPerson(person);
		/*
		 * to properly determine the scores the scoring factory needs to know
		 * the egos
		 */
		scoringFacotry.setCurrentEgos(tmpEgos);
		/*
		 * run the strategy manager with the temporarily population
		 */
		strategyManager.run(tmpPop);
		/*
		 * after strategy manager is run add also alters to temporarily
		 * population
		 */
		for (Person person : tmpAlters)
			tmpPop.addPerson(person);
		/*
		 * run simulation with temporarily population
		 */
		eventManager.resetHandlers(it);
		scorer.reset(it);
		
		runMobsim(it, tmpPop);

		scorer.finish();

		collectTrajectories(tmpPop);
		synchronizePlansWithTrajectory(); // not sure if we still need this
		/*
		 * Do the actual plan sampling
		 */
		for (Person ego : tmpEgos) {
			Plan remove = selector.selectPlan(ego);
			if (!ego.getPlans().remove(remove)) {
				throw new RuntimeException("Failed to remove plan!");
			}
			((PersonImpl) ego).setSelectedPlan(ego.getPlans().get(0));
		}
		/*
		 * delete also trajectories of deleted plans
		 */
		synchronizeTrajectories();
	}

	private void runMobsim(int iter, Population tmpPop) {
		pseudoSim.run(tmpPop, scenario.getNetwork(), travelTime, eventManager);
		// QueueSimulation queueSim = (QueueSimulation)
		// queueSimFactory.createMobsim(scenario, eventManager);
		// queueSim.setControlerIO(controlerIO);
		// new File(controlerIO.getIterationPath(it)).mkdirs();
		// queueSim.setIterationNumber(it);
		// queueSim.run();
	}

	private void drawSample(Population population, int iter) {
		/*
		 * passing null is equivalent to passing the entire population
		 */
		scoringFacotry.setCurrentEgos(null);
		
		eventManager.resetHandlers(iter);
		scorer.reset(iter);
		JointActivityScorer.totalJoinTime = 0;
		JointActivityScorer.jointAgents = 0;

		runMobsim(iter, population);

		scorer.finish();

		double score_selected = 0;
		int cnt_selected = 0;
		for (Person person : population.getPersons().values()) {
			score_selected += person.getSelectedPlan().getScore();
			cnt_selected++;
		}

		logger.info(String.format("Average selected plan score = %1$s.", score_selected / (double) cnt_selected));
		logger.info(String.format("Total joint acitivty time %1$s. Joint agents %2$s.",
				JointActivityScorer.totalJoinTime, JointActivityScorer.jointAgents));

		selector.notifyIterationEnds(null);
		selector.notifyIterationStarts(null);

		// SimulationConfigGroup simConfig =
		// ((SimulationConfigGroup)scenario.getConfig().getModule("simulation"));
		// simConfig.setSnapshotFormat("otfvis");
		// QueueSimulation queueSim = (QueueSimulation)
		// queueSimFactory.createMobsim(scenario,
		// EventsUtils.createEventsManager());
		// queueSim.setControlerIO(controlerIO);
		// new File(controlerIO.getIterationPath(it)).mkdirs();
		// queueSim.setIterationNumber(it);
		// queueSim.run();
		// simConfig.setSnapshotFormat("");
	}

	private void analyze(Population population, int iter) {
		logger.info("Analyzing...");
		/*
		 * write plans file
		 */
		PopulationWriter writer = new PopulationWriter(population, scenario.getNetwork());
		writer.useCompression(true);
		writer.write(String.format("%1$s/%2$s.plans.xml.gz", outputRootDir, iter));
		/*
		 * run analyzers
		 */
		String itertOutputDir = String.format("%1$s/analysis/%2$s/", outputRootDir, iter);
		new File(itertOutputDir).mkdirs();
		try {
			TrajectoryAnalyzer.analyze(new HashSet<Trajectory>(trajectories.values()), analyzerTask, itertOutputDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void collectTrajectories(Population population) {
		for(Person person : population.getPersons().values()) {
			Plan selected = person.getSelectedPlan();
			Trajectory t  = trajectoryBuilder.getTrajectories().get(person.getId());
			if(t == null)
				throw new RuntimeException(String.format("No trajectory for person %1$s.", person.getId()));
			
			trajectories.put(selected, t);
		}
	}
	
	private void synchronizeTrajectories() {
		Map<Plan, Trajectory> newTrajecotries = new HashMap<Plan, Trajectory>();
		
		for(Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Trajectory t = trajectories.get(plan);
			if(t == null)
				throw new RuntimeException("No trajecotry for plan!");
					
			newTrajecotries.put(plan, t);
		}
		
		trajectories = newTrajecotries;
	}
	
	private void synchronizePlansWithTrajectory() {
		for(Trajectory t : trajectories.values()) {
			for(int i = 0; i < t.getElements().size(); i++) {
				if(i % 2 == 0) {
					Activity act = (Activity) t.getElements().get(i);
					act.setStartTime(t.getTransitions().get(i));
					act.setEndTime(t.getTransitions().get(i + 1));
				} else {
					Leg leg = (Leg) t.getElements().get(i);
					leg.setDepartureTime(t.getTransitions().get(i));
					leg.setTravelTime(t.getTransitions().get(i + 1) - t.getTransitions().get(i));
				}
			}
		}
	}
}
