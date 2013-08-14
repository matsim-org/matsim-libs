/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFilter
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
package playground.dgrether.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;


/**
 * @author dgrether
 *
 */
public class NetworkFilter {

	private Network network;

	public NetworkFilter(Network net){
		this.network = net;
	}
	
	public boolean networkContainsLink(Id linkId){
		return this.network.getLinks().containsKey(linkId);
	}
	
}
