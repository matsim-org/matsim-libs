/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultQSimEngineFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.singapore.pt;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetwork;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNode;


/**
 * @author dgrether
 *
 */
public final class DefaultQSimEngineFactory implements QNetsimEngineFactory {

	@Override
	public QNetsimEngine createQSimEngine(Netsim sim) {
		NetsimNetworkFactory<QNode, QLinkImpl> netsimNetworkFactory = new NetsimNetworkFactory<QNode, QLinkImpl>() {
			@Override
			public QLinkImpl createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
				return new QLinkImpl(link, network, toQueueNode);
			}
			@Override
			public QNode createNetsimNode(final Node node, QNetwork network) {
				return new QNode(node, network);
			}
		};
		return new QNetsimEngine((QSim) sim, netsimNetworkFactory);
	}

}
