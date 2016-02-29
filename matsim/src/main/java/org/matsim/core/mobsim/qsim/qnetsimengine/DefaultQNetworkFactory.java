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

package org.matsim.core.mobsim.qsim.qnetsimengine;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;


/**
 * @author dgrether
 */
final class DefaultQNetworkFactory implements NetsimNetworkFactory {

	public DefaultQNetworkFactory() {}
	
	@Override
	public QLinkI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		return new QLinkImpl(link, network, toQueueNode);
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
