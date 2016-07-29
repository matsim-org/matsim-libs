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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import playground.gregor.TransportMode;
import playground.gregor.casim.simulation.CANetsimEngine;




/**
 * Design thoughts:<ul>
 * <li> It would probably be much better to have this in a separate package.  But this means to move a lot of scopes from
 * "package" to protected.  Worse, the interfaces are not sorted out.  So I remain here for the time being.  kai, jan'11
 */
public final class HybridQSimCANetworkFactory extends QNetworkFactory {


	private final CANetsimEngine hybridEngine;
	private NetsimInternalInterface netsimEngine;
	private final Scenario sc;
	private NetsimEngineContext context;
	private final EventsManager events;

	public HybridQSimCANetworkFactory(CANetsimEngine e, Scenario sc, EventsManager events) {
		this.hybridEngine = e;
		this.sc = sc ;
		this.events = events ;
	}

	@Override
	void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
		QSimConfigGroup qsimConfig = sc.getConfig().qsim() ;
		
		this.netsimEngine = netsimEngine1 ;
		
		double effectiveCellSize = ((Network) sc.getNetwork()).getEffectiveCellSize() ;

		SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
		linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
		if (! Double.isNaN(sc.getNetwork().getEffectiveLaneWidth())){
			linkWidthCalculator.setLaneWidth( sc.getNetwork().getEffectiveLaneWidth() );
		}
		AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
		AbstractAgentSnapshotInfoBuilder snapshotBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( sc, linkWidthCalculator );
		
		this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, snapshotBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;
	}

	@Override
	public QLinkI createNetsimLink(final Link link, final QNode toQueueNode) {
		boolean sim2DQTransitionLink = false;
		boolean qSim2DTransitionLink = link.getAllowedModes().contains(TransportMode.walkca);

		
		
		QLinkI qLink = null;
		if (qSim2DTransitionLink) {
			qLink = new CALink(link,1);
		} else {
//			qLink = new QLinkImpl(link, network, toQueueNode);
			throw new RuntimeException("Not yet implemented!");
		}


		if (qSim2DTransitionLink){
			//			if (link.getFromNode().getOutLinks().size() > 1) {
			qSim2DTransitionLink = false;
			for (Link l : link.getFromNode().getInLinks().values()) {
				if (!l.getAllowedModes().contains(TransportMode.walkca)) {
					qSim2DTransitionLink = true;
					break;
				}
			}
			//			}
		} else {
			for (Link l : link.getFromNode().getInLinks().values()) {
				if (l.getAllowedModes().contains(TransportMode.walkca)) {
					sim2DQTransitionLink = true;
					break;
				}
			}
		}

		if (qSim2DTransitionLink) {
//			Sim2DEnvironment env = this.s2dsc.getSim2DEnvironment(link);
//			QSimCATransitionLink hiResLink = new QSimCATransitionLink(link, network, toQueueNode, this.hybridEngine, qLink, env, this.agentBuilder) ;
//			this.hybridEngine.registerHiResLink(hiResLink);
//			return hiResLink ;
			throw new RuntimeException("Not yet implemented!");
		} 
		//		QLinkImpl ql = 
		if (sim2DQTransitionLink) {
//			Sim2DQTransitionLink lowResLink = new Sim2DQTransitionLink(qLink);
//			this.hybridEngine.registerLowResLink(lowResLink);
//			return lowResLink;
			throw new RuntimeException("Not yet implemented!");
		}

		//		return new QLinkImpl(link, network, toQueueNode);
		return qLink;
	}

	@Override
	public QNode createNetsimNode(final Node node) {
		//TODO CA Node;
		QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
		return builder.build( node ) ;
	}

}
