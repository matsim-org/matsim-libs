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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelectorFactory;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

import playground.johannes.coopsim.analysis.ActivityDurationTask;
import playground.johannes.coopsim.analysis.ActivityLoadTask;
import playground.johannes.coopsim.analysis.ArrivalLoadTask;
import playground.johannes.coopsim.analysis.DepartureLoadTask;
import playground.johannes.coopsim.analysis.LegLoadTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzer;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTask;
import playground.johannes.coopsim.analysis.TrajectoryAnalyzerTaskComposite;
import playground.johannes.coopsim.analysis.TripCountTask;
import playground.johannes.coopsim.analysis.TripDistanceTask;
import playground.johannes.coopsim.analysis.TripDurationTask;
import playground.johannes.coopsim.analysis.TripPurposeShareTask;
import playground.johannes.coopsim.pysical.TrajectoryEventsBuilder;
import playground.johannes.gsv.analysis.CountsCompareAnalyzer;
import playground.johannes.gsv.analysis.PkmTask;
import playground.johannes.gsv.analysis.ScoreTask;
import playground.johannes.gsv.analysis.SpeedFactorTask;
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
		
		StrategySettings settings = new StrategySettings(new IdImpl(1));
		settings.setModuleName("activityLocations");
		settings.setProbability(1.0);
		int numThreads = controler.getConfig().global().getNumberOfThreads();
		controler.addPlanStrategyFactory("activityLocations", new ActivityLocationStrategyFactory(random, numThreads, "home"));
		
		controler.addPlanSelectorFactory("mySelector", new SelectorFactory());
		controler.run();
		
	}
	
	private static class SelectorFactory implements PlanSelectorFactory<Plan, Person> {

		private ExpBetaPlanSelector<Plan, Person> instance;
		/* (non-Javadoc)
		 * @see org.matsim.core.replanning.selectors.PlanSelectorFactory#createPlanSelector(org.matsim.api.core.v01.Scenario)
		 */
		@Override
		public GenericPlanSelector<Plan, Person> createPlanSelector(Scenario scenario) {
			if(instance == null)
				instance = new ExpBetaPlanSelector<Plan, Person>(1.0);
			
			return instance;
		}
		
	}
	
	private static class ControllerSetup implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			Config config = event.getControler().getConfig();
			/*
			 * connect facilities to links
			 */
			NetworkImpl network = (NetworkImpl) controler.getNetwork();
			for(ActivityFacility facility : controler.getScenario().getActivityFacilities().getFacilities().values()) {
				Coord coord = facility.getCoord();
//				Link link = network.getNearestLinkExactly(coord);
				Link link = network.getNearestLink(coord);
				((ActivityFacilityImpl)facility).setLinkId(link.getId());
			}
			/*
			 * setup scoring and cadyts integration
			 */
			boolean disableCadyts = Boolean.parseBoolean(config.getModule("gsv").getValue("disableCadyts"));
			if (!disableCadyts) {
				CadytsContext context = new CadytsContext(controler.getScenario().getConfig());
				controler.setScoringFunctionFactory(new ScoringFactory(context, controler.getConfig(), controler.getNetwork()));

				controler.addControlerListener(context);
				Logger.getRootLogger().setLevel(org.apache.log4j.Level.FATAL);
				context.notifyStartup(event);
				Logger.getRootLogger().setLevel(org.apache.log4j.Level.DEBUG);
				controler.addControlerListener(new CadytsRegistration(context));
			}
			/*
			 * setup analysis modules
			 */
			LinkOccupancyCalculator calculator = new LinkOccupancyCalculator(controler.getPopulation());
			controler.getEvents().addHandler(calculator);
			String countsFile = config.findParam("gsv", "countsfile");
			double factor = Double.parseDouble(config.findParam("counts", "countsScaleFactor"));
			
			DTVAnalyzer dtv = new DTVAnalyzer(controler.getNetwork(), controler, controler.getEvents(), countsFile, calculator, factor);
			controler.addControlerListener(dtv);
			
			controler.addControlerListener(new CountsCompareAnalyzer(calculator, countsFile, factor));
			
			TrajectoryAnalyzerTaskComposite task = new TrajectoryAnalyzerTaskComposite();
			task.addTask(new TripDistanceTask(controler.getFacilities()));
			task.addTask(new SpeedFactorTask(controler.getFacilities()));
			task.addTask(new ScoreTask());
			task.addTask(new ArrivalLoadTask());
			task.addTask(new DepartureLoadTask());
			task.addTask(new PkmTask(controler.getFacilities()));
