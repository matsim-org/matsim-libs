
/* *********************************************************************** *
 * project: org.matsim.*
 * VisLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.vis.snapshotwriters;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;

/**Interface that is meant to replace the direct "QueueLink" accesses in the visualizer
 * 
 * @author nagel
 *
 */
public interface VisLink {

	Link getLink() ;
	
	Collection<? extends VisVehicle> getAllVehicles() ;
	
	VisData getVisData() ;
}
