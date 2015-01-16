/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.johannes.gsv.sim;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManagerModule;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioElementsModule;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;

import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripCountTask;
import playground.johannes.coopsim.analysis.TripGeoDistanceTask;
import playground.johannes.coopsim.analysis.TripDurationTask;
import playground.johannes.coopsim.analysis.TripPurposeShareTask;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.analysis.CountsCompareAnalyzer;
import playground.johannes.gsv.analysis.PkmTask;
import playground.johannes.gsv.analysis.ScoreTask;
import playground.johannes.gsv.analysis.SpeedFactorTask;
import playground.johannes.gsv.sim.cadyts.CadytsContext;
import playground.johannes.gsv.sim.cadyts.CadytsScoring;
import playground.johannes.socialnetworks.utils.XORShiftRandom;

/**
 * @author johannes
 * 
 */
public class Simulator {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Controler controler = new Controler(args);
		controler.setOverwriteFiles(true);
		controler.setDumpDataAtEnd(false);
		controler.setMobsimFactory(new MobsimConnectorFactory());
		controler.addControlerListener(new ControllerSetup());
		/*
		 * setup mutation module
		 */
		Random random = new XORShiftRandom(controler.getConfig().global().getRandomSeed());

		StrategySettings settings = new StrategySettings(Id.create(1, StrategySettings.class));
		settings.setStrategyName("activityLocations");
		int numThreads = controler.getConfig().global().getNumberOfThreads();
		double mutationError = Double.parseDouble(controler.getConfig().getParam("gsv", "mutationError"));
		controler.addPlanStrategyFactory("activityLocations", new ActivityLocationStrategyFactory(random, numThreads, "home", controler,
				mutationError));

		settings = new StrategySettings(Id.create(2, StrategySettings.class));
		settings.setStrategyName("doNothing");
	