//			task.addTask(new ModeShareTask());
			task.addTask(new ActivityDurationTask());
			task.addTask(new ActivityLoadTask());
			task.addTask(new LegLoadTask());
			task.addTask(new TripDurationTask());
			task.addTask(new TripPurposeShareTask());
//			task.addTask(new LegFrequencyTask());
			task.addTask(new TripCountTask());
			
			AnalyzerListiner listener = new AnalyzerListiner();
			listener.task = task;
			listener.controler = controler;
			listener.notifyStartup(event);
			
			controler.addControlerListener(listener);
			/*
			 * replace travel time calculator
			 */
			
		}
		
	}

	private static class CadytsRegistration implements BeforeMobsimListener {

		private CadytsContext context;
		
		public CadytsRegistration(CadytsContext context) {
			this.context = context;
		}
		
		@Override
		public void notifyBeforeMobsim(BeforeMobsimEvent event) {
			Population population = event.getControler().getPopulation();
			for(Person person : population.getPersons().values()) {
				context.getCalibrator().addToDemand(context.getPlansTranslator().getPlanSteps(person.getSelectedPlan()));
			}
			
		}
		
	}
	
	private static class ScoringFactory implements ScoringFunctionFactory {

		private ScoringFunction function;
		
		private CadytsContext context;
		
		private Config config;
		
		private Network network;
		
		public ScoringFactory(CadytsContext context, Config config, Network network) {
			this.context = context;
			this.config = config;
		}
		
		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			if(function == null) {
				CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore());
				SumScoringFunction sum = new SumScoringFunction();
				sum.addScoringFunction(new CharyparNagelLegScoring(params, network));
				sum.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
			
				CadytsScoring scoringFunction = new CadytsScoring(person.getSelectedPlan(), config, context);
//				final double cadytsScoringWeight = 10.0;
//				//final double cadytsScoringWeight = 0.0;
//				scoringFunction.setWeightOfCadytsCorrection(cadytsScoringWeight) ;
				sum.addScoringFunction(scoringFunction );
			}
			
			return function;
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
			
			Set<Person> person = new HashSet<Person>(controler.getPopulation().getPersons().values());
			builder = new TrajectoryEventsBuilder(person);
			controler.getEvents().addHandler(builder);
		}
	}
	
//	private class DummyTravelTimeCalculatorFactory implements TravelTimeCalculatorFactory {
//
//		/* (non-Javadoc)
//		 * @see org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory#createTravelTimeCalculator(org.matsim.api.core.v01.network.Network, org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup)
//		 */
//		@Override
//		public TravelTimeCalculator createTravelTimeCalculator(Network network,
//				TravelTimeCalculatorConfigGroup group) {
//			// TODO Auto-generated method stub
//			return null;
//		}
//		
//	}
//	
//	private static class DummyTravelTimeCalculator extends TravelTimeCalculator {
//
//		/**
//		 * @param network
//		 * @param timeslice
//		 * @param maxTime
//		 * @param ttconfigGroup
//		 */
//		public DummyTravelTimeCalculator(Network network, int timeslice,
//				int maxTime, TravelTimeCalculatorConfigGroup ttconfigGroup) {
//			super(network, timeslice, maxTime, ttconfigGroup);
//			// TODO Auto-generated constructor stub
//		}
//		
//	}
}
