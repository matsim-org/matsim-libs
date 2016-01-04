/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.router.transitastarlandmarks;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author thibautd
 */
public class TransitRouterAStarConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger( TransitRouterAStarConfigGroup.class );
	public static final String GROUP_NAME = "transitRouterAStar";

	public enum LandmarkComputation { degree, pieSlice, centrality; }

	private int nLandmarks = 16;
	private int initiallyActiveLandmarks = 2;
	private double overdoFactor = 1;
	private LandmarkComputation landmarkComputation = LandmarkComputation.degree;

	public TransitRouterAStarConfigGroup( ) {
		super( GROUP_NAME );
	}

	@StringGetter( "initiallyActiveLandmarks" )
	public int getInitiallyActiveLandmarks() {
		return initiallyActiveLandmarks;
	}

	@StringSetter( "initiallyActiveLandmarks" )
	public void setInitiallyActiveLandmarks( int initiallyActiveLandmarks ) {
		if ( initiallyActiveLandmarks < 1 ) {
			log.warn( "number of active landmarks below 1 makes no sense! Keeping former value "+this.initiallyActiveLandmarks );
			return;
		}
		this.initiallyActiveLandmarks = initiallyActiveLandmarks;
	}

	@StringGetter( "nLandmarks" )
	public int getNLandmarks() {
		return nLandmarks;
	}

	@StringSetter( "nLandmarks" )
	public void setNLandmarks( int nLandmarks ) {
		if ( nLandmarks < 1 ) {
			log.warn( "number of landmarks below 1 makes no sense! Keeping former value "+this.nLandmarks );
			return;
		}
		this.nLandmarks = nLandmarks;
	}

	@StringGetter( "overdoFactor" )
	public double getOverdoFactor() {
		return overdoFactor;
	}

	@StringSetter( "overdoFactor" )
	public void setOverdoFactor( double overdoFactor ) {
		if ( overdoFactor < 1 ) {
			log.warn( "Overdo factor below 1 makes no sense! Keeping former value "+this.overdoFactor );
			return;
		}
		this.overdoFactor = overdoFactor;
	}

	@StringGetter( "landmarkComputation" )
	public LandmarkComputation getLandmarkComputation() {
		return landmarkComputation;
	}

	@StringSetter( "landmarkComputation" )
	public void setLandmarkComputation( LandmarkComputation landmarkComputation ) {
		this.landmarkComputation = landmarkComputation;
	}
}
