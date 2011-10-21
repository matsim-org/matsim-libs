/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.locationchoice.bestresponse.scoring;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.charyparNagel.AgentStuckScoringFunction;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.locationchoice.bestresponse.preprocess.ComputeKValsAndMaxEpsilon;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class MixedScoringFunctionFactory extends org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory {
	private final Controler controler;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	private Config config;

	public MixedScoringFunctionFactory(Config config, Controler controler) {
		super(config.planCalcScore());				
		this.controler = controler;
		this.config = config;
		
		this.createObjectAttributes(Long.parseLong(config.locationchoice().getRandomSeed()));
	}
	
	private void createObjectAttributes(long seed) {
		this.facilitiesKValues = new ObjectAttributes();
		this.personsKValues = new ObjectAttributes();
		
		String pkValues = this.config.locationchoice().getpkValuesFile();
		if (!pkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personsKValues);
			try {
				attributesReader.parse(pkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
			}
		}
		else {
			this.computeAttributes(seed);
		}
		String fkValues = this.config.locationchoice().getfkValuesFile();
		if (!fkValues.equals("null")) {
			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.facilitiesKValues);
			try {
				attributesReader.parse(fkValues);
			} catch  (UncheckedIOException e) {
				// reading was not successful
				this.computeAttributes(seed);
			}
		}
		else {
			this.computeAttributes(seed);
		}
	}
	
	private void computeAttributes(long seed) {
		ComputeKValsAndMaxEpsilon computer = new ComputeKValsAndMaxEpsilon(seed, this.controler.getScenario(), this.config);
		computer.assignKValues();
		this.personsKValues = computer.getPersonsKValues();
		this.facilitiesKValues = computer.getFacilitiesKValues();
	}

	public ScoringFunction createNewScoringFunction(Plan plan) {		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
		
		MixedActivityScoringFunction scoringFunction = new MixedActivityScoringFunction((PlanImpl)plan, super.getParams(), 
				this.controler.getFacilities(), this.controler.getFacilityPenalties(), this.controler.getConfig(),
				this.facilitiesKValues, this.personsKValues);
		
		scoringFunctionAccumulator.addScoringFunction(scoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(super.getParams()));
		scoringFunctionAccumulator.addScoringFunction(new AgentStuckScoringFunction(super.getParams()));
		return scoringFunctionAccumulator;
	}
}
