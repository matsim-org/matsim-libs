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

package org.matsim.core.api.experimental.facilities;

import org.matsim.api.basic.v01.BasicLocation;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.Identifiable;

/**
 * A (Basic)Facility is a (Basic)Location ("getCoord") with an Id ("getId") that is connected to a Link ("getLinkId").
 * 
 * @author nagel
 */
public interface Facility extends BasicLocation, Identifiable {
	
	public Id getLinkId() ;

}
