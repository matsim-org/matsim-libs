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

import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
//import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.AgentMemories;
import playground.anhorni.surprice.Surprice;

public class SurpriceScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory{
	
	private final Controler controler;
	private AgentMemories memories = new AgentMemories();
	private String day;
	private ObjectAttributes preferences;
	private Random random;

	public SurpriceScoringFunctionFactory(Controler controler, PlanCalcScoreConfigGroup configGroup, Network network, 
			AgentMemories memories, String day, ObjectAttributes preferences) {
		super(configGroup, network);
		this.controler = controler;
		this.memories = memories;
		this.day = day;
		this.preferences = preferences;	
	}
	
	public ScoringFunction createNewScoringFunction(Plan plan) {	
		
		// generate alpha_trip with id of agent
		this.random = new Random(Integer.parseInt(plan.getPerson().getId().toString()));
		
		for (int i = 0; i < 100; i++) {
			this.random.nextDouble();
		}
		double alphaTrip = 0.0;	
		double alphaTripRange = Double.parseDouble(controler.getConfig().findParam(Surprice.SURPRICE_RUN, "alphaTripRange"));
		alphaTrip = 0.5 * alphaTripRange * (1.0 - this.random.nextDouble());		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				
		SurpriceActivityScoringFunction scoringFunction = new SurpriceActivityScoringFunction(
				plan, super.getParams(), controler.getConfig(), this.controler.getFacilities(), 
				(Double)this.preferences.getAttribute(plan.getPerson().getId().toString(), "alpha"),
				this.day);
		
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		
		scoringFunctionAccumulator.addScoringFunction(new SurpriceLegScoringFunction(
				super.getParams(), controler.getNetwork(), controler.getConfig(),
				this.memories.getMemory(plan.getPerson().getId()),
				this.day,
				(Double)this.preferences.getAttribute(plan.getPerson().getId().toString(), "alpha"),
				(Double)this.preferences.getAttribute(plan.getPerson().getId().toString(), "gamma"),
				alphaTrip));
		
		//scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(super.getParams()));
		return scoringFunctionAccumulator;
	}
}
