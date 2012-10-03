/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data.vehtrajectories;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 * @author mrieser
 */
public class CalculateLinkTravelTimesFromVehTrajectories implements VehicleTrajectoryHandler {

	private final TravelTimeCalculator ttcalc;
	private final Network network;
	
	public CalculateLinkTravelTimesFromVehTrajectories(final TravelTimeCalculator ttcalc, final Network network) {
		this.ttcalc = ttcalc;
		this.network = network;
	}
	
	@Override
	public void handleVehicleTrajectory(VehicleTrajectory trajectory) {
		int[] nodes = trajectory.getTravelledNodes();
		double[] times = trajectory.getTravelledNodeTimes();
		
		double time = trajectory.getStartTime();
		
		Node prevNode = null;
		for (int i = 0; i < nodes.length; i++) {
			Node node = this.network.getNodes().get(new IdImpl(i));
			if (prevNode != null) {
				Link link = NetworkUtils.getConnectingLink(prevNode, node);
				double linkTime = times[i];
				Id id = new IdImpl(trajectory.getVehNr());
				this.ttcalc.handleEvent(new LinkEnterEvent(time, id, link.getId(), id));
				time += linkTime;
				this.ttcalc.handleEvent(new LinkLeaveEvent(time, id, link.getId(), id));
			}
			prevNode = node;
		}
	}
}
