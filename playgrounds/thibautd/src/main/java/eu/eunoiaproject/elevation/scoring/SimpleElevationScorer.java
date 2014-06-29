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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.SumScoringFunction.LegScoring;

import eu.eunoiaproject.elevation.ElevationProvider;

/**
 * @author thibautd
 */
public class SimpleElevationScorer implements LegScoring, ActivityScoring {
	private double score = 0;

	private final ElevationProvider<Id> elevationProvider;

	private final Collection<String> modes;
	private final double marginalUtilityOfDifferential_m;

	private boolean scoreNextArrival = false;
	private Activity lastAct;

	public SimpleElevationScorer(
			final SimpleElevationScorerConfigGroup config,
			final ElevationProvider<Id> elevationProvider ) {
		this( config.getMarginalUtilityOfDenivelation_m(),
				config.getModes(),
				elevationProvider );
	}

	public SimpleElevationScorer(
			final double marginalUtilityOfDifferential_m,
			final Collection<String> modes,
			final ElevationProvider<Id> elevationProvider ) {
		this.marginalUtilityOfDifferential_m = marginalUtilityOfDifferential_m;
		this.elevationProvider = elevationProvider;
		this.modes = modes;
	}

	@Override
	public void finish() { }

	@Override
	public double getScore() {
		return score;
	}

	@Override
	public void handleLeg(final Leg leg) {
		if ( modes.contains( leg.getMode() ) ) scoreNextArrival = true;
	}

	@Override
	public void handleActivity( final Activity act ) {
		if ( scoreNextArrival ) {
			scoreNextArrival = false;
			final double startAlt = elevationProvider.getAltitude( lastAct.getFacilityId() );
			final double endAlt = elevationProvider.getAltitude( act.getFacilityId() );
			this.score += score( startAlt , endAlt );
		}
		this.lastAct = act;
	}

	private double score(
			final double startAlt,
			final double endAlt) {
		return marginalUtilityOfDifferential_m * ( endAlt - startAlt ); 
	}

	@Override
	public void handleFirstActivity( final Activity act ) {
		handleActivity( act );
	}

	@Override
	public void handleLastActivity( final Activity act ) {
		handleActivity( act );
	}
}

