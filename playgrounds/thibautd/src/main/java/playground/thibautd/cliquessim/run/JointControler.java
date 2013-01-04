/* *********************************************************************** *
 * project: org.matsim.*
 * JointControler.java
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
package playground.thibautd.cliquessim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.replanning.JointPlansReplanning;
import playground.thibautd.cliquessim.replanning.JointStrategyManager;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.router.JointPlanRouter;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;

/**
 * Custom controler for handling clique replanning
 * @author thibautd
 */
public class JointControler extends Controler {
	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	public JointControler(final Scenario scenario) {
		super(scenario);
	}

	/*
	 * =========================================================================
	 * overrided methods
	 * =========================================================================
	 */
	@Override
	protected void setUp() {
		super.setUp();

		ParallelPersonAlgorithmRunner.run(
				getPopulation(),
				getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new ImportedJointRoutesChecker( getTripRouterFactory().createTripRouter() );
					}
		});
	}

	@Override
	protected void loadControlerListeners() {
		addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				setTripRouterFactory( new JointTripRouterFactory( event.getControler() ) );
			}
		});

		// Note: The PlansReplanning listenner is still there!
		// We cannot remove it once it is added, and overriding the
		// loadCoreListenners method causes problems every second refactoring
		// of the controler. td 11.2012
		// JointStrategyManager is designed so that it is not a problem.
		addControlerListener( new JointPlansReplanning() );

		super.loadControlerListeners();
	}

	@Override
	protected void loadData() {
		setMobsimFactory( new JointQSimFactory() );
		super.loadData();
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new JointStrategyManager();
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm() {
		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		return new JointPlanRouter( tripRouterFactory.createTripRouter() );
	}

	@Override
	public TripRouterFactory getTripRouterFactory() {
		return new JointTripRouterFactory( this );
	}
}
