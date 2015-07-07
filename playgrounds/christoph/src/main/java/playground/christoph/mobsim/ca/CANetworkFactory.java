/* *********************************************************************** *
 * project: org.matsim.*
 * CANetworkFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;

public class CANetworkFactory  {

	private final double spatialResolution;
	
	public CANetworkFactory(double spatialResolution) {
		this.spatialResolution = spatialResolution;
	}
	
	public CANode createNetsimNode(Node node, QNetwork network) {
		return null;
	}

	public CANode createNetsimNode(Node node, CANetwork network) {
		return new CANode(node, network);
	}
	
	public CALink createNetsimLink(Link link, QNetwork network, CANode queueNode) {
		return null;
	}
	
	public CALink createNetsimLink(Link link, NetsimNetwork network, CANode toNode) {
		return new CALink(link, toNode, MatsimRandom.getLocalInstance(), this.spatialResolution);
	}

}
