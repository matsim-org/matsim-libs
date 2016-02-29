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
 * Design thoughts:<ul>
 * <li> It would probably be much better to have this in a separate package.  But this means to move a lot of scopes from
 * "package" to protected.  Worse, the interfaces are not sorted out.  So I remain here for the time being.  kai, jan'11
 */
public final class KaiHybridNetworkFactory implements NetsimNetworkFactory {

	private final KaiHybridEngine hybridEngine;

	public KaiHybridNetworkFactory( KaiHybridEngine hybridEngine) {
		this.hybridEngine = hybridEngine;
	}

	@Override
	public QLinkI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		if ( link.getId().toString().equals("15") ) {
			KaiHiResLink hiResLink = new KaiHiResLink(link, network, toQueueNode, hybridEngine ) ;
			this.hybridEngine.registerHiResLink(hiResLink);
			return hiResLink ;
		} else {
			return new QLinkImpl(link, network, toQueueNode);
		}
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
