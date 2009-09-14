/* *********************************************************************** *
 * project: org.matsim.*
 * MyQueueNetwork.java
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

import org.apache.log4j.Logger;

import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.network.NetworkLayer;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.events.LinkVehiclesCounter;

public class MyQueueNetwork extends QueueNetwork{
	
	protected LinkVehiclesCounter linkVehiclesCounter;
	protected LinkReplanningMap linkReplanningMap;
	
	final private static Logger log = Logger.getLogger(MyQueueNetwork.class);

	public MyQueueNetwork(NetworkLayer networkLayer)
	{
		super(networkLayer);
	}
	
	public void setLinkVehiclesCounter(LinkVehiclesCounter linkVehiclesCounter)
	{
		this.linkVehiclesCounter = linkVehiclesCounter;
	}
	
	public LinkVehiclesCounter getLinkVehiclesCounter()
	{
		return linkVehiclesCounter;
	}
	
	public void setLinkReplanningMap(LinkReplanningMap linkReplanningMap)
	{
		this.linkReplanningMap = linkReplanningMap;
	}
	
	public LinkReplanningMap getLinkReplanningMap()
	{
		return this.linkReplanningMap;
	}

}