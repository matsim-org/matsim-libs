/* *********************************************************************** *
 * project: org.matsim.*
 * BlackListedActivityScoringFunction.java
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
package org.matsim.contrib.socnetsim.jointtrips.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.SumScoringFunction;

/**
 * @author thibautd
 */
public class BlackListedActivityScoringFunction implements SumScoringFunction.ActivityScoring, ScoringFunctionAccumulator.ActivityScoring {
	private final SumScoringFunction.ActivityScoring delegate;
	private final ScoringFunctionAccumulator.ActivityScoring accDelegate;

	private final StageActivityTypes blackList;

	public BlackListedActivityScoringFunction(
			final StageActivityTypes blackList,
			final SumScoringFunction.ActivityScoring delegate) {
		this.blackList = blackList;
		this.delegate = delegate;
		this.accDelegate = delegate instanceof ScoringFunctionAccumulator.ActivityScoring ?
			(ScoringFunctionAccumulator.ActivityScoring) delegate :
			null;
	}

	@Override
	public void handleFirstActivity(Activity act) {
		if ( blackList.isStageActivity( act.getType() ) ) return;
		delegate.handleFirstActivity( act);
	}

	@Override
	public void handleActivity(Activity act) {
		if ( blackList.isStageActivity( act.getType() ) ) return;
		delegate.handleActivity( act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		if ( blackList.isStageActivity( act.getType() ) ) return;
		delegate.handleLastActivity( act);
	}

	@Override
	public void finish() {
		delegate.finish();
	}

	@Override
	public double getScore() {
		return delegate.getScore();
	}


	@Override
	@Deprecated
	public void reset() {
		if ( accDelegate != null ) {
			accDelegate.reset();
		}
		else throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void startActivity(double time, Activity act) {
		if ( accDelegate != null ) {
			if ( blackList.isStageActivity( act.getType() ) ) return;
			accDelegate.startActivity( time , act );
		}
		else throw new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public void endActivity(double time, Activity act) {
		if ( accDelegate != null ) {
			if ( blackList.isStageActivity( act.getType() ) ) return;
			accDelegate.endActivity( time , act );
		}
		else throw new UnsupportedOperationException();
	}
}

