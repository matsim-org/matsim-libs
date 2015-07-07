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
public final class HybridQSim2DNetworkFactory implements NetsimNetworkFactory {


	private final Sim2DEngine hybridEngine;
	private final Sim2DScenario s2dsc;
	private final Sim2DAgentFactory agentBuilder;


	public HybridQSim2DNetworkFactory( Sim2DEngine e, Scenario sc, Sim2DAgentFactory builder) {
		this.hybridEngine = e;
		this.agentBuilder = builder;
		this.s2dsc = (Sim2DScenario) sc.getScenarioElement(Sim2DScenario.ELEMENT_NAME);

	}

	@Override
	public QLinkInternalI createNetsimLink(final Link link, final QNetwork network, final QNode toQueueNode) {
		boolean sim2DQTransitionLink = false;
		boolean qSim2DTransitionLink = link.getAllowedModes().contains(TransportMode.walk2d);

		QLinkInternalI qLink = null;
		qLink = new QLinkImpl(link, network, toQueueNode);

		if (qSim2DTransitionLink){
			qSim2DTransitionLink = false;
			for (Link l : link.getFromNode().getInLinks().values()) {
				if (!l.getAllowedModes().contains(TransportMode.walk2d)) {
					qSim2DTransitionLink = true;
					break;
				}
			}
			//			}
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
		if (sim2DQTransitionLink) {
			Sim2DQAdapterLink lowResLink = new Sim2DQAdapterLink(qLink);
			this.hybridEngine.registerLowResLink(lowResLink);
			return qLink;
		}
		return qLink;
	}

	@Override
	public QNode createNetsimNode(final Node node, QNetwork network) {
		return new QNode(node, network);
	}

}
