/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacility.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.matsim.facilities.Facility;

/**
 * Defines a bike sharing station: something with an Id,
 * a location (coord + link), a capacity and an initial
 * number of bikes.
 * @author thibautd
 */
public interface BikeSharingFacility extends Facility<BikeSharingFacility> {
	public int getCapacity();
	public int getInitialNumberOfBikes();
}

