/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
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

package org.matsim.world;

import org.matsim.api.basic.v01.BasicLocation;
import org.matsim.api.basic.v01.Identifiable;

/**
 * @author nagel
 *
 */
public interface Location extends BasicLocation, Identifiable {
	// yy One can debate if "Location" should have an "id" by force.  It is, 
	// however, easier to refactor this way.  If you just want a coordinate,
	// you can still use BasicLocation.  kai, jul09

	@Deprecated // use of the current "layer" structure is discouraged
	public abstract Layer getLayer();
	// yyyy There was some decision that "location objects" have the back pointer.  
	// If there is agreement on this, we could leave the getLayer syntax there
	// (although, without the mappings, it is not very meaningful)

}
