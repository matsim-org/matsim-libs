/* *********************************************************************** *
 * project: org.matsim.*
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

package eu.eunoiaproject.bikesharing.framework.qsim;

import org.matsim.api.core.v01.Id;

import java.util.Collection;
import java.util.Map;


public interface BikeSharingManager {

	public void addListener(BikeSharingManagerListener l);

	public Map<Id, ? extends StatefulBikeSharingFacility> getFacilities();

	public Map<Id, ? extends Collection<? extends StatefulBikeSharingFacility>> getFacilitiesAtLinks();

	public void takeBike(Id facility);

	public void takeBikes(Id facility, int amount);

	public void putBike(Id facility);

	public void putBikes(Id facility, int amount);

}
