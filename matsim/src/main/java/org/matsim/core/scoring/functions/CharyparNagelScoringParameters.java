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

	public final Map<String, ActivityUtilityParameters> utilParams;
	public final Map<String, ModeUtilityParameters> modeParams;
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
			final Map<String, ModeUtilityParameters> modeParams,
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

	public static final class Builder {
		private final Map<String, ActivityUtilityParameters.Builder> utilParams;
		private final Map<String, ModeUtilityParameters.Builder> modeParams;

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

		public Builder(
				final Scenario scenario,
				final Id<Person> person ) {
			this(
					scenario.getConfig().planCalcScore(),
					scenario.getConfig().planCalcScore().getScoringParameters(
							(String)
									scenario.getPopulation().getPersonAttributes().getAttribute(
											person.toString(),
											scenario.getConfig().plans().getSubpopulationAttributeName() ) ),
					scenario.getConfig().scenario() );
		}

		public Builder(
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
				utilParams.put(params.getActivityType(), factory ) ;
			}

			modeParams = new TreeMap<>() ;
			Map<String, PlanCalcScoreConfigGroup.ModeParams> modes = scoringParameterSet.getModes();
			double worstMarginalUtilityOfTraveling_s = 0.0;
			for (Map.Entry<String, PlanCalcScoreConfigGroup.ModeParams> mode : modes.entrySet()) {
				String modeName = mode.getKey();
				ModeParams params = mode.getValue();
				worstMarginalUtilityOfTraveling_s = Math.min(worstMarginalUtilityOfTraveling_s, params.getMarginalUtilityOfTraveling() / 3600. );
				modeParams.put(modeName, new ModeUtilityParameters.Builder( params ) );
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

		public Builder setActivityParameters(String activityType, ActivityUtilityParameters.Builder params) {
			this.utilParams.put( activityType , params );
			return this;
		}

		public ActivityUtilityParameters.Builder getActivityParameters(String activityType) {
			return this.utilParams.get( activityType );
		}

		public Builder setModeParameters(String mode, ModeUtilityParameters.Builder params) {
			this.modeParams.put( mode , params );
			return this;
		}

		public ModeUtilityParameters.Builder getModeParameters(String mode) {
			return this.modeParams.get( mode  );
		}

		public Builder setMarginalUtilityOfWaiting_s(double marginalUtilityOfWaiting_s) {
			this.marginalUtilityOfWaiting_s = marginalUtilityOfWaiting_s;
			return this;
		}

		public Builder setMarginalUtilityOfLateArrival_s(double marginalUtilityOfLateArrival_s) {
			this.marginalUtilityOfLateArrival_s = marginalUtilityOfLateArrival_s;
			return this;
		}

		public Builder setMarginalUtilityOfEarlyDeparture_s(double marginalUtilityOfEarlyDeparture_s) {
			this.marginalUtilityOfEarlyDeparture_s = marginalUtilityOfEarlyDeparture_s;
			return this;
		}

		public Builder setMarginalUtilityOfWaitingPt_s(double marginalUtilityOfWaitingPt_s) {
			this.marginalUtilityOfWaitingPt_s = marginalUtilityOfWaitingPt_s;
			return this;
		}

		public Builder setMarginalUtilityOfPerforming_s(double marginalUtilityOfPerforming_s) {
			this.marginalUtilityOfPerforming_s = marginalUtilityOfPerforming_s;
			return this;
		}

		public Builder setUtilityOfLineSwitch(double utilityOfLineSwitch) {
			this.utilityOfLineSwitch = utilityOfLineSwitch;
			return this;
		}

		public Builder setMarginalUtilityOfMoney(double marginalUtilityOfMoney) {
			this.marginalUtilityOfMoney = marginalUtilityOfMoney;
			return this;
		}

		public Builder setAbortedPlanScore(double abortedPlanScore) {
			this.abortedPlanScore = abortedPlanScore;
			return this;
		}

		public Builder setScoreActs(boolean scoreActs) {
			this.scoreActs = scoreActs;
			return this;
		}

		public Builder setUsingOldScoringBelowZeroUtilityDuration(boolean usingOldScoringBelowZeroUtilityDuration) {
			this.usingOldScoringBelowZeroUtilityDuration = usingOldScoringBelowZeroUtilityDuration;
			return this;
		}

		public CharyparNagelScoringParameters build() {
			final Map<String, ModeUtilityParameters> modes = new TreeMap<>();
			for ( Map.Entry<String, ModeUtilityParameters.Builder> e : modeParams.entrySet() ) {
				modes.put( e.getKey() , e.getValue().build() );
			}

			final Map<String, ActivityUtilityParameters> acts = new TreeMap<>();
			for ( Map.Entry<String, ActivityUtilityParameters.Builder> e : utilParams.entrySet() ) {
				acts.put( e.getKey() , e.getValue().build() );
			}

			return new CharyparNagelScoringParameters(
					acts,
					modes,
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
