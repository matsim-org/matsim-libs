/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisSignalsMobsimFeature
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
package playground.dgrether.signalsystems;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.vis.otfvis.OTFVisMobsimFeature;
import org.matsim.vis.snapshots.writers.VisMobsim;


/**
 * @author dgrether
 *
 */
public class OTFVisSignalsMobsimFeature extends OTFVisMobsimFeature implements SimulationBeforeSimStepListener {
	
	private static final Logger log = Logger.getLogger(OTFVisSignalsMobsimFeature.class);
	/**
	 * @param queueSimulation
	 */
	public OTFVisSignalsMobsimFeature(VisMobsim visSim) {
		super(visSim);
		
	}
	
	
	
	@Override
	public void notifySimulationInitialized(@SuppressWarnings("unused") SimulationInitializedEvent ev) {
		super.notifySimulationInitialized(ev);
		log.error("initialized");
//		this.otfServer.getQuad(id, connect);
	}

	
//	private void init(){
//		for (SignalSystem system : this.signalManager.getSignalSystems().values()){
//			for (Signal signal : system.getSignals().values()){
//				QLink link = net.getLinks().get(signal.getLinkId());
//				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
//					QLinkImpl l = (QLinkImpl) link;
//					l.setSignalized(true);
//					signal.addSignalizeableItem(l);
//				}
//				else {
//					QLinkLanesImpl l = (QLinkLanesImpl) link;
//					for (Id laneId : signal.getLaneIds()){
//						for (QLane lane : link.getQueueLanes()){
//							if (lane.getId().equals(laneId)){
//								return lane;
//							}
//						}
//
//						
//						QLane lane = getQLane(laneId, l);
//						lane.setSignalized(true);
//						signal.addSignalizeableItem(lane);
//					}
//				}
//			}
//		}
//	}


	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
	}

}
