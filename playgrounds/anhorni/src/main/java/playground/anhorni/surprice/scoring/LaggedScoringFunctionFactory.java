/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.scoring;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;

import playground.anhorni.surprice.AgentMemories;

public class LaggedScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory{
	
	private final Controler controler;
	private AgentMemories memories = new AgentMemories();
	private CharyparNagelScoringParameters params;

	public LaggedScoringFunctionFactory(Controler controler, PlanCalcScoreConfigGroup configGroup, Network network, AgentMemories memories) {
		super(configGroup, network);
		this.controler = controler;
		this.memories = memories;
		this.params =  super.getParams();
	}
	
	public ScoringFunction createNewScoringFunction(Plan plan) {	
		
		// take into account lag effects
		this.adaptCoefficients(plan.getPerson());
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
			
		LaggedScoringFunction scoringFunction = new LaggedScoringFunction(plan, this.params, this.controler.getFacilities());
		
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(super.getParams(), controler.getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(super.getParams()));
		return scoringFunctionAccumulator;
	}
	
	private void adaptCoefficients(Person person) {
		
	}
}
