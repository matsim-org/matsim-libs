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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

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
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

public class HybridNetworkFactory extends QNetworkFactory {
	@Inject QSimConfigGroup qsimConfig ;
	@Inject Scenario scenario ;
	@Inject Network network ;
	@Inject EventsManager events ;

	private final Map<String, QNetworkFactory> facs = new LinkedHashMap<>();
	private NetsimInternalInterface netsimEngine;
	private NetsimEngineContext context;
	
	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(network.getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
		}
		AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
		AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
		
		double effectiveCellSize = ((NetworkImpl) network ).getEffectiveCellSize() ;
		this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, snapshotInfoBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;
		
		this.netsimEngine = netsimEngine1 ;
	}

	@Override
	public QNode createNetsimNode(Node node) {
		QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
		return builder.build( node ) ;
	}

	@Override
	public QLinkI createNetsimLink(Link link, QNode queueNode) {
		
		for (Entry<String, QNetworkFactory> e : this.facs.entrySet()) {
			if (link.getAllowedModes().contains(e.getKey())) {
				return e.getValue().createNetsimLink(link, queueNode);
			}
		}
		//default QLink
		QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine ) ;
		return linkBuilder.build(link, queueNode) ;
	}
	
	public void putNetsimNetworkFactory(String key, QNetworkFactory fac) {
		this.facs.put(key, fac);
	}

}
