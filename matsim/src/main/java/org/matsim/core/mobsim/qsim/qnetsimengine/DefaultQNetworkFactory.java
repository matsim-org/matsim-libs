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


import javax.inject.Inject;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.network.NetworkImpl;


/**
 * @author dgrether
 */
public final class DefaultQNetworkFactory extends QNetworkFactory {
	@Inject QSimConfigGroup qsimConfig ;
	@Inject EventsManager events ;
	@Inject Network network ;
	@Inject Mobsim mobsim ;

	@Override
	public QLinkI createNetsimLink(final Link link, final QNetwork qnetwork, final QNode toQueueNode) {
		double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;
		AgentCounter agentCounter = ((QSim)mobsim).getAgentCounter() ;
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = qnetwork.simEngine.getAgentSnapshotInfoBuilder();
		Gbl.assertNotNull(agentSnapshotInfoBuilder);
		final QueueWithBufferContext context = new QueueWithBufferContext( events, effectiveCellSize,
				agentCounter, agentSnapshotInfoBuilder, qsimConfig );

		QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder( context ) ;

		return new QLinkImpl(link, qnetwork, toQueueNode, builder );
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork qnetwork) {
		return new QNode(node, qnetwork);
	}

}
