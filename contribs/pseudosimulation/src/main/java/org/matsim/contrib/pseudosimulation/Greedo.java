/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.QSimModuleForPSim;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStragetyProvider;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStrategy;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAccelerator;
import org.matsim.contrib.pseudosimulation.searchacceleration.listeners.TransitStopInteractionListener;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.PTCapacityAdjusmentPerSample;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.ScoreHistogramLogger;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo extends AbstractModule {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Greedo.class);

	private final int defaultIterationsPerCycle = 3; // TODO

	private final boolean defaultFullTransitPerformanceTransmission = false;

	// -------------------- MEMBERS --------------------

	private final Map<String, Integer> randomInnovationStrategy2possibleVariationCnt = new LinkedHashMap<>();

	private final Set<String> bestResponseInnovationStrategyNames = new LinkedHashSet<>();

	private Config config = null;

	private Scenario scenario = null;

	private Controler controler = null;

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
		this.setBestResponseStrategy("ReRoute");
		this.setRandomInnovationStrategy("TimeAllocationMutator_ReRoute", 20);
		this.setRandomInnovationStrategy("TimeAllocationMutator", 20);
		this.setRandomInnovationStrategy("ChangeLegMode", 2); // switches entire plan
		this.setRandomInnovationStrategy("ChangeTripMode", 2); // switches entire plan
		this.setRandomInnovationStrategy("ChangeSingleLegMode", 2 * 4); // switches one out of ~4 legs
		this.setRandomInnovationStrategy("SubtoutModeChoice", 2 * 2); // switches one out of ~2 tours
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setBestResponseStrategy(final String strategyName) {
		this.bestResponseInnovationStrategyNames.add(strategyName);
	}

	public void setRandomInnovationStrategy(final String strategyName, final int numberOfVariations) {
		this.randomInnovationStrategy2possibleVariationCnt.put(strategyName, numberOfVariations);
	}

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * The following should not be necessary but is currently assumed when handling
		 * iteration-dependent replanning rates.
		 */
		config.controler()
				.setLastIteration(config.controler().getLastIteration() - config.controler().getFirstIteration());
		config.controler().setFirstIteration(0);

		/*
		 * Use the event manager that does not check for event order. Essential for
		 * PSim, which generates events person-after-person.
		 */
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		/*
		 * Ensure a valid PSim configuration; fall back to default values if not
		 * available.
		 */
		final boolean pSimConfigExists = config.getModules().containsKey(PSimConfigGroup.GROUP_NAME);
		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);
		if (!pSimConfigExists) {
			// Automagic -- use default values.
			pSimConf.setIterationsPerCycle(this.defaultIterationsPerCycle);
			pSimConf.setFullTransitPerformanceTransmission(this.defaultFullTransitPerformanceTransmission);
			// The following accounts for the pSim iteration overhead.
			config.controler().setLastIteration(config.controler().getLastIteration() * this.defaultIterationsPerCycle);
			// config.controler().setWriteEventsInterval(
			// config.controler().getWriteEventsInterval() *
			// this.defaultIterationsPerCycle);
			// config.controler()
			// .setWritePlansInterval(config.controler().getWritePlansInterval() *
			// this.defaultIterationsPerCycle);
			// config.controler().setWriteSnapshotsInterval(
			// config.controler().getWriteSnapshotsInterval() *
			// this.defaultIterationsPerCycle);
		}

		/*
		 * Ensure a valid Acceleration configuration; fall back to default values if not
		 * available.
		 */
		ConfigUtils.addOrGetModule(config, AccelerationConfigGroup.class);

		/*
		 * Use minimal choice set and always remove the worse plan. This probably as
		 * close as it can get to best-response in the presence of random innovation
		 * strategies.
		 */
		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");

		/*
		 * Keep only plan innovation strategies. Re-weight for maximum pSim efficiency.
		 * 
		 */
		double bestResponseStrategyWeightSum = 0.0;
		double randomStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (this.bestResponseInnovationStrategyNames.contains(strategyName)) {
				strategySettings.setWeight(1.0 / pSimConf.getIterationsPerCycle());
				bestResponseStrategyWeightSum += strategySettings.getWeight();
			} else if (this.randomInnovationStrategy2possibleVariationCnt.containsKey(strategyName)) {
				randomStrategyWeightSum += this.randomInnovationStrategy2possibleVariationCnt.get(strategyName);
			} else {
				strategySettings.setWeight(0.0); // i.e., dismiss
			}
		}
		final double randomStrategyFactor = (1.0 - bestResponseStrategyWeightSum) / randomStrategyWeightSum;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (this.randomInnovationStrategy2possibleVariationCnt.containsKey(strategyName)) {
				strategySettings.setWeight(
						randomStrategyFactor * this.randomInnovationStrategy2possibleVariationCnt.get(strategyName));
			}
		}

		/*
		 * Add a strategy that decides which of the better-response re-planning
		 * decisions coming out of the pSim is allowed to be implemented.
		 */
		final StrategySettings acceptIntendedReplanningStrategySettings = new StrategySettings();
		acceptIntendedReplanningStrategySettings.setStrategyName(AcceptIntendedReplanningStrategy.STRATEGY_NAME);
		acceptIntendedReplanningStrategySettings.setWeight(0.0); // changed dynamically
		config.strategy().addStrategySettings(acceptIntendedReplanningStrategySettings);
	}

	public void meet(final Scenario scenario) {

		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		} else if (this.scenario != null) {
			throw new RuntimeException("Have already met the scenario.");
		}
		this.scenario = scenario;

		ConfigUtils.addOrGetModule(this.config, AccelerationConfigGroup.class).configure(this.scenario,
				ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class).getIterationsPerCycle());
	}

	public void meet(final Controler controler) {

		if (this.scenario == null) {
			throw new RuntimeException("First meet the scenario.");
		} else if (this.controler != null) {
			throw new RuntimeException("Have already met the controler.");
		}
		this.controler = controler;

		controler.addOverridingModule(this);
	}

	// -------------------- PREPARE THE CONFIG --------------------

	@Override
	public void install() {

		if (this.controler == null) {
			throw new RuntimeException("First meet the controler.");
		}

		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class);
		final PSimProvider pSimProvider = new PSimProvider(this.scenario, this.controler.getEvents());
		final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConf, this.scenario);

		System.out.println("Installing the pSim");
		this.addControlerListenerBinding().toInstance(mobSimSwitcher);
		this.bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
		
		this.install(new QSimModuleForPSim());

		this.bind(WaitTimeCalculator.class).to(PSimWaitTimeCalculator.class);
		this.bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);
		this.bind(StopStopTimeCalculator.class).to(PSimStopStopTimeCalculator.class);
		this.bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);
		this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
		this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
		this.bind(PlanCatcher.class).toInstance(new PlanCatcher());
		this.bind(PSimProvider.class).toInstance(pSimProvider);

		System.out.println("Installing the acceleration");
		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);

		final TransitStopInteractionListener transitStopInteractionListener = new TransitStopInteractionListener(
				mobSimSwitcher, this.scenario.getPopulation(), this.scenario.getTransitVehicles(),
				this.scenario.getTransitSchedule());
		this.bind(TransitStopInteractionListener.class).toInstance(transitStopInteractionListener);
		this.addEventHandlerBinding().toInstance(transitStopInteractionListener);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void runCarOnly() {

		/*
		 * TODO The "auto-magic" needs refinement for sub-populations.
		 */

		/*
		 * Create the Greedo. Indicate all relevant plan innovation strategies. TODO Are
		 * all standard strategies pre-configured?
		 */

		Greedo greedo = null; // new Greedo();

		/*
		 * Create Config, Scenario, Controler in the usual order. Let the Greedo meet
		 * each one of them before moving on to the next. Finally, run the (now greedy)
		 * simulation.
		 */

		Config config = ConfigUtils.loadConfig(
				"/Users/GunnarF/NoBackup/data-workspace/searchacceleration//rerun-2015-11-23a_No_Toll_large/matsim-config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(48 * 3600); // FIXME QSim seems to interpret zero end time wrong.
		if (greedo != null) {
			greedo.meet(config);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		if (greedo != null) {
			greedo.meet(scenario);
		}

		Controler controler = new Controler(scenario);
		if (greedo != null) {
			greedo.meet(controler);
		}

		controler.run();
	}

	public static void runPT() {

		Greedo greedo = null; // new Greedo();

		Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/pt/production-scenario/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		// FIXME QSim misinterprets default time values.
		if (greedo != null) {
			greedo.meet(config);
			System.out.println("Greedo has met the config.");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
//		AdjustPseudoNetwork adj = new AdjustPseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_");
//		adj.run();
//		ValidationResult validationResult = TransitScheduleValidator.validateAll(scenario.getTransitSchedule(),
//				scenario.getNetwork());
//		TransitScheduleValidator.printResult(validationResult);

		final Set<Id<Person>> remove = new LinkedHashSet<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement planEl : person.getSelectedPlan().getPlanElements()) {
				if (planEl instanceof Leg && !"pt".equals(((Leg) planEl).getMode())) {
					remove.add(person.getId());
				}
			}
		}
		log.info("before pt filter: " + scenario.getPopulation().getPersons().size());
		for (Id<Person> removeId : remove) {
			scenario.getPopulation().getPersons().remove(removeId);
		}
		log.info("after pt filter: " + scenario.getPopulation().getPersons().size());

		if (greedo != null) {
			greedo.meet(scenario);
			System.out.println("Greedo has met the scenario.");
		}

		Controler controler = new Controler(scenario);
		if (greedo != null) {
			greedo.meet(controler);
			System.out.println("Greedo has met the controler.");
		}

		controler.addOverridingModule(new ScoreHistogramLogger(scenario.getPopulation(), config));

		double samplesize = config.qsim().getStorageCapFactor();
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);

//		List<TransitLine> removeLines = new ArrayList<>();
//		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
//			List<TransitRoute> removeRoutes = new ArrayList<>();
//			for (TransitRoute route : line.getRoutes().values()) {
//				log.info("route " + route.getId() + ": " + route.getRoute());
//				for (TransitRouteStop stop : route.getStops()) {
//					// System.out.println("At " + stop.getStopFacility().getId() + ": await departure time = true");
//					stop.setAwaitDepartureTime(true);
//				}
//				final List<Departure> removeDepartures = new ArrayList<>();
//				for (Departure dpt : route.getDepartures().values()) {
//					for (TransitRouteStop stop : route.getStops()) {											
//						if (dpt.getDepartureTime() + stop.getArrivalOffset() >= 24 * 3600) {
//							removeDepartures.add(dpt);
//						}
//					}
//				}
//				for (Departure dpt : removeDepartures) {
//					log.info("removed departure at " + dpt.getDepartureTime());
//					route.removeDeparture(dpt);
//				}
//				if (route.getDepartures().size() == 0) {
//					removeRoutes.add(route);
//				}
//			}
//			for (TransitRoute removeRoute : removeRoutes) {
//				log.info("removed route " + removeRoute.getId());
//				line.removeRoute(removeRoute);
//			}
//			if (line.getRoutes().size() == 0) {
//				removeLines.add(line);
//			}
//		}
//		for (TransitLine removeLine : removeLines) {
//			log.info("removed line " + removeLine.getId());
//			scenario.getTransitSchedule().removeTransitLine(removeLine);
//		}

		System.out.println("Adding the Raptor...");
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SBBTransitModule());
				install(new SwissRailRaptorModule());
			}
			@Provides
			QSimComponents provideQSimComponents() {
				QSimComponents components = new QSimComponents();
				new StandardQSimComponentsConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}
		});

		System.out.println("Starting the controler...");
		controler.run();

		System.out.println("... DONE");
	}

	public static void main(String[] args) {
		// runCarOnly();
		runPT();
	}
}
