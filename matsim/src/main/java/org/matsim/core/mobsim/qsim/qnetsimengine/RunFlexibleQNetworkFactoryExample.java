/* *********************************************************************** *
 * project: org.matsim.*												   *
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

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

/**
 * @author nagel
 *
 */
public class RunFlexibleQNetworkFactoryExample {

	static class MyQNetworkFactory extends QNetworkFactory {
		@Inject private EventsManager events ;
		@Inject private Scenario scenario ; // yyyyyy I would like to get rid of this. kai, mar'16
		@Inject private Network network ;
		@Inject private QSimConfigGroup qsimConfig ;
		@Inject private MobsimTimer mobsimTimer;
		@Inject private AgentCounter agentCounter;

		private NetsimEngineContext context;
		
		public MyQNetworkFactory() {
			initializeFactory(); // Refactoring artifact. Draw in here.
		}

		private void initializeFactory() {
			double effectiveCellSize = ((Network)network).getEffectiveCellSize() ;
			
			SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
			linkWidthCalculator.setLinkWidthForVis( qsimConfig.getLinkWidthForVis() );
			if (! Double.isNaN(network.getEffectiveLaneWidth())){
				linkWidthCalculator.setLaneWidth( network.getEffectiveLaneWidth() );
			}
			AbstractAgentSnapshotInfoBuilder snapshotBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
			
			this.context = new NetsimEngineContext(events, effectiveCellSize, agentCounter, snapshotBuilder, qsimConfig, mobsimTimer, linkWidthCalculator ) ;
		}
		@Override QNodeI createNetsimNode(Node node, InternalInterface internalInterface) {
			QNodeImpl.Builder builder = new QNodeImpl.Builder( internalInterface, context ) ;
			return builder.build( node ) ;
			
		}
		@Override QLinkI createNetsimLink(Link link, QNodeI queueNode, InternalInterface internalInterface) {
			QueueWithBuffer.Builder laneBuilder = new QueueWithBuffer.Builder(context) ;
			
			QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, internalInterface) ;
			linkBuilder.setLaneFactory(laneBuilder);
			
			return linkBuilder.build(link, queueNode) ;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig() ;
		
		Scenario scenario = ScenarioUtils.createScenario( config ) ;
		
		Controler controler = new Controler( scenario ) ;
		
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				bind( QNetworkFactory.class ).to( MyQNetworkFactory.class ) ;
			}
		});
		
		controler.run();
		
	}

}
