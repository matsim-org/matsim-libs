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

package org.matsim.withinday.controller;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.scoring.functions.OnlyTravelDependentScoringFunctionFactory;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifierFactory;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.InitialReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

/**
 * This controller should give an example what is needed to run
 * simulations with WithinDayReplanning.
 *
 * The path to a config file is needed as argument to run the
 * simulation.
 * 
 * It should be possible to run this controller with
 * "src/test/resources/test/scenarios/berlin/config_withinday.xml"
 * as argument.
 *
 * @author Christoph Dobler
 */
public class ExampleWithinDayController extends WithinDayController {

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pDuringActivityReplanning = 1.0;
	protected double pDuringLegReplanning = 0.10;
	
	protected InitialIdentifierFactory initialIdentifierFactory;
	protected DuringActivityIdentifierFactory duringActivityIdentifierFactory;
	protected DuringLegIdentifierFactory duringLegIdentifierFactory;
	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplannerFactory initialReplannerFactory;
	protected WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory;
	protected WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
	protected ProbabilityFilterFactory initialProbabilityFilterFactory;
	protected ProbabilityFilterFactory duringActivityProbabilityFilterFactory;
	protected ProbabilityFilterFactory duringLegProbabilityFilterFactory;
	
	public ExampleWithinDayController(String[] args) {
		super(args);

		init();
	}

	// only for Batch Runs
	public ExampleWithinDayController(Config config) {
		super(config);

		init();
	}

	private void init() {
		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTravelDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	@Override
	protected void initReplanners(QSim sim) {
		
		/*
		 * Initialize TripRouterFactory for within-day replanning. For car travel times,
		 * a TravelTimeCollector is used which provides real time information.
		 */
		this.setTravelDisutilityFactory(new OnlyTimeDependentTravelCostCalculatorFactory());
		this.initWithinDayTripRouterFactory();
		
		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), 
				this.getTravelTimeCollector(), this.config.planCalcScore());
		
		this.initialIdentifierFactory = new InitialIdentifierImplFactory(sim);
		this.initialProbabilityFilterFactory = new ProbabilityFilterFactory(this.pInitialReplanning);
		this.initialIdentifierFactory.addAgentFilterFactory(this.initialProbabilityFilterFactory);
		this.initialIdentifier = initialIdentifierFactory.createIdentifier();
		this.initialReplannerFactory = new InitialReplannerFactory(this.scenarioData, this.getWithinDayEngine(),
				this.getWithinDayTripRouterFactory(), routingContext);
		this.initialReplannerFactory.addIdentifier(this.initialIdentifier);
		this.getWithinDayEngine().addIntialReplannerFactory(this.initialReplannerFactory);
		
		this.duringActivityIdentifierFactory = new ActivityEndIdentifierFactory(super.getActivityReplanningMap());
		this.duringActivityProbabilityFilterFactory = new ProbabilityFilterFactory(this.pDuringActivityReplanning);
		this.duringActivityIdentifierFactory.addAgentFilterFactory(this.duringActivityProbabilityFilterFactory);
		this.duringActivityIdentifier = duringActivityIdentifierFactory.createIdentifier();
		this.duringActivityReplannerFactory = new NextLegReplannerFactory(this.scenarioData, this.getWithinDayEngine(),
				this.getWithinDayTripRouterFactory(), routingContext);
		this.duringActivityReplannerFactory.addIdentifier(this.duringActivityIdentifier);
		this.getWithinDayEngine().addDuringActivityReplannerFactory(this.duringActivityReplannerFactory);
		
		this.duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(super.getLinkReplanningMap());
		this.duringLegProbabilityFilterFactory = new ProbabilityFilterFactory(this.pDuringLegReplanning);
		this.duringLegIdentifierFactory.addAgentFilterFactory(this.duringLegProbabilityFilterFactory);
		this.duringLegIdentifier = this.duringLegIdentifierFactory.createIdentifier();
		this.duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenarioData, this.getWithinDayEngine(),
				this.getWithinDayTripRouterFactory(), routingContext);
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(this.duringLegReplannerFactory);
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
			final ExampleWithinDayController controller = new ExampleWithinDayController(args);
			controller.run();
		}
		System.exit(0);
	}

}