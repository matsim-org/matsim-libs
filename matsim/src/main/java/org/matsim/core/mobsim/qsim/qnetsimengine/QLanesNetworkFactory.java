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


import java.util.List;

import jakarta.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.ModelLane;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;


public final class QLanesNetworkFactory implements QNetworkFactory {
	private final Lanes laneDefinitions;
	private final DefaultQNetworkFactory delegate ;
	private FlowEfficiencyCalculator flowEfficiencyCalculator;
	private NetsimEngineContext context;
	private final QSimConfigGroup qsimConfig;
	private final EventsManager events;
	private final Network network;
	private final Scenario scenario;
	private NetsimInternalInterface netsimEngine;
	@Inject QLanesNetworkFactory( EventsManager events, Scenario scenario, DefaultQNetworkFactory delegate ) {
		this.qsimConfig = scenario.getConfig().qsim();
		this.events = events ;
		this.network = scenario.getNetwork() ;
		this.scenario = scenario ;
		this.laneDefinitions = scenario.getLanes();
//		this.delegate = new DefaultQNetworkFactory( events, scenario ) ;
		this.delegate = delegate;
	}

	@Override
	public void initializeFactory( AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1 ) {
		this.netsimEngine = netsimEngine1 ;
		double effectiveCellSize = network.getEffectiveCellSize() ;
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngineWithThreadpool.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator );
		delegate.initializeFactory(agentCounter, mobsimTimer, netsimEngine1);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNodeI queueNode) {
		QLinkI ql = null;
		LanesToLinkAssignment l2l = this.laneDefinitions.getLanesToLinkAssignments().get(link.getId());
		if (l2l != null){
			List<ModelLane> lanes = LanesUtils.createLanes(link, l2l);

			QLinkLanesImpl.Builder builder = new QLinkLanesImpl.Builder(context, netsimEngine) ;

			LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator() ;
			builder.setLinkSpeedCalculator( linkSpeedCalculator );


			if (flowEfficiencyCalculator != null)
				builder.setFlowEfficiencyCalculator(flowEfficiencyCalculator);

			ql = builder.build( link, queueNode, lanes ) ;
		}
		else {
			ql = this.delegate.createNetsimLink(link, queueNode);
		}
		return ql;
	}

	@Override
	public QNodeI createNetsimNode(Node node) {
		return this.delegate.createNetsimNode(node);
	}

	/**
	 * Set factory to create QLinks that are not lanes.
	 */
//	public void setDelegate(QNetworkFactory delegate) {
//		this.delegate = delegate;
//	}

	/**
	 * Flow efficiency calculator to use for lanes.
	 */
	public void setFlowEfficiencyCalculator(FlowEfficiencyCalculator flowEfficiencyCalculator) {
		this.flowEfficiencyCalculator = flowEfficiencyCalculator;
	}
}
