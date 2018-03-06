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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorSerializable;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculatorSerializable;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;

/**
 * @author pieterfourie
 * <p>
 * A controler that alternates between the QSim and PSim for the mobility simulation.
 * Run this class with no arguments to get printed help listing current command line options.
 */
public class RunPSim {
	private PSimConfigGroup pSimConfigGroup;
	private Config config;
	private Scenario scenario;
	private TransitPerformanceRecorder transitPerformanceRecorder;
	private Controler matsimControler;
	private WaitTimeCalculatorSerializable waitTimeCalculator;
	private StopStopTimeCalculatorSerializable stopStopTimeCalculator;
	private PSimTravelTimeCalculator carTravelTimeCalculator;


	private RunPSim(Scenario scenario) {
		this.scenario = scenario;
		this.config = scenario.getConfig();
		pSimConfigGroup = new PSimConfigGroup();
		config.addModule(pSimConfigGroup);
		initPSim();

		matsimControler.getEvents().addHandler(waitTimeCalculator);
		matsimControler.getEvents().addHandler(stopStopTimeCalculator);
		matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(new TransitRouterEventsWSFactory(RunPSim.this.scenario,
						waitTimeCalculator.getWaitTimes(),
						stopStopTimeCalculator.getStopStopTimes()));
			}
		});
	}

	public static void main(String args[]) {
		new RunPSim(ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]))).run();
	}


	public MatsimServices getMatsimControler() {
		return matsimControler;
	}

	public void run() {
		matsimControler.run();
	}

	private void initPSim() {
		//The following line will make the controler use the events manager that doesn't check for event order.
		//This is essential for pseudo-simulation as the PSim module generates events on a person-basis,
		//not a system basis
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);
		this.matsimControler = new Controler(scenario);
		matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(SwitchingMobsimProvider.class);
			}
		});
		MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup.getIterationsPerCycle(), scenario);
		matsimControler.addControlerListener(mobSimSwitcher);
		matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
			}
		});

		this.carTravelTimeCalculator = new PSimTravelTimeCalculator(matsimControler.getScenario().getNetwork(),
				config.travelTimeCalculator(), (int) (config.qsim().getEndTime() - config.qsim().getStartTime()), mobSimSwitcher);
		matsimControler.getEvents().addHandler(carTravelTimeCalculator);
		if (config.transit().isUseTransit()) {
			if (pSimConfigGroup.isFullTransitPerformanceTransmission()) {
				transitPerformanceRecorder = new TransitPerformanceRecorder(matsimControler.getScenario(), matsimControler.getEvents(), mobSimSwitcher);
			}
			this.waitTimeCalculator = new PSimWaitTimeCalculator(
					matsimControler.getScenario().getTransitSchedule(),
					config,
					mobSimSwitcher);
			matsimControler.getEvents().addHandler(waitTimeCalculator);
			this.stopStopTimeCalculator = new PSimStopStopTimeCalculator(
					matsimControler.getScenario().getTransitSchedule(),
					matsimControler
							.getConfig().travelTimeCalculator()
							.getTraveltimeBinSize(),
					(int) (config.qsim().getEndTime() - config.qsim().getStartTime()),
					mobSimSwitcher);
			matsimControler.getEvents().addHandler(stopStopTimeCalculator);
		}

		config.controler().setCreateGraphs(false);

	}

}
