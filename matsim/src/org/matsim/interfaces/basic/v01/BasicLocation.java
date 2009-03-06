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

package org.matsim.interfaces.basic.v01;

import org.matsim.basic.v01.LocationType;

/**
 * @author dgrether
 */
@Deprecated // not yet clear
public interface BasicLocation {
	
	public Coord getCenter();
	// FIXME [kn] rename to getCoord ... and then make all interfaces with getCoord to inherit from this.
	
	public Id getId();
	
	/**
	 * @deprecated not yet clear
	 */
	@Deprecated // not yet clear
	public LocationType getLocationType();
	
}
