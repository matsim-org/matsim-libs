/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.hybridsim.simulation.ExternalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;


 public class HybridQSimExternalNetworkFactory extends QNetworkFactory {
	 @Inject private Network network ;
	 @Inject private QSimConfigGroup qsimConfig ;
	 @Inject private Scenario scenario ;
	 @Inject private EventsManager events ;

	private final ExternalEngine e;
	private QNetsimEngine netsimEngine;
	private NetsimEngineContext context;

	public HybridQSimExternalNetworkFactory(ExternalEngine e) {
		this.e = e;
	}

	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, QNetsimEngine arg2) {
		double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, snapshotInfoFactory );

		this.context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, snapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;
		
		this.netsimEngine = arg2 ;
	}

	@Override
	public QNode createNetsimNode(Node node, QNetwork network) {
		return new QNode(node, network);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNode queueNode) {
		if (link.getAllowedModes().contains("2ext")) {
			return new QSimExternalTransitionLink(link, e, context, netsimEngine, queueNode );
		}
//		QLinkImpl ret = new QLinkImpl(link, network, queueNode, linkSpeedCalculator);
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine );
		QLinkImpl ret = linkBuilder.build(link, queueNode) ;
		if (link.getAllowedModes().contains("ext2")) {
			this.e.registerAdapter(new QLinkInternalIAdapter(ret));
		}
		return ret;
	}

}
