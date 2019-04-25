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

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.router.TransitRouter;

/**
 * @author pieterfourie
 * <p>
 * A controler that alternates between the QSim and PSim for the mobility simulation.
 * Run this class with no arguments to get printed help listing current command line options.
 */
public class RunPSim {
	private Config config;
	private Scenario scenario;
	private TransitPerformanceRecorder transitPerformanceRecorder;
	private Controler matsimControler;

	private PlanCatcher plancatcher;
	private PSimProvider pSimProvider;

	public RunPSim(Config config, PSimConfigGroup pSimConfigGroup) {
		this.config = config;
		this.scenario = ScenarioUtils.loadScenario(config);;

		//The following line will make the controler use the events manager that doesn't check for event order.
		//This is essential for pseudo-simulation as the PSim module generates events on a person-basis,
		//not a system basis
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		this.matsimControler = new Controler(scenario);

		MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup,scenario);
		matsimControler.addControlerListener(mobSimSwitcher);

		matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
				bindMobsim().toProvider(SwitchingMobsimProvider.class);
				bind(WaitTimeCalculator.class).to(PSimWaitTimeCalculator.class);
				bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);
				bind(StopStopTimeCalculator.class).to(PSimStopStopTimeCalculator.class);
				bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);

//				bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
				// I made TravelTimeCalculator final, so PSimTravelTimeCalculator can no longer inherit from it.  The following statement binds PSimTravelTimeCalculator
				// directly.  I am not even sure if that is necessary.  Pls let me know if you get stuck and I will try to help.  kai, feb'19

				bind(PSimTravelTimeCalculator.class).in( Singleton.class ) ;
				bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
				addRoutingModuleBinding(TransportMode.pt).toProvider(TransitRouterEventsWSFactory.class);
				bind(PlanCatcher.class).toInstance(new PlanCatcher());
				bind(PSimProvider.class).toInstance(new PSimProvider(scenario,matsimControler.getEvents()));
				bind(QSimProvider.class);
			}
		});

		if (config.transit().isUseTransit()) {
			if (pSimConfigGroup.isFullTransitPerformanceTransmission()) {
				transitPerformanceRecorder = new TransitPerformanceRecorder(matsimControler.getScenario(), matsimControler.getEvents(), mobSimSwitcher);
			}

		}



	}

	public static void main(String args[]) {
		Config config = ConfigUtils.loadConfig(args[0]);
		config.controler().setCreateGraphs(false);

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
