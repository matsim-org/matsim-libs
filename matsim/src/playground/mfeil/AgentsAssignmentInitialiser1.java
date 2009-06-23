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

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import java.util.LinkedList;



/**
 * @author Matthias Feil
 * Initializes the agentsAssigner.
 */

public class AgentsAssignmentInitialiser1 extends AbstractMultithreadedModule {
	
	protected final NetworkLayer 					network;
	protected final Controler						controler;
	protected final LocationMutatorwChoiceSet 		locator;
	protected final PlanScorer 						scorer;
	protected final RecyclingModule1				module;
	protected final double							minimumTime;
	protected LinkedList<String>					nonassignedAgents;
	protected final DepartureDelayAverageCalculator 	tDepDelayCalc;
	
	private final DistanceCoefficients 				distanceCoefficients;

		
	public AgentsAssignmentInitialiser1 (final Controler controler, 
			final DepartureDelayAverageCalculator 	tDepDelayCalc,
			final LocationMutatorwChoiceSet locator,
			final PlanScorer scorer,
			final RecyclingModule1 module, 
			final double minimumTime,
			final DistanceCoefficients distanceCoefficients,
			LinkedList<String> nonassignedAgents) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);	
		this.locator = locator;
		this.scorer = scorer;
		this.module = module;
		this.minimumTime = minimumTime;
		this.nonassignedAgents = nonassignedAgents;
		this.tDepDelayCalc = tDepDelayCalc;
		
		this.distanceCoefficients = distanceCoefficients;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm agentsAssigner;
		
		agentsAssigner = new AgentsAssigner1 (this.controler, this.tDepDelayCalc,
					this.locator, this.scorer, this.module, this.minimumTime, this.distanceCoefficients,
					this.nonassignedAgents);
		
		return agentsAssigner;
	}
}
