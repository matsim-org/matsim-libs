/* *********************************************************************** *
 * project: org.matsim.*
 * ExampleWithinDayController.java
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

package playground.imob.feathers2forWithinDayReplanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * This class should give an example what is needed to run
 * simulations with WithinDayReplanning.
 *
 * The path to a config file is needed as argument to run the
 * simulation.
 *
 * @author Christoph Dobler
 */
final class RunWithinDayExample implements StartupListener {

	private Scenario scenario;
	private Provider<TripRouter> tripRouterProvider;
	private WithinDayEngine withinDayEngine;
	private ActivityReplanningMap activityReplanningMap;
	private WithinDayControlerListener withinDayControlerListener;
	

	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new Controler(args);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addControlerListenerBinding().to(RunWithinDayExample.class);
					bindCarTravelDisutilityFactory().toInstance(new OnlyTimeDependentTravelDisutilityFactory());
				}
			});
			// Use a Scoring Function that only scores the travel times.  Result will be that the router (see below) routes only based on travel times
			controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
			controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			controler.run();
		}
		System.exit(0);
	}

	@Inject
	public RunWithinDayExample(Scenario scenario, Provider<TripRouter> tripRouterProvider, WithinDayEngine withinDayEngine, ActivityReplanningMap activityReplanningMap) {
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.withinDayEngine = withinDayEngine;
		this.activityReplanningMap = activityReplanningMap;
		this.withinDayControlerListener = new WithinDayControlerListener();
		this.scenario.getConfig().qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		// initialze within-day module
		this.withinDayControlerListener.notifyStartup(event);
		
		this.initReplanners();
	}
	
	private void initReplanners() {
		// this defines which agents are replanned:
		ActivityEndIdentifierFactory activityEndIdentifierFactory = new ActivityEndIdentifierFactory(this.activityReplanningMap);

		// this defines the agent replanning:
		NextActivityAppendingReplannerFactory duringActivityReplannerFactory = new NextActivityAppendingReplannerFactory(this.scenario, this.withinDayEngine, this.tripRouterProvider);
		duringActivityReplannerFactory.addIdentifier(activityEndIdentifierFactory.createIdentifier());
		this.withinDayEngine.addDuringActivityReplannerFactory(duringActivityReplannerFactory);
	}

}