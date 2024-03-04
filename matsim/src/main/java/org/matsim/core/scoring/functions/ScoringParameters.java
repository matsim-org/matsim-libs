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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;
import java.util.TreeMap;

public class ScoringParameters implements MatsimParameters {

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

	public final double simulationPeriodInDays;

	private ScoringParameters(
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
			final double simulationPeriodInDays) {
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
		private final Map<String, ActivityUtilityParameters> utilParams;
		private final Map<String, ModeUtilityParameters> modeParams;

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
		private double simulationPeriodInDays = 1.0;

		// This is an error-prone design.  In the original design, there were defensive copies of the activity parameters for each individual
		// person.  So in situations where there are many activity parameters (e.g. for home_30, home_60, home_120, etc.), this resulted in
		// N(activityTypes) x N(persons) many objects of type ActivityUtilityParameters, which became fairly large.  Tilmann has now changed
		// it such that the ActivityUtilityParameters can be externally set, meaning that one can externally program something that re-uses
		// them. However, one still has to pass the embedding objects ScoringParameters and PlanCalcScoreConfig group, since one can also set
		// things at that upper level.  Conceptually, one _always_ makes a copy of PlanCalcScoreConfig group, but then partially fills it with
		// references to already existing objects (i.e. the ActivityUtilityParameters).  However, the code design does not make this very
		// clear.  kai, may'22

		@Deprecated
		public Builder(
				final Scenario scenario,
				final Person person ) {
			this(
					scenario.getConfig().scoring(),
					scenario.getConfig().scoring().getScoringParameters( PopulationUtils.getSubpopulation( person ) ),
					scenario.getConfig().scenario() );
		}

		/**
		 * This constructor makes defensive copies of both the activity and the mode params.
		 * Rather use the other constructor.
		 *
		 * @param configGroup
		 * @param scoringParameterSet
		 * @param scenarioConfig
		 */
		@Deprecated
		public Builder(
				final ScoringConfigGroup configGroup,
				final ScoringConfigGroup.ScoringParameterSet scoringParameterSet,
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
				utilParams.put(params.getActivityType(), factory.build() ) ;
			}

			modeParams = new TreeMap<>() ;
			Map<String, ScoringConfigGroup.ModeParams> modes = scoringParameterSet.getModes();
			double worstMarginalUtilityOfTraveling_s = 0.0;
			for (Map.Entry<String, ScoringConfigGroup.ModeParams> mode : modes.entrySet()) {
				String modeName = mode.getKey();
				ModeParams params = mode.getValue();
				worstMarginalUtilityOfTraveling_s = Math.min(worstMarginalUtilityOfTraveling_s, params.getMarginalUtilityOfTraveling() / 3600. );
				modeParams.put(modeName, new ModeUtilityParameters.Builder( params ).build() );
			}

			abortedPlanScore = Math.min(
					Math.min(marginalUtilityOfLateArrival_s, marginalUtilityOfEarlyDeparture_s),
					Math.min(worstMarginalUtilityOfTraveling_s-marginalUtilityOfPerforming_s, marginalUtilityOfWaiting_s-marginalUtilityOfPerforming_s)
					) * 3600.0 * 24.0; // SCENARIO_DURATION
			// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)
			// This rather complicated definition has to do with the fact that exp(some_large_number) relatively quickly becomes Inf.
			// In consequence, the abortedPlanScore needs to be more strongly negative than anything else, but not much more.
			// kai, feb'12
			// yyyy given that there is now this.simulationPeriodInDays, one could just multiply with that.  Will probably fail a number
			// of tests, thus I am not doing it right now.  kai, may'22
		}


		/**
		 * This constructor does not make defensive copies of the activity params but it makes copies of the mode params
		 *
		 * @param configGroup
		 * @param scoringParameterSet
		 * @param scenarioConfig
		 */
		public Builder(
				final ScoringConfigGroup configGroup,
				final ScoringConfigGroup.ScoringParameterSet scoringParameterSet,
				Map<String, ActivityUtilityParameters> activityParams,
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

			utilParams = activityParams;

			modeParams = new TreeMap<>() ;
			Map<String, ScoringConfigGroup.ModeParams> modes = scoringParameterSet.getModes();
			double worstMarginalUtilityOfTraveling_s = 0.0;
			for (Map.Entry<String, ScoringConfigGroup.ModeParams> mode : modes.entrySet()) {
				String modeName = mode.getKey();
				ModeParams params = mode.getValue();
				worstMarginalUtilityOfTraveling_s = Math.min(worstMarginalUtilityOfTraveling_s, params.getMarginalUtilityOfTraveling() / 3600. );
				modeParams.put(modeName, new ModeUtilityParameters.Builder( params ).build() );
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

		public Builder setActivityParameters(String activityType, ActivityUtilityParameters params) {
			this.utilParams.put( activityType , params );
			return this;
		}

		public ActivityUtilityParameters getActivityParameters(String activityType) {
			return this.utilParams.get( activityType );
		}

		public Builder setModeParameters(String mode, ModeUtilityParameters params) {
			this.modeParams.put( mode , params );
			return this;
		}

		public ModeUtilityParameters getModeParameters(String mode) {
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

		public ScoringParameters build() {
			return new ScoringParameters(
					utilParams,
					modeParams,
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
