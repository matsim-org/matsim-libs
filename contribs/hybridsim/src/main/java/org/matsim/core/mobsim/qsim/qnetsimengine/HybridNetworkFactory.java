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
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public class HybridNetworkFactory extends QNetworkFactory {
	@Inject QSimConfigGroup qsimConfig ;
	@Inject EventsManager events ;
	@Inject Scenario scenario ;
	@Inject Network network;
	@Inject AgentCounter agentCounter;
	@Inject MobsimTimer mobsimTimer;

	private NetsimEngineContext context;


	private ExternalEngine externalEngine;

	public HybridNetworkFactory() {
		initializeFactory(); // Refactoring artefact. Draw in here.
	}
	
	private void initializeFactory() {
		double effectiveCellSize = ( network).getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );

		this.context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, snapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;
	}

	@Override
	public QNodeI createNetsimNode(Node node, InternalInterface internalInterface) {
		QNodeImpl.Builder builder = new QNodeImpl.Builder( internalInterface, context ) ;
		return builder.build( node ) ;
	}


	@Override
	public QLinkI createNetsimLink(Link link, QNodeI queueNode, InternalInterface internalInterface) {
		if (link.getAllowedModes().contains("2ext")) {
			return new QSimExternalTransitionLink(link, this.externalEngine, context, internalInterface, queueNode );
		}
//		QLinkImpl ret = new QLinkImpl(link, network, queueNode, linkSpeedCalculator);
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, internalInterface );
		QLinkImpl ret = linkBuilder.build(link, queueNode) ;
		if (link.getAllowedModes().contains("ext2")) {
			this.externalEngine.registerAdapter(new QLinkInternalIAdapter(ret));
		}
		return ret;
	}
	
	public void setExternalEngine(ExternalEngine externalEngine) {
		this.externalEngine = externalEngine;
	}
}
