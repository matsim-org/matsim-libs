/* *********************************************************************** *
 * project: org.matsim.*
 * MyQueueLink.java
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

package playground.christoph.mobsim;

import java.util.LinkedList;
import java.util.Queue;

import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.network.Link;

public class MyQueueLink extends QueueLink {
	
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QueueVehicle> buffer = new LinkedList<QueueVehicle>();
	
	public MyQueueLink(final Link l, final QueueNetwork queueNetwork, final QueueNode toNode) 
	{
		super(l, queueNetwork, toNode);
	}
	
	public Queue<QueueVehicle> getBuffer()
	{
		return buffer;
	}
}