		controler.addPlanStrategyFactory("doNothing", new PlanStrategyFactory() {

			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager) {
				return new PlanStrategy() {

					@Override
					public void run(HasPlansAndId<Plan, Person> person) {
					}

					@Override
					public void init(ReplanningContext replanningContext) {
					}

					@Override
					public void finish() {
					}
				};
			}
		});
		/*
		 * setup scoring and cadyts integration
		 */
		boolean disableCadyts = Boolean.parseBoolean(controler.getConfig().getModule("gsv").getValue("disableCadyts"));
		// boolean personEqualsVeh =
		// Boolean.parseBoolean(controler.getConfig().getModule("gsv").getValue("personEqualsVeh"));
		LinkOccupancyCalculator calculator = new LinkOccupancyCalculator(controler.getScenario().getPopulation());
		controler.getEvents().addHandler(calculator);
		if (!disableCadyts) {
			CadytsContext context = new CadytsContext(controler.getScenario().getConfig(), null, calculator);
			// controler.addControlerListener(context);
			controler.setScoringFunctionFactory(new ScoringFactory(context, controler.getConfig(), controler.getScenario().getNetwork()));

			controler.addControlerListener(context);
			// Logger.getRootLogger().setLevel(org.apache.log4j.Level.FATAL);
			// context.notifyStartup(event);
			// Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
			controler.addControlerListener(new CadytsRegistration(context));
			// controler.addPlanSelectorFactory("mySelector", new
			// SelectorFactory());
		}
		Config config = controler.getConfig();
		String countsFile = config.findParam("gsv", "countsfile");
		double factor = Double.parseDouble(config.findParam("counts", "countsScaleFactor"));

		DTVAnalyzer dtv = new DTVAnalyzer(controler.getScenario().getNetwork(), controler, controler.getEvents(), countsFile, calculator, factor);
		controler.addControlerListener(dtv);

		controler.addControlerListener(new CountsCompareAnalyzer(calculator, countsFile, factor));

		controler.setModules(new AbstractModule() {
			@Override
			public void install() {
			
				include(new ScenarioElementsModule());
				// include(new TravelTimeCalculatorModule());
				include(new TravelDisutilityModule());
				include(new TripRouterModule());
				include(new StrategyManagerModule());
				// include(new LinkStatsModule());
				// include(new VolumesAnalyzerModule());
				// include(new LegHistogramModule());
				// include(new LegTimesModule());
				// include(new ScoreStatsModule());
				// include(new CountsModule());
				// include(new PtCountsModule());
				// include(new VspPlansCleanerModule());
				// include(new SignalsModule());

				bindToInstance(TravelTime.class, MobsimConnectorFactory.getTravelTimeCalculator(1.5));

			}
		});

		// controler.addOverridingModule(abstractModule);
		controler.run();

	}

	private static class ControllerSetup implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			/*
			 * connect facilities to links
			 */
			NetworkImpl network = (NetworkImpl) controler.getScenario().getNetwork();
			for (ActivityFacility facility : controler.getScenario().getActivityFacilities().getFacilities().values()) {
				Coord coord = facility.getCoord();
				Link link = NetworkUtils.getNearestLink(network, coord);
				((ActivityFacilityImpl) facility).setLinkId(link.getId());
			}

			/*
			 * setup analysis modules
			 */
			TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
			task.addTask(new TripGeoDistanceTask(controler.getScenario().getActivityFacilities()));
			task.addTask(new SpeedFactorTask(controler.getScenario().getActivityFacilities()));
			task.addTask(new ScoreTask());
			// task.addTask(new ArrivalLoadTask());
			// task.addTask(new DepartureLoadTask());
			task.addTask(new PkmTask(controler.getScenario().getActivityFacilities()));
			// task.addTask(new ModeShareTask());
			// task.addTask(new ActivityDurationTask());
			// task.addTask(new ActivityLoadTask());
			// task.addTask(new LegLoadTask());
			task.addTask(new TripDurationTask());
			task.addTask(new TripPurposeShareTask());
			// task.addTask(new LegFrequencyTask());
			task.addTask(new TripCountTask());

			AnalyzerListiner listener = new AnalyzerListiner();
			listener.task = task;
			listener.controler = controler;
			listener.notifyStartup(event);

			controler.addControlerListener(listener);
		}

	}

	private static class CadytsRegistration implements AfterMobsimListener {

		private CadytsContext context;

		public CadytsRegistration(CadytsContext context) {
			this.context = context;
		}

		@Override
		public void notifyAfterMobsim(AfterMobsimEvent event) {
			Population population = event.getControler().getScenario().getPopulation();
			for (Person person : population.getPersons().values()) {
				context.getCalibrator().addToDemand(context.getPlansTranslator().getPlanSteps(person.getSelectedPlan()));
			}

		}

	}

	private static class ScoringFactory implements ScoringFunctionFactory {

		// private ScoringFunction function;

		private CadytsContext context;

		private Config config;

		public ScoringFactory(CadytsContext context, Config config, Network network) {
			this.context = context;
			this.config = config;
		}

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			SumScoringFunction sum = new SumScoringFunction();
			CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, context);
			sum.addScoringFunction(scoringFunction);
			return sum;
		}

	}

	private static class AnalyzerListiner implements IterationEndsListener, IterationStartsListener, StartupListener {

		private Controler controler;

		private TrajectoryAnalyzerTask task;

		private TrajectoryEventsBuilder builder;

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			try {
				TrajectoryAnalyzer.analyze(builder.trajectories(), task, controler.getControlerIO().getIterationPath(event.getIteration()));

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		public void notifyIterationStarts(IterationStartsEvent event) {
			builder.reset(event.getIteration());
		}

		@Override
		public void notifyStartup(StartupEvent event) {

			Set<Person> person = new HashSet<Person>(controler.getScenario().getPopulation().getPersons().values());
			builder = new TrajectoryEventsBuilder(person);
			controler.getEvents().addHandler(builder);
		}
	}
}
