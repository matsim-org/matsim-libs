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

package playground.anhorni.rc;
import org.apache.log4j.Logger;
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
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.population.Desires;
import org.matsim.utils.objectattributes.ObjectAttributes;;

public class RCScoringFunctionFactory implements ScoringFunctionFactory {
	private Scenario scenario;
	private PlanCalcScoreConfigGroup config;
	private CharyparNagelScoringParameters params = null;
	public ObjectAttributes prefs;
	private static final Logger log = Logger.getLogger(RCScoringFunctionFactory.class);
	
	public RCScoringFunctionFactory(final PlanCalcScoreConfigGroup config, final Scenario scenario) {		
    	this.config = config;
		this.scenario = scenario;
	}
		
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {	
		if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			this.params = CharyparNagelScoringParameters.getBuilder(this.config).createCharyparNagelScoringParameters();
		}
		if (this.prefs == null) {
			this.readPrefs(scenario);
		}
		
		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new RCActivityScoringFunction(person, params, scenario.getActivityFacilities(), 
				this.prefs));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

		return sumScoringFunction;
	}
	
	private void readPrefs(Scenario scenario) {
		log.info("reading prefs ...");
		this.prefs = new ObjectAttributes();
		for (ActivityParams activityParams : scenario.getConfig().planCalcScore().getActivityParams()) {
			log.info("activity param:" + activityParams.getActivityType());
			int counter = 0;
			int nextMsg = 1;
			for (Person p : scenario.getPopulation().getPersons().values()) {
				counter++;
				if (counter % nextMsg == 0) {
					nextMsg *= 2;
					log.info(" person # " + counter);
				}
				PersonImpl person = (PersonImpl)p;
				Desires desires = person.getDesires();					
				if (desires != null) {
					// hä? in the desires, only the typical duration can be specified. need to get the rest from the config anyway, or from where else?
					prefs.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getActivityType(), desires.getActivityDuration(activityParams.getActivityType()));
				} else {				
					prefs.putAttribute(p.getId().toString(), "typicalDuration_" + activityParams.getActivityType(), activityParams.getTypicalDuration());
					log.error("there should be desires!");
				}
				prefs.putAttribute(p.getId().toString(), "latestStartTime_" + activityParams.getActivityType(), activityParams.getLatestStartTime());
				prefs.putAttribute(p.getId().toString(), "earliestEndTime_" + activityParams.getActivityType(), activityParams.getEarliestEndTime());
				prefs.putAttribute(p.getId().toString(), "minimalDuration_" + activityParams.getActivityType(), activityParams.getMinimalDuration());
			}
		}
		log.info("Reading prefs finished");
	}
}
