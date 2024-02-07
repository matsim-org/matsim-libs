/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * A Facility is a (Basic)Location ("getCoord") that is connected to a Link ("getLinkId").  Since about 2018, it no longer implements Identifiable, since
 * that caused headaches with the generification of Id.
 *
 * @author (of javadoc) nagel
 */
public interface Facility extends BasicLocation, Customizable {
	// yyyyyy we might consider to not further differentiate the different facility types in id space.  Then it could just be
	//   interface Facility extends BasicLocation<Facility>, ...
	// kai, dec'15

	public Id<Link> getLinkId();


	public static final String FACILITY_NO_LONGER_IDENTIFIABLE = "After refactoring, facility " +
													 "does not longer automatically " +
													 "implement Identifiable.  Don't know what to do.";

}
