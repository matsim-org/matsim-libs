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
import org.matsim.controler.Controler;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.network.NetworkLayer;

public class MyQueueNetwork extends QueueNetwork{
	
	protected Controler controler; 
	
	final private static Logger log = Logger.getLogger(MyQueueNetwork.class);
	
	public MyQueueNetwork(NetworkLayer networkLayer)
	{
		super(networkLayer);
	}
	
	public MyQueueNetwork(NetworkLayer networkLayer, QueueNetworkFactory<QueueNode, QueueLink> factory) {
		super(networkLayer, factory);
	}

	public void setControler(Controler controler) 
	{
		this.controler = controler;
	}
	
	public Controler getControler()
	{
		return controler;
	}

}
