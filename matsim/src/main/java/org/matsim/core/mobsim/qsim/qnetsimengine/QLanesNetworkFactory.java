/* *********************************************************************** *
 * project: org.matsim.*
 * QLanesNetworkFactory
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

package org.matsim.core.mobsim.qsim.qnetsimengine;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.network.NetworkImpl;
import org.matsim.lanes.ModelLane;
import org.matsim.lanes.data.v20.Lanes;
import org.matsim.lanes.data.v20.LanesToLinkAssignment20;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.lanes.LanesUtils;

import java.util.List;

import javax.inject.Inject;


public class QLanesNetworkFactory extends QNetworkFactory {

	private Lanes laneDefinitions;
	
	private final QNetworkFactory delegate ;

	private NetsimEngineContext context;

	private QSimConfigGroup qsimConfig;

	private EventsManager events;

	private Network network;

	private Scenario scenario;

	private NetsimInternalInterface netsimEngine;
	
	@Inject 
	public QLanesNetworkFactory( QSimConfigGroup qsimConfig, EventsManager events, Network network, Scenario scenario, Lanes lanesDefinitions ) {
		this.qsimConfig = qsimConfig ;
		this.events = events ;
		this.network = network ;
		this.scenario = scenario ;
		this.laneDefinitions = lanesDefinitions;
		delegate = new DefaultQNetworkFactory( events, scenario ) ;
	}

	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
		this.netsimEngine = netsimEngine1 ;
		double effectiveCellSize = ((NetworkImpl) network).getEffectiveCellSize() ;
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator );
		delegate.initializeFactory(agentCounter, mobsimTimer, netsimEngine1);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNode queueNode) {
		QLinkI ql = null;
		LanesToLinkAssignment20 l2l = this.laneDefinitions.getLanesToLinkAssignments().get(link.getId());
		if (l2l != null){
			List<ModelLane> lanes = LanesUtils.createLanes(link, l2l);
			ql = new QLinkLanesImpl(link, queueNode, lanes, context, netsimEngine);
		}
		else {
			ql = this.delegate.createNetsimLink(link, queueNode);
		}
		return ql;
	}

	@Override
	public QNode createNetsimNode(Node node) {
		return this.delegate.createNetsimNode(node);
	}

}
