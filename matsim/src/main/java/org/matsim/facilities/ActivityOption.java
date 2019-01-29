/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.SortedSet;

import org.matsim.core.api.internal.MatsimFacilitiesObject;

/**
 * @author dgrether
 * @author mrieser / Senozon AG
 */
public interface ActivityOption extends MatsimFacilitiesObject {

	public double getCapacity();

	public void setCapacity(double cap);

	public void addOpeningTime(OpeningTime openingTime);

	public SortedSet<OpeningTime> getOpeningTimes();

	public String getType();

}
