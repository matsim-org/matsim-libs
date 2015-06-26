/* *********************************************************************** *
 * project: org.matsim.*
 * LineChangeScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.scoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.SumScoringFunction.ArbitraryEventScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

/**
 * @author thibautd
 */
public class LineChangeScoringFunction implements ArbitraryEventScoring, ScoringFunctionAccumulator.ArbitraryEventScoring {
	private final LineChangeScoringParameters params;

	private double score = 0;
	private boolean nextEnterVehicleIsFirstOfTrip = true ;
	private boolean nextStartPtLegIsFirstOfTrip = true ;
	private boolean currentLegIsPtLeg = false;
	private double lastActivityEndTime = Time.UNDEFINED_TIME ;

	public LineChangeScoringFunction(final CharyparNagelScoringParameters config) {
		this( new LineChangeScoringParameters( config ) );
	}

	public LineChangeScoringFunction(final PlanCalcScoreConfigGroup config) {
		this( new LineChangeScoringParameters( config ) );
	}

	public LineChangeScoringFunction(final LineChangeScoringParameters params) {
		this.params = params;
	}

	@Override
	public void handleEvent(final Event event) {
		if ( event instanceof ActivityEndEvent ) {
			// When there is a "real" activity, flags are reset:
			if ( !PtConstants.TRANSIT_ACTIVITY_TYPE.equals( ((ActivityEndEvent)event).getActType()) ) {
				this.nextEnterVehicleIsFirstOfTrip  = true ;
				this.nextStartPtLegIsFirstOfTrip = true ;
			}
			this.lastActivityEndTime = event.getTime() ;
		}

		if ( event instanceof PersonEntersVehicleEvent && currentLegIsPtLeg ) {
			if ( !this.nextEnterVehicleIsFirstOfTrip ) {
				// all vehicle entering after the first triggers the disutility of line switch:
				this.score  += params.utilityOfLineSwitch ;
			}
			this.nextEnterVehicleIsFirstOfTrip = false ;
			// add score of waiting, _minus_ score of travelling (since it is added in the legscoring above):
			this.score += (event.getTime() - this.lastActivityEndTime) * (this.params.marginalUtilityOfWaiting_s - this.params.marginalUtilityOfTraveling_s) ;
		}

		if ( event instanceof PersonDepartureEvent ) {
			this.currentLegIsPtLeg = TransportMode.pt.equals( ((PersonDepartureEvent)event).getLegMode() );
			if ( currentLegIsPtLeg ) {
				if ( !this.nextStartPtLegIsFirstOfTrip ) {
					this.score -= params.constant ;
					// (yyyy deducting this again, since is it wrongly added above.  should be consolidated; this is so the code
					// modification is minimally invasive.  kai, dec'12)
				}
				this.nextStartPtLegIsFirstOfTrip = false ;
			}
		}
	}

	@Override
	public void finish() {}

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void reset() {
		score = 0;
		nextEnterVehicleIsFirstOfTrip = true ;
		nextStartPtLegIsFirstOfTrip = true ;
		currentLegIsPtLeg = false;
		lastActivityEndTime = Time.UNDEFINED_TIME ;
	}

	public static class LineChangeScoringParameters {
		private final double constant;
		private final double utilityOfLineSwitch;
		private final double marginalUtilityOfWaiting_s;
		private final double marginalUtilityOfTraveling_s;

		public LineChangeScoringParameters(
				final CharyparNagelScoringParameters config ) {
			this( config.modeParams.get( TransportMode.pt ).constant,
				config.utilityOfLineSwitch,
				config.marginalUtilityOfWaitingPt_s,
				config.modeParams.get( TransportMode.pt ).marginalUtilityOfTraveling_s );
		}
		public LineChangeScoringParameters(
				final PlanCalcScoreConfigGroup config ) {
			this( config.getModes().get( TransportMode.pt ).getConstant(),
				config.getUtilityOfLineSwitch(),
				config.getMarginalUtlOfWaitingPt_utils_hr() / 3600.,
				config.getModes().get( TransportMode.pt ).getMarginalUtilityOfTraveling() / 3600. );
		}

		public LineChangeScoringParameters(
				final double constant,
				final double utilityOfLineSwitch,
				final double marginalUtilityOfWaiting_s,
				final double marginalUtilityOfTraveling_s) {
			this.constant = constant;
			this.utilityOfLineSwitch = utilityOfLineSwitch;
			this.marginalUtilityOfWaiting_s = marginalUtilityOfWaiting_s;
			this.marginalUtilityOfTraveling_s = marginalUtilityOfTraveling_s;
		}
	}
}

