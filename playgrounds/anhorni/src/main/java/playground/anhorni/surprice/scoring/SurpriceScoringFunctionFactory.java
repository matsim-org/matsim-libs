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
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.anhorni.surprice.AgentMemories;
import playground.anhorni.surprice.Surprice;

public class SurpriceScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory{
	
	private final Controler controler;
	private AgentMemories memories = new AgentMemories();
	private String day;
	private ObjectAttributes preferences;
	private Random random;
	private double alpha = 0.0;	
	private double gamma = 0.0;
	private double alphaTrip = 0.0;	
	private double gammaTrip = 0.0;

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
		double alphaTripRange = Double.parseDouble(controler.getConfig().findParam(Surprice.SURPRICE_RUN, "alphaTripRange"));
		double r = this.random.nextDouble();
		alphaTrip = alphaTripRange * (0.5 - r);	// tripRange * [-0.5 .. 0.5]
		gammaTrip = -1.0 * alphaTrip;
		alpha = (Double)this.preferences.getAttribute(plan.getPerson().getId().toString(), "alpha");
		gamma = (Double)this.preferences.getAttribute(plan.getPerson().getId().toString(), "gamma");
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
						
		scoringFunctionAccumulator.addScoringFunction(new SurpriceActivityScoringFunction(
				plan, super.getParams(), controler.getConfig(), this.controler.getFacilities(), this.alpha, this.alphaTrip, this.day));
		
		scoringFunctionAccumulator.addScoringFunction(new SurpriceLegScoringFunction(
				super.getParams(), controler.getNetwork(), controler.getConfig(),
				this.memories.getMemory(plan.getPerson().getId()),
				this.day, this.alpha, this.gamma, this.alphaTrip, this.gammaTrip));
		
		if (Boolean.parseBoolean(controler.getConfig().findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {	
			scoringFunctionAccumulator.addScoringFunction(new SupriceMoneyScoringFunction(
					super.getParams(), this.gamma));
		}
		
		//scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(super.getParams()));
		return scoringFunctionAccumulator;
	}
	
	public double getAlpha() {
		return alpha;
	}

	public double getGamma() {
		return gamma;
	}

	public double getAlphaTrip() {
		return alphaTrip;
	}

	public double getGammaTrip() {
		return gammaTrip;
	}
}
