/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringParameters.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;

public class CharyparNagelScoringParameters implements MatsimParameters {

	public static class Mode {
		public Mode(
				double marginalUtilityOfTraveling_s,
				double marginalUtilityOfDistance_m,
				double monetaryDistanceCostRate,
				double constant) {
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
			this.monetaryDistanceCostRate = monetaryDistanceCostRate;
			this.constant = constant;
		}
		public final double marginalUtilityOfTraveling_s;
		public final double marginalUtilityOfDistance_m;
		public final double monetaryDistanceCostRate;
		public final double constant;
	}
	
	public final Map<String, ActivityUtilityParameters> utilParams;
	public final Map<String, Mode> modeParams;
	public final double marginalUtilityOfWaiting_s;
	public final double marginalUtilityOfLateArrival_s;
	public final double marginalUtilityOfEarlyDeparture_s;
	public final double marginalUtilityOfWaitingPt_s;
	public final double marginalUtilityOfPerforming_s;
	public final double utilityOfLineSwitch ;
	public final double marginalUtilityOfMoney;
	public final double abortedPlanScore;
	public final boolean scoreActs;
	
	public final boolean usingOldScoringBelowZeroUtilityDuration ;
	
	public final int simulationPeriodInDays;

	private CharyparNagelScoringParameters(
			final Map<String, ActivityUtilityParameters> utilParams,
			final Map<String, Mode> modeParams,
			final double marginalUtilityOfWaiting_s,
			final double marginalUtilityOfLateArrival_s,
			final double marginalUtilityOfEarlyDeparture_s,
			final double marginalUtilityOfWaitingPt_s,
			final double marginalUtilityOfPerforming_s,
			final double utilityOfLineSwitch,
			final double marginalUtilityOfMoney,
			final double abortedPlanScore,
			final boolean scoreActs,
			final boolean usingOldScoringBelowZeroUtilityDuration,
			final int simulationPeriodInDays) {
		this.utilParams = utilParams;
		this.modeParams = modeParams;
		this.marginalUtilityOfWaiting_s = marginalUtilityOfWaiting_s;
		this.marginalUtilityOfLateArrival_s = marginalUtilityOfLateArrival_s;
		this.marginalUtilityOfEarlyDeparture_s = marginalUtilityOfEarlyDeparture_s;
		this.marginalUtilityOfWaitingPt_s = marginalUtilityOfWaitingPt_s;
		this.marginalUtilityOfPerforming_s = marginalUtilityOfPerforming_s;
		this.utilityOfLineSwitch = utilityOfLineSwitch;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.abortedPlanScore = abortedPlanScore;
		this.scoreActs = scoreActs;
		this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
		this.simulationPeriodInDays = simulationPeriodInDays;
	}

	public static CharyparNagelScoringParametersBuilder getBuilder(
			final Scenario scenario,
			final Id<Person> person ) {
		return new CharyparNagelScoringParametersBuilder(
				scenario.getConfig().planCalcScore(),
				scenario.getConfig().planCalcScore().getScoringParameters(
						(String)
						scenario.getPopulation().getPersonAttributes().getAttribute(
								person.toString(),
								scenario.getConfig().plans().getSubpopulationAttributeName() ) ),
				scenario.getConfig().scenario() );
	}

	public static CharyparNagelScoringParametersBuilder getBuilder(
			final PlanCalcScoreConfigGroup config,
			final PlanCalcScoreConfigGroup.ScoringParameterSet scoringParameterSet,
			final ScenarioConfigGroup scenarioConfig) {
		return new CharyparNagelScoringParametersBuilder( config, scoringParameterSet, scenarioConfig );
	}

	public static final class CharyparNagelScoringParametersBuilder {
		private final Map<String, ActivityUtilityParameters> utilParams;
		private final Map<String, Mode> modeParams;

		private double marginalUtilityOfWaiting_s;
		private double marginalUtilityOfLateArrival_s;
		private double marginalUtilityOfEarlyDeparture_s;
		private double marginalUtilityOfWaitingPt_s;
		private double marginalUtilityOfPerforming_s;
		private double utilityOfLineSwitch;
		private double marginalUtilityOfMoney;
		private double abortedPlanScore;
		private boolean scoreActs;
		private boolean usingOldScoringBelowZeroUtilityDuration;
		private int simulationPeriodInDays = 1;

