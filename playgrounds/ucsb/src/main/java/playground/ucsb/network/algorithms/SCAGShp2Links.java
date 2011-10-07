/* *********************************************************************** *
 * project: org.matsim.*
 * SCAGShp2Links.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb.network.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.network.NetworkImpl;

/**
 * @author balmermi
 *
 */
public class SCAGShp2Links implements NetworkRunnable {

	/* (non-Javadoc)
	 * @see org.matsim.core.api.internal.NetworkRunnable#run(org.matsim.api.core.v01.network.Network)
	 */
	@Override
	public void run(Network network) {
		
//		Link l = network.getFactory().createLink(null,null, null);
//		Set<String> modes = new HashSet<String>();
//		modes.add(TransportMode.car);
//		modes.add(TransportMode.pt);
//		l.setAllowedModes(modes);
//		l.setCapacity(capacity)
//		l.setFreespeed(freespeed)
//		l.setLength(length)
//		l.setNumberOfLanes(lanes)

	}

}
