/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesAndDesiresScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.telaviv.scoring.functions;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.Desires;

/**
 * Generates {@link CharyparNagelOpenTimesActivityScoring}s.
 *
 * An extension of CharyparNagelOpenTimesScoringFunctionFactory that get an
 * agent's typical activity durations from its desires and not from the config file.
 * 
 * The implementation is not very memory efficient since the scoring parameters are
 * duplicated for all agents. They only differ in the typical activity durations. However,
 * this implementation is more flexible than having an adapted ActivityScoringFunction.
 * 
 * @author cdobler
 */
public class CharyparNagelOpenTimesAndDesiresScoringFunctionFactory implements ScoringFunctionFactory {

	private final Map<String, Double> originalTypicalDurations;
	private final Map<Id, CharyparNagelScoringParameters> paramsMap;
    private Scenario scenario;
	private PlanCalcScoreConfigGroup config;

    public CharyparNagelOpenTimesAndDesiresScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final Scenario scenario) {
    	this.config = config;
		this.scenario = scenario;
		this.paramsMap = new HashMap<Id, CharyparNagelScoringParameters>();
		
		/*
		 * backup configs original typical durations
		 */
		this.originalTypicalDurations = new HashMap<String, Double>();
		for (ActivityParams params : config.getActivityParams()) {
			originalTypicalDurations.put(params.getActivityType(), params.getTypicalDuration());
		}
	}

	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		
		CharyparNagelScoringParameters params = this.paramsMap.get(person.getId());
		/*
		 * We get the typical durations from the persons' desires. Instead, we
		 * could also store them in the PersonAttributes and get them from there.
		 */		
		if (params == null) {
			// ensure that only one thread at a time can adapt the config parameters
			synchronized (this) {
				Desires desires = ((PersonImpl) person).getDesires();
				
				// replace typical durations in config - quite an ugly hack...
				for (ActivityParams activityParams : config.getActivityParams()) {
					double typicalDuration = desires.getActivityDuration(activityParams.getActivityType());
					if (typicalDuration != Time.UNDEFINED_TIME) activityParams.setTypicalDuration(typicalDuration);
				}
				
				// create CharyparNagelScoringParameters on person's typical durations
				params = CharyparNagelScoringParameters.getBuilder(this.config).createCharyparNagelScoringParameters();
				this.paramsMap.put(person.getId(), params);
				
				// reset original typical durations
				for (ActivityParams activityParams : config.getActivityParams()) {
					activityParams.setTypicalDuration(this.originalTypicalDurations.get(activityParams.getActivityType()));
				}
			}
		}
		
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelOpenTimesActivityScoring(params, scenario.getActivityFacilities()));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return sumScoringFunction;
	}
	
}
