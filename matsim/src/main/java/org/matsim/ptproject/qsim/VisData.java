/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim;

import java.util.Collection;

import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
/**
 * Interface for methods to provide a visualizer with data.
 * @author dgrether
 */
public interface VisData {
	/**
	 * Returns a measure for how many vehicles on the link have a travel time
	 * higher than freespeedTraveltime on a scale from 0 to 2. When more then half
	 * of the possible vehicles are delayed, the value 1 will be returned, which
	 * depicts the worst case on a (traditional) scale from 0 to 1.
	 *
	 * @return A measure for the number of vehicles being delayed on this link.
	 */
	public double getDisplayableTimeCapValue(double time);

	public Collection<AgentSnapshotInfo> getVehiclePositions(double time, final Collection<AgentSnapshotInfo> positions);


}