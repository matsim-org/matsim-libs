/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayQueueNode.java
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

package playground.gregor.withinday_evac.mobsim;





import org.apache.log4j.Logger;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueNode;
import org.matsim.mobsim.Vehicle;
import org.matsim.network.Node;


public class WithindayQueueNode extends QueueNode {
	private static final Logger log = Logger.getLogger(WithindayQueueNode.class);
	
	public WithindayQueueNode(final Node n, final QueueNetworkLayer queueNetworkLayer) {
		super(n, queueNetworkLayer);
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	@Override
	public boolean moveVehicleOverNode(final Vehicle veh, final double now) {
		((OccupiedVehicle)veh).setNextLink(now,this.getNode().getId());
		
		return super.moveVehicleOverNode(veh, now);

	}
}