		private CharyparNagelScoringParametersBuilder(
				final PlanCalcScoreConfigGroup configGroup,
				final PlanCalcScoreConfigGroup.ScoringParameterSet scoringParameterSet,
				final ScenarioConfigGroup scenarioConfig) {
			this.simulationPeriodInDays = scenarioConfig.getSimulationPeriodInDays();
			
			this.usingOldScoringBelowZeroUtilityDuration = configGroup.isUsingOldScoringBelowZeroUtilityDuration() ;

			marginalUtilityOfWaiting_s = scoringParameterSet.getMarginalUtlOfWaiting_utils_hr() / 3600.0;
			marginalUtilityOfLateArrival_s = scoringParameterSet.getLateArrival_utils_hr() / 3600.0;
			marginalUtilityOfEarlyDeparture_s = scoringParameterSet.getEarlyDeparture_utils_hr() / 3600.0;
			marginalUtilityOfWaitingPt_s = scoringParameterSet.getMarginalUtlOfWaitingPt_utils_hr() / 3600.0 ;
			marginalUtilityOfPerforming_s = scoringParameterSet.getPerforming_utils_hr() / 3600.0;
			utilityOfLineSwitch = scoringParameterSet.getUtilityOfLineSwitch() ;
			marginalUtilityOfMoney = scoringParameterSet.getMarginalUtilityOfMoney() ;
			scoreActs = marginalUtilityOfPerforming_s != 0 || marginalUtilityOfWaiting_s != 0 ||
					marginalUtilityOfLateArrival_s != 0 || marginalUtilityOfEarlyDeparture_s != 0;

			utilParams = new TreeMap<>() ;
			for (ActivityParams params : scoringParameterSet.getActivityParams()) {
				ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params) ;
				utilParams.put(params.getActivityType(), factory.create() ) ;
			}

			modeParams = new TreeMap<String, Mode>() ;
			Map<String, PlanCalcScoreConfigGroup.ModeParams> modes = scoringParameterSet.getModes();
			double worstMarginalUtilityOfTraveling_s = 0.0;
			for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> mode : modes.entrySet()) {
				String modeName = mode.getKey();
				ModeParams params = mode.getValue();
				double marginalUtilityOfTraveling_s = params.getMarginalUtilityOfTraveling() / 3600.0;
				worstMarginalUtilityOfTraveling_s = Math.min(worstMarginalUtilityOfTraveling_s, marginalUtilityOfTraveling_s);
				double marginalUtilityOfDistance_m = params.getMarginalUtilityOfDistance();
				double monetaryDistanceRate = params.getMonetaryDistanceRate();
				double constant = params.getConstant();
				Mode newModeParams = new Mode(
						marginalUtilityOfTraveling_s,
						marginalUtilityOfDistance_m,
						monetaryDistanceRate,
						constant);
				modeParams.put(modeName, newModeParams);
			}

			abortedPlanScore = Math.min(
					Math.min(marginalUtilityOfLateArrival_s, marginalUtilityOfEarlyDeparture_s),
					Math.min(worstMarginalUtilityOfTraveling_s-marginalUtilityOfPerforming_s, marginalUtilityOfWaiting_s-marginalUtilityOfPerforming_s)
					) * 3600.0 * 24.0; // SCENARIO_DURATION
			// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)
			// This rather complicated definition has to do with the fact that exp(some_large_number) relatively quickly becomes Inf.
			// In consequence, the abortedPlanScore needs to be more strongly negative than anything else, but not much more.
			// kai, feb'12
		}

		public CharyparNagelScoringParametersBuilder withActivityParameters(String activityType, ActivityUtilityParameters params) {
			this.utilParams.put( activityType , params );
			return this;
		}

		public CharyparNagelScoringParametersBuilder withModeParameters(String mode, Mode params) {
			this.modeParams.put( mode , params );
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfWaiting_s(double marginalUtilityOfWaiting_s) {
			this.marginalUtilityOfWaiting_s = marginalUtilityOfWaiting_s;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfLateArrival_s(double marginalUtilityOfLateArrival_s) {
			this.marginalUtilityOfLateArrival_s = marginalUtilityOfLateArrival_s;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfEarlyDeparture_s(double marginalUtilityOfEarlyDeparture_s) {
			this.marginalUtilityOfEarlyDeparture_s = marginalUtilityOfEarlyDeparture_s;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfWaitingPt_s(double marginalUtilityOfWaitingPt_s) {
			this.marginalUtilityOfWaitingPt_s = marginalUtilityOfWaitingPt_s;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfPerforming_s(double marginalUtilityOfPerforming_s) {
			this.marginalUtilityOfPerforming_s = marginalUtilityOfPerforming_s;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withUtilityOfLineSwitch(double utilityOfLineSwitch) {
			this.utilityOfLineSwitch = utilityOfLineSwitch;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
			this.marginalUtilityOfMoney = marginalUtilityOfMoney;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withAbortedPlanScore(double abortedPlanScore) {
			this.abortedPlanScore = abortedPlanScore;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withScoreActs(boolean scoreActs) {
			this.scoreActs = scoreActs;
			return this;
		}

		public CharyparNagelScoringParametersBuilder withUsingOldScoringBelowZeroUtilityDuration(boolean usingOldScoringBelowZeroUtilityDuration) {
			this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
			return this;
		}

		public CharyparNagelScoringParameters create() {
			return new CharyparNagelScoringParameters(
					Collections.unmodifiableMap( utilParams ),
					Collections.unmodifiableMap( modeParams ),
					marginalUtilityOfWaiting_s,
					marginalUtilityOfLateArrival_s,
					marginalUtilityOfEarlyDeparture_s,
					marginalUtilityOfWaitingPt_s,
					marginalUtilityOfPerforming_s,
					utilityOfLineSwitch,
					marginalUtilityOfMoney,
					abortedPlanScore,
					scoreActs,
					usingOldScoringBelowZeroUtilityDuration,
					this.simulationPeriodInDays);
		}
	}
}
