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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.anhorni.surprice.AgentMemories;

public class LaggedScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory{
	
	private final Controler controler;
	private AgentMemories memories = new AgentMemories();
	private String day;
	private ObjectAttributes vot;

	public LaggedScoringFunctionFactory(Controler controler, PlanCalcScoreConfigGroup configGroup, Network network, 
			AgentMemories memories, String day, ObjectAttributes vot) {
		super(configGroup, network);
		this.controler = controler;
		this.memories = memories;
		this.day = day;
		this.vot = vot;
	}
	
	public ScoringFunction createNewScoringFunction(Plan plan) {			
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		LaggedActivityScoringFunction scoringFunction = new LaggedActivityScoringFunction(
				plan, super.getParams(), controler.getConfig(), this.controler.getFacilities(),
				(Double)this.vot.getAttribute(plan.getPerson().getId().toString(), "vot"), this.day);
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		
		scoringFunctionAccumulator.addScoringFunction(new LaggedLegScoringFunction(
				super.getParams(), controler.getNetwork(), controler.getConfig(),
				this.memories.getMemory(plan.getPerson().getId()),
				this.day,
				(Double)this.vot.getAttribute(plan.getPerson().getId().toString(), "vot")));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(super.getParams()));
		return scoringFunctionAccumulator;
	}
}
