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
package playground.ivt.scoring;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scoring.ScoringFunctionAccumulator.ActivityScoring;

/**
 * @author thibautd
 */
public class BlackListedActivityScoringFunction implements ActivityScoring {
	private final ActivityScoring delegate;
	private final StageActivityTypes blackList;

	public BlackListedActivityScoringFunction(
			final StageActivityTypes blackList,
			final ActivityScoring delegate) {
		this.blackList = blackList;
		this.delegate = delegate;
	}

	@Override
	public void startActivity(double time, Activity act) {
		if ( blackList.isStageActivity( act.getType() ) ) return;
		delegate.startActivity(time, act);
	}

	@Override
	public void endActivity(double time, Activity act) {
		if ( blackList.isStageActivity( act.getType() ) ) return;
		delegate.endActivity(time, act);
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
	public void reset() {
		delegate.reset();
	}
}

