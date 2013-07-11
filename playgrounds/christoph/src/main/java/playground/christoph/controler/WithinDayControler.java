/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.OnlyTravelDependentScoringFunctionFactory;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.InitialReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

/**
 * This Controller should give an Example what is needed to run
 * Simulations with WithinDayReplanning.
 * 
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 * 
 * Additional Parameters have to be set in the WithinDayControler
 * Class. Here we just add the Knowledge Component.
 * 
 * By default "test/scenarios/berlin/config.xml" should work.
 * 
 * @author Christoph Dobler
 */
public class WithinDayControler extends WithinDayController {

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pActEndReplanning = 1.0;
	protected double pLeaveLinkReplanning = 1.0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 2;

	protected TravelTime travelTime;

	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplannerFactory initialReplannerFactory;
	protected WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory;
	protected WithinDayDuringLegReplannerFactory duringLegReplannerFactory;

	protected SelectHandledAgentsByProbability selector;
	protected QSim sim;
	
	static final Logger log = Logger.getLogger(WithinDayControler.class);

	public WithinDayControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public WithinDayControler(Config config) {
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters() {
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTravelDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {

		super.initWithinDayEngine(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.initWithinDayTripRouterFactory();
		super.getWithinDayEngine().setTripRouterFactory(this.getWithinDayTripRouterFactory());

		this.initialIdentifier = new InitialIdentifierImplFactory(this.sim).createIdentifier();
		this.selector.addIdentifier(this.initialIdentifier, this.pInitialReplanning);
		this.initialReplannerFactory = new InitialReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.initialReplannerFactory.addIdentifier(this.initialIdentifier);
		super.getWithinDayEngine().addIntialReplannerFactory(this.initialReplannerFactory);

		super.createAndInitActivityReplanningMap();
		ActivityReplanningMap activityReplanningMap = super.getActivityReplanningMap();
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringActivityIdentifier, this.pActEndReplanning);
		this.duringActivityReplannerFactory = new NextLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.duringActivityReplannerFactory.addIdentifier(this.duringActivityIdentifier);
		super.getWithinDayEngine().addDuringActivityReplannerFactory(this.duringActivityReplannerFactory);

		super.createAndInitLinkReplanningMap();
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringLegIdentifier, this.pLeaveLinkReplanning);
		this.duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine());
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		super.getWithinDayEngine().addDuringLegReplannerFactory(this.duringLegReplannerFactory);
	}
	
	@Override
	protected void runMobSim() {

		log.info("Initialize Replanning Routers");
		initReplanningRouter();
		
		selector = new SelectHandledAgentsByProbability();
		super.getFixedOrderSimulationListener().addSimulationListener(selector);
		
		super.runMobSim();
	}

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
			final WithinDayControler controler = new WithinDayControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}