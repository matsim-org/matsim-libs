/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *  * ***********************************************************************
 */

package org.matsim.contrib.pseudosimulation;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitPerformanceFromEventBasedRouterInterfaces;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitPerformanceFromPSimSpecificImplementation;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.StopStopTime;
import org.matsim.contrib.pseudosimulation.trafficinfo.WaitTime;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author pieterfourie
 * <p>
 * A controler that alternates between the QSim and PSim for the mobility simulation.
 * Run this class with no arguments to get printed help listing current command line options.
 */
public class RunPSim {
	private Scenario scenario;
	private Controler matsimControler;

	public RunPSim(Config config, PSimConfigGroup pSimConfigGroup) {
		this.scenario = ScenarioUtils.loadScenario(config);

		//The following line will make the controler use the events manager that doesn't check for event order.
		//This is essential for pseudo-simulation as the PSim module generates events on a person-basis,
		//not a system basis
		config.eventsManager().setSynchronizeOnSimSteps(false);
		config.eventsManager().setNumberOfThreads(1);

		disableExperiencedPlanAnalysis(config);

		this.matsimControler = new Controler(scenario);

		MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup,scenario);
		matsimControler.addControllerListener(mobSimSwitcher);

		final PSimConfigGroup.TransitEmulation transitEmulation = transitEmulation(config, pSimConfigGroup);

		// A PSim iteration reads transit performance recorded during the preceding QSim iteration.
		// Without a recorder there is nothing to read, so transit legs would take no time at all.
		final TransitPerformanceRecorder transitPerformanceRecorder =
				transitEmulation == PSimConfigGroup.TransitEmulation.fullTransitPerformance
						? new TransitPerformanceRecorder(scenario, matsimControler.getEvents(), mobSimSwitcher)
						: null;

		matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
				bindMobsim().toProvider(SwitchingMobsimProvider.class);

//				bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
				// I made TravelTimeCalculator final, so PSimTravelTimeCalculator can no longer inherit from it.  The following statement binds PSimTravelTimeCalculator
				// directly.  I am not even sure if that is necessary.  Pls let me know if you get stuck and I will try to help.  kai, feb'19

				bind(PSimTravelTimeCalculator.class).in( Singleton.class ) ;
				bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
				// Registered as the handler itself so that its guarded reset survives PSim
				// iterations; see PSimTravelTimeCalculator.
				addEventHandlerBinding().to(PSimTravelTimeCalculator.class);
				switch (transitEmulation) {
					case none -> bind(TransitEmulator.class).to(NoTransitEmulator.class).in(Singleton.class);
					case fullTransitPerformance -> {
						Provider<TransitPerformance> transitPerformance =
								transitPerformanceRecorder::getTransitPerformance;
						bind(TransitPerformanceRecorder.class).toInstance(transitPerformanceRecorder);
						bind(TransitPerformance.class).toProvider(transitPerformance);
						bind(TransitEmulator.class).to(TransitPerformanceFromPSimSpecificImplementation.class)
								.in(Singleton.class);
					}
					case waitAndStopStopTimes -> {
						// Registered as the handlers themselves, for the same reason as
						// PSimTravelTimeCalculator: their guards live in reset and handleEvent.
						bind(PSimWaitTimeCalculator.class).in(Singleton.class);
						bind(PSimStopStopTimeCalculator.class).in(Singleton.class);
						addEventHandlerBinding().to(PSimWaitTimeCalculator.class);
						addEventHandlerBinding().to(PSimStopStopTimeCalculator.class);
						bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);
						bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);
						bind(TransitEmulator.class).to(TransitPerformanceFromEventBasedRouterInterfaces.class)
								.in(Singleton.class);
					}
				}
				bind(PlanCatcher.class).toInstance(new PlanCatcher());
				bind(PSimProvider.class).toInstance(new PSimProvider(scenario,matsimControler.getEvents()));
				bind(QSimProvider.class);
			}
		});
	}

	/**
	 * A scenario that does not simulate transit has no transit performance to record, so the
	 * configured emulation is overridden rather than left to measure nothing.
	 */
	static PSimConfigGroup.TransitEmulation transitEmulation(Config config, PSimConfigGroup pSimConfigGroup) {
		if (!config.transit().isUseTransit()) {
			return PSimConfigGroup.TransitEmulation.none;
		}
		return pSimConfigGroup.getTransitEmulation();
	}

	/**
	 * PSim emits events only for the agents whose plans were replanned, so the experienced plans
	 * assembled from that stream cover a fraction of the population. TripsAndLegsWriter fails
	 * outright on those partial plans, and the travel distance and mode statistics derived from
	 * them would silently describe the replanned subset rather than the population.
	 */
	private static void disableExperiencedPlanAnalysis(Config config) {
		if (config.controller().getWriteTripsInterval() > 0) {
			LogManager.getLogger(RunPSim.class).warn(
					"Disabling trips and legs output: it is assembled from experienced plans, which PSim "
							+ "iterations only produce for replanned agents.");
			config.controller().setWriteTripsInterval(0);
		}
	}

	public static void main(String args[]) {
		Config config = ConfigUtils.loadConfig(args[0]);
		config.controller().setCreateGraphsInterval(0);

		PSimConfigGroup pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);

		new RunPSim(config,pSimConfigGroup).run();
	}


	public MatsimServices getMatsimControler() {
		return matsimControler;
	}

	public void run() {
		matsimControler.run();
	}


}
