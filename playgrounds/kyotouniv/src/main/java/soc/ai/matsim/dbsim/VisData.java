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

package soc.ai.matsim.dbsim;

import java.util.Collection;

import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
/**
 * Interface for methods to provide a visualizer with data.
 * @author dgrether
 */
public interface VisData {

	/**
	 * @return The value for coloring the link in NetVis. Actual: veh count / space capacity
	 */
	public double getDisplayableSpaceCapValue();

	/**
	 * Returns a measure for how many vehicles on the link have a travel time
	 * higher than freespeedTraveltime on a scale from 0 to 2. When more then half
	 * of the possible vehicles are delayed, the value 1 will be returned, which
	 * depicts the worst case on a (traditional) scale from 0 to 1.
	 *
	 * @return A measure for the number of vehicles being delayed on this link.
	 */
	public double getDisplayableTimeCapValue(double now);

	public Collection<AgentSnapshotInfo> getVehiclePositions(
			final Collection<AgentSnapshotInfo> positions);

/* I don't think these two methods should be part of the interface!
 * Only getVehiclePositions() should be in the interface, how the
 * vehicles are positioned, is free to be implemented.
 * 
 */
//	/**
//	 * Calculates the positions of all vehicles on this link so that there is
//	 * always the same distance between following cars. A single vehicle will be
//	 * placed at the middle (0.5) of the link, two cars will be placed at
//	 * positions 0.25 and 0.75, three cars at positions 0.16, 0.50, 0.83, and so
//	 * on.
//	 *
//	 * @param positions
//	 *          A collection where the calculated positions can be stored.
//	 */
//	public void getVehiclePositionsEquil(final Collection<PositionInfo> positions);
//
//	/**
//	 * Calculates the positions of all vehicles on this link according to the
//	 * queue-logic: Vehicles are placed on the link according to the ratio between
//	 * the free-travel time and the time the vehicles are already on the link. If
//	 * they could have left the link already (based on the time), the vehicles
//	 * start to build a traffic-jam (queue) at the end of the link.
//	 *
//	 * @param positions
//	 *          A collection where the calculated positions can be stored.
//	 */
//	public void getVehiclePositionsQueue(final Collection<PositionInfo> positions);

}