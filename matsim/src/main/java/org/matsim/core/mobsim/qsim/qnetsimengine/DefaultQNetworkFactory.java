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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.network.NetworkImpl;


/**
 * @author dgrether
 */
public final class DefaultQNetworkFactory extends QNetworkFactory {

	@Override
	public QLinkI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		QSimConfigGroup qsimConfig = network.simEngine.getMobsim().getScenario().getConfig().qsim() ;
		EventsManager events = network.simEngine.getMobsim().getEventsManager() ;
		double effectiveCellSize = ((NetworkImpl) network.getNetwork()).getEffectiveCellSize() ;
		AgentCounter agentCounter = network.simEngine.getMobsim().getAgentCounter() ;
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = network.simEngine.getAgentSnapshotInfoBuilder();
		Gbl.assertNotNull(agentSnapshotInfoBuilder);
		final QueueWithBufferContext context = new QueueWithBufferContext( events, effectiveCellSize,
				agentCounter, agentSnapshotInfoBuilder, qsimConfig );

		QueueWithBuffer.Builder builder = new QueueWithBuffer.Builder( context ) ;

		return new QLinkImpl(link, network, toQueueNode, builder );
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
