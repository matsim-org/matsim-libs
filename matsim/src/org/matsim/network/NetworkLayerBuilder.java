/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkLayerBuilder.java
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

package org.matsim.network;

import org.matsim.mobsim.QueueNetworkLayer;

import playground.marcel.ptnetwork.PtNetworkLayer;

public class NetworkLayerBuilder {
	public static final int NETWORK_DEFAULT = 0;
	public static final int NETWORK_SIMULATION = 1;
//	public static final int NETWORK_ROUTER = 2;
	public static final int NETWORK_PT = 3;
	
	private static int type = 0;
	
	public static NetworkLayer newNetworkLayer() {
		NetworkLayer result = null;
		switch (type)
		{
			case NETWORK_PT:
				result = new PtNetworkLayer();
				break;
			case NETWORK_SIMULATION:
				result = new QueueNetworkLayer();
				break;
			case NETWORK_DEFAULT:
			default:
				result = new NetworkLayer();
			break;
		};
		return result;
	}
	
	public static void setNetworkLayerType(int newtype) { type = newtype; }
	
}
