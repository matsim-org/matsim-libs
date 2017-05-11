/* *********************************************************************** *
 * project: org.matsim.*
 * KTIYear3ScoringFunctionFactory.java
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

package playground.meisterk.kti.scoring;

class KTIYear3ScoringFunctionFactory {
	
}

//public class KTIYear3ScoringFunctionFactory extends org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory {
//
//    private final Scenario scenario;
//	private final KtiConfigGroup ktiConfigGroup;
//	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
//	private final ActivityFacilities facilities;
//	private Config config;
//
//	public KTIYear3ScoringFunctionFactory(
//			final Scenario scenario,
//			final KtiConfigGroup ktiConfigGroup,
//			final TreeMap<Id, FacilityPenalty> facilityPenalties,
//			final ActivityFacilities facilities) {
//		super(scenario.getConfig().planCalcScore(), scenario.getConfig().scenario(), scenario.getNetwork());
//		this.scenario = scenario;
//		this.ktiConfigGroup = ktiConfigGroup;
//		this.facilityPenalties = facilityPenalties;
//		this.facilities = facilities;
//		this.config=scenario.getConfig();
//	}
//
//	@Override
//	public ScoringFunction createNewScoringFunction(Person person) {
//
//		SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
//
//		scoringFunctionAccumulator.addScoringFunction(new ActivityScoringFunction(
//				person.getSelectedPlan(),
//				CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create(),
//				this.facilityPenalties,
//				this.facilities));
//		scoringFunctionAccumulator.addScoringFunction(new LegScoringFunction(
//				person.getSelectedPlan(),
//				CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create(),
//				scenario.getConfig(),
//                scenario.getNetwork(),
//				this.ktiConfigGroup));
//		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelMoneyScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
//		scoringFunctionAccumulator.addScoringFunction(new org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring(CharyparNagelScoringParameters.getBuilder(config.planCalcScore(), config.scenario()).create()));
//
//		return scoringFunctionAccumulator;
//	}
//
//}
