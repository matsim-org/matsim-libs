/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAssignmentInitialiser.java
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
package playground.mfeil;

import java.util.LinkedList;

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mfeil.MDSAM.ActivityTypeFinder;



/**
 * @author Matthias Feil
 * Initializes the agentsAssigner.
 */

public class AgentsAssignmentInitialiser extends AbstractMultithreadedModule {
	
	protected final NetworkImpl 						network;
	protected final Controler							controler;
	protected final LocationMutatorwChoiceSet 			locator;
	protected final PlanScorer 							scorer;
	protected final ScheduleRecycling						module;
	protected LinkedList<String>						nonassignedAgents;
	protected final DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final ActivityTypeFinder 					finder;
	
	private final DistanceCoefficients 					distanceCoefficients;

		
	public AgentsAssignmentInitialiser (final Controler controler, 
			final DepartureDelayAverageCalculator 	tDepDelayCalc,
			final LocationMutatorwChoiceSet locator,
			final PlanScorer scorer,
			final ActivityTypeFinder finder,
			final ScheduleRecycling module, 
			final DistanceCoefficients distanceCoefficients,
			LinkedList<String> nonassignedAgents) {
		super(controler.getConfig().global());
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);	
		this.locator = locator;
		this.scorer = scorer;
		this.finder = finder;
		this.module = module;
		this.nonassignedAgents = nonassignedAgents;
		this.tDepDelayCalc = tDepDelayCalc;
		
		this.distanceCoefficients = distanceCoefficients;
	}
	
	private void init(final NetworkImpl network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm agentsAssigner;
		
		agentsAssigner = new AgentsAssigner (this.controler, this.tDepDelayCalc,
					this.locator, this.scorer, this.finder, this.module, this.distanceCoefficients,
					this.nonassignedAgents);
		
		return agentsAssigner;
	}
}
