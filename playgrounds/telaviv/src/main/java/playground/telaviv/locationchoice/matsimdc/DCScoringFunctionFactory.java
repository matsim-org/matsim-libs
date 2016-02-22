/* *********************************************************************** *
 * project: org.matsim.*
 * DCScoringFunctionFactory.java
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

package playground.telaviv.locationchoice.matsimdc;

import java.util.HashMap;
import java.util.Map;
//import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.core.config.Config;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.facilities.FacilitiesCreator;
import playground.telaviv.locationchoice.CalculateDestinationChoice;

public class DCScoringFunctionFactory implements ScoringFunctionFactory {
	private final MatsimServices controler;
	private DestinationChoiceBestResponseContext dcContext;
	private Config config;	
//	private int iteration = -1;
	private Map<Id, Integer> facilityToZoneIndexMap;
	private CalculateDestinationChoice dcCalculator;
	private final static Logger log = Logger.getLogger(DCScoringFunctionFactory.class);
	private boolean initialized = false;

	private CharyparNagelScoringParameters params = null;
	
	public DCScoringFunctionFactory(Config config, MatsimServices controler, DestinationChoiceBestResponseContext dcContext) {
		this.controler = controler;
		this.dcContext = dcContext;
		this.config = config;
		log.info("creating DCScoringFunctionFactory");
	}
	
	private void initialize() {
		// should not play a role here when iteration number is exactly set. 
		
//		not necessary anymore as the constant factors do not change over the iterations:
//		if (this.iteration != this.controler.getIterationNumber()) {
//			this.iteration = this.controler.getIterationNumber();
		
			// it is actually a map Id -> Zone Index (and NOT taz!)
			// the indices have the same order as the taz values but are enumerated from 0 upwards
			this.facilityToZoneIndexMap = new HashMap<Id, Integer>();
		
			Scenario scenario = this.dcContext.getScenario();
			ObjectAttributes facilitiesAttributes = scenario.getActivityFacilities().getFacilityAttributes();
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				Object value = facilitiesAttributes.getAttribute(facility.getId().toString(), FacilitiesCreator.indexObjectAttributesName);
				if (value != null) {
					this.facilityToZoneIndexMap.put(facility.getId(),(Integer) value);
				}
			}
			
			this.dcCalculator = new CalculateDestinationChoice(this.dcContext.getScenario());
			this.dcCalculator.calculateVTODForDCModule();

		this.params = new CharyparNagelScoringParameters.Builder(this.config.planCalcScore(), this.config.planCalcScore().getScoringParameters(null), this.controler.getConfig().scenario()).build();
			
			// actually not necessary here:
//			this.dcCalculator.calculateDynamicFactors(
//					new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(this.dcContext.getScenario().getNetwork(), 
//							this.dcContext.getScenario().getConfig().travelTimeCalculator()).getLinkTravelTimes());
//					
//			this.dcCalculator.calculateTotalFactors();			
//		}
	}
		
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {
		if (!this.initialized) {
			this.initialize();
			this.initialized = true;
		}
		
		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();		
		CharyparNagelActivityScoring activityScoringFunction = new DCActivityScoringFunction(

//					(PlanImpl) person,
					person.getSelectedPlan(),
					// I found "(PlanImpl) person".  This could never have worked.  But when making PlanImpl final, it does not even
					// compile any more.  I am thus speculating that person.getSelectedPlan() was meant.  kai, nov'15
					
					this.dcContext.getFacilityPenalties(), 
					dcContext,
					this.facilityToZoneIndexMap,
					this.dcCalculator);
		scoringFunctionAccumulator.addScoringFunction(activityScoringFunction);
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelMoneyScoring(params));
		return scoringFunctionAccumulator;
		
//		ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
//		
//		CharyparNagelActivityScoring scoringFunction = new DCActivityScoringFunction(
//					(PlanImpl)plan, 
//					this.dcContext.getFacilityPenalties(), 
//					dcContext,
//					this.linkToZoneMap,
//					this.dcCalculator);
//		scoringFunctionAccumulator.addScoringFunction(scoringFunction);		
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(new CharyparNagelScoringParameters(config.planCalcScore()), controler.getNetwork()));
//		scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(new CharyparNagelScoringParameters(config.planCalcScore())));
//		return scoringFunctionAccumulator;
	}
}
