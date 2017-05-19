/* *********************************************************************** *
 * project: org.matsim.*
 * GfipMultimodalQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

/**
 * Class to set up (provide) the Mobsim for the Gauteng Freeway Improvement 
 * Project (GFIP) mode with multiple modes and different passing queue types.
 *  
 * @author jwjoubert
 */
public class GfipMultimodalQSimFactory implements Provider<Mobsim> {
	final private Logger log = Logger.getLogger(GfipMultimodalQSimFactory.class);
	final private QueueType queueType;
	
	public GfipMultimodalQSimFactory(QueueType queueType) {
		this.queueType = queueType;
		switch (queueType) {
		case FIFO:
			log.info("------------------------------------------------------------------");
			log.info("  Using first-in-first-out (FIFO) queue in mobility simulation."); 
			log.info("------------------------------------------------------------------");
			break;
		case BASIC_PASSING:
			log.info("------------------------------------------------------------------");
			log.info("  Using priority queue (allowing passing) in mobility simulation."); 
			log.info("------------------------------------------------------------------");
			break;
		case GFIP_PASSING:
			log.info("---------------------------------------------------------------------------------------");
			log.info("  Using link density-based priority queue (allowing passing) in mobility simulation."); 
			log.info("---------------------------------------------------------------------------------------");
			break;
		case GFIP_FIFO:
			log.info("---------------------------------------------------------------------------------------");
			log.info("  Using link density-based velocity fifo queue in mobility simulation."); 
			log.info("---------------------------------------------------------------------------------------");
			break;
		default:
			throw new IllegalArgumentException("Do not know what to do with queue type " + queueType.toString());
		}
	}
	
	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;

	@Override
	public Mobsim get() {
		final QSimConfigGroup configGroup = scenario.getConfig().qsim();
		if (configGroup == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		/* Set up the QSim. */
		QSim qSim = new QSim(scenario, eventsManager);
		/* Add the activity engine. */
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		LinkSpeedCalculator linkSpeedCalculator = new DefaultLinkSpeedCalculator() ;
		/* Add the custom GFIP link speed calculator, but only when required. */
		if(queueType == QueueType.FIFO){
			log.info("------------------------------------------------------------------------------");
			log.info("  Using basic FIFO link speed calculator. "); 
			log.info("------------------------------------------------------------------------------");
			linkSpeedCalculator = new DefaultLinkSpeedCalculator();
		} else if(queueType == QueueType.BASIC_PASSING){
			log.info("------------------------------------------------------------------------------");
			log.info("  Using basic passing link speed calculator. "); 
			log.info("------------------------------------------------------------------------------");
			linkSpeedCalculator = new GfipLinkSpeedCalculator(scenario.getVehicles(), qSim, queueType) ;
		} else if(queueType == QueueType.GFIP_PASSING){
			log.info("------------------------------------------------------------------------------");
			log.info("  Using custom GFIP-link-density-based link speed calculator with passing. "); 
			log.info("------------------------------------------------------------------------------");
			linkSpeedCalculator = new GfipLinkSpeedCalculator(scenario.getVehicles(), qSim, queueType) ;
		} else if(queueType == QueueType.GFIP_FIFO){
			log.info("------------------------------------------------------------------------------");
			log.info("  Using custom GFIP-link-density-based link speed calculator without passing."); 
			log.info("------------------------------------------------------------------------------");
			linkSpeedCalculator = new GfipLinkSpeedCalculator(scenario.getVehicles(), qSim, queueType) ;
		}
		final LinkSpeedCalculator theLinkSpeedCalculator = linkSpeedCalculator ; // to get this final

		
		/* This is the crucial part for changing the queue type. */ 
		QNetworkFactory netsimNetworkFactory = new QNetworkFactory() {
			private NetsimEngineContext context;
			private NetsimInternalInterface netsimEngine;
			@Override
			void initializeFactory(AgentCounter agentCounter, MobsimTimer mobsimTimer, NetsimInternalInterface netsimEngine1) {
				double effectiveCellSize = ((Network) scenario.getNetwork()).getEffectiveCellSize() ;
				
				SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
				linkWidthCalculator.setLinkWidthForVis( configGroup.getLinkWidthForVis() );
				if (! Double.isNaN(scenario.getNetwork().getEffectiveLaneWidth())){
					linkWidthCalculator.setLaneWidth( scenario.getNetwork().getEffectiveLaneWidth() );
				}
				AgentSnapshotInfoFactory snapshotInfoFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
				AbstractAgentSnapshotInfoBuilder snapshotInfoBuilder = QNetsimEngine.createAgentSnapshotInfoBuilder( scenario, linkWidthCalculator );
				
				this.context = new NetsimEngineContext(eventsManager, effectiveCellSize, agentCounter, snapshotInfoBuilder, configGroup, mobsimTimer, 
						linkWidthCalculator ) ;
				this.netsimEngine = netsimEngine1 ;
			}
			@Override
			public QLinkImpl createNetsimLink(final Link link, final QNode toQueueNode) {
				VehicleQ<QVehicle> vehicleQ = null;
				switch (queueType) {
				case FIFO:
				case GFIP_FIFO:
					vehicleQ = new FIFOVehicleQ();
					break;
				case BASIC_PASSING:
				case GFIP_PASSING:
					vehicleQ = new PassingVehicleQ();
					break;
				default:
					throw new RuntimeException("Do not know what VehicleQ to use with queue type " + queueType.toString());
				}
				// vehicleQueue and speedCalculator are at the level of the lane:
				QueueWithBuffer.Builder laneBuilder = new QueueWithBuffer.Builder(context) ;
				laneBuilder.setVehicleQueue(vehicleQ);
				laneBuilder.setLinkSpeedCalculator(theLinkSpeedCalculator);

				// insert the lane into the link:
				QLinkImpl.Builder linkBuilder = new QLinkImpl.Builder(context, netsimEngine) ;
				linkBuilder.setLaneFactory(laneBuilder);
				return linkBuilder.build(link, toQueueNode) ;
			}
			@Override
			public QNode createNetsimNode(final Node node) {
				QNode.Builder builder = new QNode.Builder( netsimEngine, context ) ;
				return builder.build( node ) ;
			}
		};
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, netsimNetworkFactory);
		
	
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		
		/* ..Update the PopulationAgentSource to ensure the correct vehicle is 
		 * passed to the mobsim, not some default-per-mode vehicle. */		
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);

		//     	since the vehicles are already in scenario.getVehicles, they are not required to add again here. setter for modeVehicleTypes to agent source is gone. Amit May'17
// 		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();
//		for(Id<VehicleType> id : scenario.getVehicles().getVehicleTypes().keySet()){
//			modeVehicleTypes.put(id.toString(), scenario.getVehicles().getVehicleTypes().get(id));
//		}
//		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);

		return qSim;		
	}

	public enum QueueType{
		FIFO, BASIC_PASSING, GFIP_PASSING, GFIP_FIFO;
	}


}
