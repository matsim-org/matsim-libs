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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;
import playground.anhorni.surprice.AgentMemories;
import playground.anhorni.surprice.Surprice;

public class SurpriceScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory{
	
	private final Controler controler;
	private AgentMemories memories = new AgentMemories();
	private String day;
	private PlanCalcScoreConfigGroup config;
	private ObjectAttributes preferences;


	public SurpriceScoringFunctionFactory(Controler controler, PlanCalcScoreConfigGroup configGroup, Network network, 
			AgentMemories memories, String day, ObjectAttributes preferences) {
		super(configGroup, network);
		this.controler = controler;
		this.memories = memories;
		this.day = day;
		this.config = configGroup;
		this.preferences = preferences;
	}
		
	public ScoringFunction createNewScoringFunction(Person person) {			
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();

		scoringFunctionAccumulator.addScoringFunction(new SurpriceActivityScoringFunction(
				person.getSelectedPlan(), CharyparNagelScoringParameters.getBuilder(config).create(), controler.getConfig(), this.controler.getScenario().getActivityFacilities(),
				this.day));

		scoringFunctionAccumulator.addScoringFunction(new SurpriceLegScoringFunction(
				CharyparNagelScoringParameters.getBuilder(config).create(),
                this.controler.getScenario().getNetwork(),
				this.memories.getMemory(person.getId()),
				this.day, (PersonImpl)person, 
				(Double)this.preferences.getAttribute(person.getId().toString(), "dudm")));
		
		if (Boolean.parseBoolean(controler.getConfig().findParam(Surprice.SURPRICE_RUN, "useRoadPricing"))) {
			scoringFunctionAccumulator.addScoringFunction(new SupriceTollScoringFunction(
					CharyparNagelScoringParameters.getBuilder(config).create(), (PersonImpl)person, this.day,
					(Double)this.preferences.getAttribute(person.getId().toString(), "dudm")));
		}				
		//scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(super.getParams()));
		return scoringFunctionAccumulator;
	}
}
