/* *********************************************************************** *
 * project: org.matsim.*
 * StatefulBikeSharingFacility.java
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
package eu.eunoiaproject.bikesharing.framework.qsim;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;

/**
 * A bike sharing facility with a current number of bikes.
 * Code which interacts with the stations (puting or removing bikes)
 * must pass by the {@link BikeSharingManager} to move bikes around.
 *
 * @author thibautd
 */
public interface StatefulBikeSharingFacility extends BikeSharingFacility {
	public boolean hasBikes();
	public int getNumberOfBikes();
}

