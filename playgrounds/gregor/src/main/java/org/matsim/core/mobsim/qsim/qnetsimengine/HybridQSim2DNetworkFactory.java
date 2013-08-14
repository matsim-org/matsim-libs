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
import org.matsim.api.core.v01.network.Node;

import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.TransportMode;
import playground.gregor.sim2d_v4.simulation.Sim2DAgentFactory;
import playground.gregor.sim2d_v4.simulation.Sim2DEngine;



/**
 * Design thoughts:<ul>
 * <li> It would probably be much better to have this in a separate package.  But this means to move a lot of scopes from
 * "package" to protected.  Worse, the interfaces are not sorted out.  So I remain here for the time being.  kai, jan'11
 */
public final class HybridQSim2DNetworkFactory implements NetsimNetworkFactory<QNode, QLinkInternalI> {


	private final Sim2DEngine hybridEngine;
	private final Sim2DScenario s2dsc;
	private final Sim2DAgentFactory agentBuilder;
	
	private final BiQNetworkFactory biQFac = new BiQNetworkFactory();

	public HybridQSim2DNetworkFactory( Sim2DEngine e, Scenario sc, Sim2DAgentFactory builder) {
		this.hybridEngine = e;
		this.agentBuilder = builder;
		this.s2dsc = sc.getScenarioElement(Sim2DScenario.class);

	}

	@Override
	public QLinkInternalI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		boolean sim2DQTransitionLink = false;
		boolean qSim2DTransitionLink = link.getAllowedModes().contains(TransportMode.walk2d);
		
//		QueueWithBuffer.HOLES = true;
		
//		QLinkInternalI qLink = 
		QLinkInternalI qLink = null;
		if (link.getId().toString().startsWith("mt")) {
			qLink = new QLinkImpl(link, network, toQueueNode);
//		} else if (link.getId().toString().equals("t_l5b")) {
//			qLink = new BiPedQLinkImpl(link,network,toQueueNode);
////			qLink = new QLinkImpl(link, network, toQueueNode);
		}else if (link.getId().toString().startsWith("t")) {
//			qLink = new QLinkImpl(link, network, toQueueNode);
			qLink = this.biQFac.createNetsimLink(link, network, toQueueNode);
//					this.biQFac.createNetsimLink(link, network, toQueueNode);
		} else {
//			qLink =this.biQFac.createNetsimLink(link, network, toQueueNode);
			qLink = new QLinkImpl(link, network, toQueueNode);
		}
		
		if (qSim2DTransitionLink){
			if (link.getFromNode().getOutLinks().size() > 1) {
				qSim2DTransitionLink = false;
				for (Link l : link.getFromNode().getInLinks().values()) {
					if (!l.getAllowedModes().contains(TransportMode.walk2d)) {
						qSim2DTransitionLink = true;
						break;
					}
				}
			}
		} else {
			for (Link l : link.getFromNode().getInLinks().values()) {
				if (l.getAllowedModes().contains(TransportMode.walk2d)) {
					sim2DQTransitionLink = true;
					break;
				}
			}
		}

		if (qSim2DTransitionLink) {
			Sim2DEnvironment env = this.s2dsc.getSim2DEnvironment(link);
			QSim2DTransitionLink hiResLink = new QSim2DTransitionLink(link, network, toQueueNode, this.hybridEngine, qLink, env, this.agentBuilder) ;
			this.hybridEngine.registerHiResLink(hiResLink);
			return hiResLink ;
		} 
		//		QLinkImpl ql = 
		if (sim2DQTransitionLink) {
			Sim2DQTransitionLink lowResLink = new Sim2DQTransitionLink(qLink);
			this.hybridEngine.registerLowResLink(lowResLink);
			return lowResLink;
		}

//		return new QLinkImpl(link, network, toQueueNode);
		return qLink;
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
