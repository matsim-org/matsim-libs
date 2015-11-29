/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleElevationScorer.java
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
package eu.eunoiaproject.elevation.scoring;

import eu.eunoiaproject.elevation.ElevationProvider;
import eu.eunoiaproject.elevation.scoring.SimpleElevationScorerParameters.Params;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;

/**
 * @author thibautd
 */
public class SimpleElevationScorer implements LegScoring, ActivityScoring {
	private double score = 0;

	private final ElevationProvider<Id> elevationProvider;

	private final SimpleElevationScorerParameters params;

	private String lastMode = null;
	private Activity lastAct;

	public SimpleElevationScorer(
			final SimpleElevationScorerParameters params,
			final ElevationProvider<Id> elevationProvider ) {
		this.params = params;
		this.elevationProvider = elevationProvider;
	}

	@Override
	public void finish() { }

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleLeg(final Leg leg) {
		lastMode = leg.getMode();
	}

	@Override
	public void handleActivity( final Activity act ) {
		if ( lastMode == null ) {
			lastAct = act;
			return;
		}

		final Params p = params.getParams( lastMode );
		if ( p != null ) {
			try {
				final double startAlt = elevationProvider.getAltitude( lastAct.getFacilityId() );
				final double endAlt = elevationProvider.getAltitude( act.getFacilityId() );
	
				final double dennivelation = endAlt - startAlt;
				this.score += (dennivelation > 0 ?
							p.marginalUtilityOfUphillDenivelation_m :
							p.marginalUtilityOfDownhillDenivelation_m )
						* Math.abs( dennivelation );
			}
			catch ( IllegalStateException e ) {
				throw new RuntimeException( "problem while scoring mode "+lastMode , e );
			}
		}
		this.lastMode = null;
		this.lastAct = act;
	}

	@Override
	public void handleFirstActivity( final Activity act ) {
		this.lastAct = act;
	}

	@Override
	public void handleLastActivity( final Activity act ) {
		handleActivity( act );
	}
}

