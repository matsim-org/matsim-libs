/* *********************************************************************** *
 * project: org.matsim.*
 * DgNetShrinkImproved
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
package playground.dgrether.events;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkFilterManager;

import playground.dgrether.EnvelopeLinkStartEndFilter;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class DgNetShrinkImproved {

	public Network createSmallNetwork(Network net, Envelope envelope) {
		NetworkFilterManager filterManager = new NetworkFilterManager(net);
		filterManager.addLinkFilter(new EnvelopeLinkStartEndFilter(envelope));
		Network newNetwork = filterManager.applyFilters();
		return newNetwork;		
	}
	
}
