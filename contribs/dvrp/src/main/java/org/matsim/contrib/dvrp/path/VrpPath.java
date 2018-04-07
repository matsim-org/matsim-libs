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

package org.matsim.contrib.dvrp.path;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;

/**
 * Contains relevant information about a dvrp path. But not information about where we are. This information is
 * accessible via {@link OnlineDriveTaskTracker#getCurrentLinkIdx()}
 * 
 * @author (of documentation) nagel
 */
public interface VrpPath extends Iterable<Link> {
	int getLinkCount();

	Link getLink(int idx);

	double getLinkTravelTime(int idx);

	void setLinkTravelTime(int idx, double linkTT);

	Link getFromLink();

	Link getToLink();
}