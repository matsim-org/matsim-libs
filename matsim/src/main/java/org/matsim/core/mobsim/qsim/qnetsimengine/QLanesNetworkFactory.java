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
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.lanes.data.ModelLane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.lanes.data.LanesUtils;

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

	private InternalInterface internalInterface;
	
	private AgentCounter agentCounter;
	private MobsimTimer mobsimTimer;
	
	@Inject 
	public QLanesNetworkFactory( EventsManager events, Scenario scenario, MobsimTimer mobsimTimer, AgentCounter agentCounter ) {
		this.qsimConfig = scenario.getConfig().qsim();
		this.events = events ;
		this.network = scenario.getNetwork() ;
		this.scenario = scenario ;
		this.laneDefinitions = scenario.getLanes();
		delegate = new DefaultQNetworkFactory( events, scenario, mobsimTimer, agentCounter ) ;
		
		this.agentCounter = agentCounter;
		this.mobsimTimer = mobsimTimer;
	}

	@Override
	void initializeFactory(InternalInterface internalInterface) {
		this.internalInterface = internalInterface ;
		double effectiveCellSize = network.getEffectiveCellSize() ;
		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AbstractAgentSnapshotInfoBuilder agentSnapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		context = new NetsimEngineContext( events, effectiveCellSize, agentCounter, agentSnapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator );
		delegate.initializeFactory(internalInterface);
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNodeI queueNode) {
		QLinkI ql = null;
		LanesToLinkAssignment l2l = this.laneDefinitions.getLanesToLinkAssignments().get(link.getId());
		if (l2l != null){
			List<ModelLane> lanes = LanesUtils.createLanes(link, l2l);
			ql = new QLinkLanesImpl(link, queueNode, lanes, context, internalInterface);
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

}
