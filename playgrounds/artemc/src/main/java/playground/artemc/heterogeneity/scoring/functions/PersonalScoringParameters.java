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

package playground.artemc.heterogeneity.scoring.functions;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.pt.PtConstants;

public class PersonalScoringParameters implements MatsimParameters {
	
	public static class Mode {
		private Mode(
				double marginalUtilityOfTraveling_s,
				double marginalUtilityOfDistance_m,
				double monetaryDistanceCostRate,
				double constant) {
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
			this.marginalUtilityOfDistance_m = marginalUtilityOfDistance_m;
			this.monetaryDistanceCostRate = monetaryDistanceCostRate;
			this.constant = constant;
		}
		public double marginalUtilityOfTraveling_s;
		public double marginalUtilityOfDistance_m;
		public double monetaryDistanceCostRate;
		public final double constant;
	}
	
	public final Map<String, ActivityUtilityParameters> utilParams;
	public final Map<String, Mode> modeParams;
	public double marginalUtilityOfWaiting_s;
	public double marginalUtilityOfLateArrival_s;
	public double marginalUtilityOfEarlyDeparture_s;
	public double marginalUtilityOfWaitingPt_s;
	public double marginalUtilityOfPerforming_s;
	public double utilityOfLineSwitch ;
	public double marginalUtilityOfMoney;
	public double abortedPlanScore;
	public boolean scoreActs;
	
	public final boolean usingOldScoringBelowZeroUtilityDuration ;

	public PersonalScoringParameters(final PlanCalcScoreConfigGroup config) {
		this.usingOldScoringBelowZeroUtilityDuration = config.isUsingOldScoringBelowZeroUtilityDuration() ;
		
		marginalUtilityOfWaiting_s = config.getMarginalUtlOfWaiting_utils_hr() / 3600.0;
		marginalUtilityOfLateArrival_s = config.getLateArrival_utils_hr() / 3600.0;
		marginalUtilityOfEarlyDeparture_s = config.getEarlyDeparture_utils_hr() / 3600.0;
		marginalUtilityOfWaitingPt_s = config.getMarginalUtlOfWaitingPt_utils_hr() / 3600.0 ;
		marginalUtilityOfPerforming_s = config.getPerforming_utils_hr() / 3600.0;
		utilityOfLineSwitch = config.getUtilityOfLineSwitch() ;
		marginalUtilityOfMoney = config.getMarginalUtilityOfMoney() ;
		scoreActs = marginalUtilityOfPerforming_s != 0 || marginalUtilityOfWaiting_s != 0 ||
				marginalUtilityOfLateArrival_s != 0 || marginalUtilityOfEarlyDeparture_s != 0;

		SortedMap<String,ActivityUtilityParameters> tmpUtlParams = new TreeMap<String, ActivityUtilityParameters>() ;
		for (ActivityParams params : config.getActivityParams()) {
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder(params) ;
			// the following was introduced in nov'12.  Also see setupTransitSimulation in Controler.  kai, nov'12
			if (params.getActivityType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
				factory.setScoreAtAll(false) ;
			}
			tmpUtlParams.put(params.getActivityType(), factory.build() ) ;
		}
		utilParams = Collections.unmodifiableMap(tmpUtlParams );
		
		SortedMap<String, Mode> tmpModeParams = new TreeMap<String, Mode>() ;
		Map<String, ModeParams> modes = config.getModes();
		double worstMarginalUtilityOfTraveling_s = 0.0;
		for (Entry<String, ModeParams> mode : modes.entrySet()) {
			String modeName = mode.getKey();
			ModeParams modeParams = mode.getValue();
			double marginalUtilityOfTraveling_s = modeParams.getMarginalUtilityOfTraveling() / 3600.0;
			worstMarginalUtilityOfTraveling_s = Math.min(worstMarginalUtilityOfTraveling_s, marginalUtilityOfTraveling_s);
			double marginalUtilityOfDistance_m = modeParams.getMarginalUtilityOfDistance();
			double monetaryDistanceRate = modeParams.getMonetaryDistanceRate();
			double constant = modeParams.getConstant();
			Mode newModeParams = new Mode(
					marginalUtilityOfTraveling_s,
					marginalUtilityOfDistance_m,
					monetaryDistanceRate,
					constant);
			tmpModeParams.put(modeName, newModeParams);
		}
		modeParams = Collections.unmodifiableMap(tmpModeParams);
		
		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival_s, marginalUtilityOfEarlyDeparture_s),
				Math.min(worstMarginalUtilityOfTraveling_s-marginalUtilityOfPerforming_s, marginalUtilityOfWaiting_s-marginalUtilityOfPerforming_s)
				) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)
		// This rather complicated definition has to do with the fact that exp(some_large_number) relatively quickly becomes Inf.
		// In consequence, the abortedPlanScore needs to be more strongly negative than anything else, but not much more.  
		// kai, feb'12
	}

}
