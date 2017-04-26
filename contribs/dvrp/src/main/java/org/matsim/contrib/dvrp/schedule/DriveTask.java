/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.path.*;

/**
 * @author (of documentation) maciejewski, nagel
 *
 */
public interface DriveTask extends Task {
	VrpPath getPath();

	/**
	 * Vehicle changes its path. Just replaces the previous VrpPath with this one; this will work (if consistent) since
	 * (1) nobody (hopefully) caches a references to the original path, and (2) access to the link on the path is via
	 * index only (and not via an iterator or similar). <br/>
	 * <br/>
	 * Can be used for:
	 * <ul>
	 * <li>changing destination (while keeping the current task active)
	 * <li>stopping it as soon as possible (i.e. at the end of the current/next link)
	 * <li>random walk, roaming/cruising around
	 * <li>...
	 * </ul>
	 */
	void pathDiverted(DivertedVrpPath divertedPath, double newEndTime);
}
