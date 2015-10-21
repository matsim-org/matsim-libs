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
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.OnlyTravelTimeDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;

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
			controler.addControlerListener(new RunWithinDayExample(controler));

			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

			controler.run();
		}
		System.exit(0);
	}
	
	public RunWithinDayExample(Controler controler) {
		
		this.scenario = controler.getScenario();
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		this.scenario.getConfig().qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.queue);

		// only necessary for visualization, which does, however, not work that well (replanned plans cannot be visualized):
//		this.withinDayControlerListener.setOriginalMobsimFactory( new MobsimFactory() {
//			@Override
//			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
//				QSim qsim = (QSim) new QSimFactory().createMobsim(sc, eventsManager) ;
//
//				OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim( sc.getConfig(), sc, eventsManager, qsim ) ;
//				OTFClientLive.run(sc.getConfig(), server);
//				
//				return qsim ;
//			}
//		} );

		// Use a Scoring Function that only scores the travel times.  Result will be that the router (see below) routes only based on travel times
		controler.setScoringFunctionFactory(new OnlyTravelTimeDependentScoringFunctionFactory());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(new OnlyTimeDependentTravelDisutilityFactory());
			}
		});

		// workaround
		// yyyy	 this works around what?
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		// initialze within-day module
		this.withinDayControlerListener.notifyStartup(event);
		
		this.initReplanners();
	}
	
	private void initReplanners() {
		// this defines which agents are replanned:
		ActivityEndIdentifierFactory activityEndIdentifierFactory = new ActivityEndIdentifierFactory(this.withinDayControlerListener.getActivityReplanningMap());

		// this defines the agent replanning:
		NextActivityAppendingReplannerFactory duringActivityReplannerFactory =
				new NextActivityAppendingReplannerFactory(this.scenario, this.withinDayControlerListener.getWithinDayEngine(),
				this.withinDayControlerListener.getWithinDayTripRouterFactory());
		duringActivityReplannerFactory.addIdentifier(activityEndIdentifierFactory.createIdentifier());
		this.withinDayControlerListener.getWithinDayEngine().addDuringActivityReplannerFactory(duringActivityReplannerFactory);
	}

}