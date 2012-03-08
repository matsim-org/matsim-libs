/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;



/**
 * Design thoughts:<ul>
 * <li> It would probably be much better to have this in a separate package.  But this means to move a lot of scopes from
 * "package" to protedted.  Worse, the interfaces are not sorted out.  So I remain here for the time being.  kai, jan'11
 */
public final class PatnaNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {

	@Override
	public QLinkInternalI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		
//		List<QVehicle> internalQueue = new ...internalQueue ;
//		
//		return new QLinkIimpl( link, network, toQueueNode, internalQueue ) ;
		return new QLinkImpl( link, network, toQueueNode) ;
		
	}

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network) ;
	}

}
