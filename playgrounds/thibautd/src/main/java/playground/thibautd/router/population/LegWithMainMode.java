/* *********************************************************************** *
 * project: org.matsim.*
 * LegWithMainMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router.population;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;

/**
 * @author thibautd
 */
public class LegWithMainMode implements Leg {
	private final Leg delegate;
	private String mainMode = null;

	public LegWithMainMode(final Leg leg) {
		if (leg instanceof LegWithMainMode) {
			delegate = ((LegWithMainMode) leg).delegate;
			this.mainMode = ((LegWithMainMode) leg).mainMode;
		}
		else {
			this.delegate = leg;
		}
	}

	/**
	 * @return the last value set by {@link #setMainMode}, if it was not null;
	 * the result of {@link #getMode()} otherwise.
	 */
	public String getMainMode() {
		return mainMode != null ? mainMode : getMode();
	}

	public void setMainMode(final String mainMode) {
		this.mainMode = mainMode;
	}

	// /////////////////////////////////////////////////////////////////////////
	// delegate methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public String getMode() {
		return delegate.getMode();
	}

	@Override
	public void setMode(final String mode) {
		delegate.setMode( mode );
	}

	@Override
	public Route getRoute() {
		return delegate.getRoute();
	}

	@Override
	public void setRoute(final Route route) {
		delegate.setRoute( route );
	}

	@Override
	public double getDepartureTime() {
		return delegate.getDepartureTime();
	}

	@Override
	public void setDepartureTime(final double seconds) {
		delegate.setDepartureTime( seconds );
	}

	@Override
	public double getTravelTime() {
		return delegate.getTravelTime();
	}

	@Override
	public void setTravelTime(final double seconds) {
		delegate.setTravelTime( seconds );
	}

	public String toString() {
		return "legWithMainMode: mainMode="+mainMode+", leg="+delegate;
	}
}

