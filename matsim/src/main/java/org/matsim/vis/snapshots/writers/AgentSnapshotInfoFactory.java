/* *********************************************************************** *
 * project: matsim
 * AgentSnapshotInfoFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.vis.snapshots.writers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

/**
 * @author nagel
 *
 */
public class AgentSnapshotInfoFactory {

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link) {
		return new PositionInfo(agentId, link);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, int cnt) {
		return new PositionInfo(agentId, link, cnt);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane) {
		return new PositionInfo(agentId, link, distanceOnLink, lane);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane, int cnt) {
		return new PositionInfo(agentId, link, distanceOnLink, lane, cnt);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id driverId, double easting, double northing, double elevation, double azimuth) {
		return new PositionInfo(driverId, easting, northing, elevation, azimuth);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id agentId, Link link, double distanceOnLink, int lane, double speed,
			AgentState vehicleState)
	{
		return new PositionInfo(agentId, link, distanceOnLink, lane, speed, vehicleState);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(double linkScale, Id agentId, Link link, double distanceOnLink, int lane,
			double speed, AgentState agentState)
	{
		return new PositionInfo(linkScale, agentId, link, distanceOnLink, lane, speed, agentState);
	}

	public static AgentSnapshotInfo staticCreateAgentSnapshotInfo(Id driverId, double easting, double northing, double elevation, double azimuth,
			double speed, AgentState vehicleState)
	{
		return new PositionInfo(driverId, easting, northing, elevation, azimuth, speed, vehicleState);
	}

}
