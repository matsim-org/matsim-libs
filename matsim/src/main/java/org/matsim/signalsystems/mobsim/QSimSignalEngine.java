/* *********************************************************************** *
 * project: org.matsim.*
 * SignalEngineImpl
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
package org.matsim.signalsystems.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.ptproject.qsim.interfaces.Mobsim;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.qnetsimengine.QLane;
import org.matsim.ptproject.qsim.qnetsimengine.QLinkImpl;
import org.matsim.ptproject.qsim.qnetsimengine.QLinkLanesImpl;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;


/**
 * @author dgrether
 *
 */
public class QSimSignalEngine implements SignalEngine {

	private static final Logger log = Logger.getLogger(QSimSignalEngine.class);

	private SignalSystemsManager signalManager;

	public QSimSignalEngine(SignalSystemsManager signalManager) {
		this.signalManager = signalManager;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.initializeSignalizedItems(((Mobsim)e.getQueueSimulation()));
	}


	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.signalManager.requestControlUpdate(e.getSimulationTime());
	}
	
	private void initializeSignalizedItems(Mobsim qSim) {
		NetsimNetwork net = qSim.getNetsimNetwork();
		for (SignalSystem system : this.signalManager.getSignalSystems().values()){
			for (Signal signal : system.getSignals().values()){
				log.debug("initializing signal " + signal.getId() + " on link " + signal.getLinkId());
				NetsimLink link = net.getNetsimLinks().get(signal.getLinkId());
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					QLinkImpl l = (QLinkImpl) link;
					l.setSignalized(true);
					signal.addSignalizeableItem(l);
				}
				else {
					QLinkLanesImpl l = (QLinkLanesImpl) link;
					for (Id laneId : signal.getLaneIds()){
						QLane lane = getQLane(laneId, l);
						lane.setSignalized(true);
						signal.addSignalizeableItem(lane);
					}
				}
			}
			system.simulationInitialized(qSim.getSimTimer().getTimeOfDay());
		}
	}

	private QLane getQLane(Id laneId, QLinkLanesImpl link){
		for (QLane lane : link.getQueueLanes()){
			if (lane.getId().equals(laneId)){
				return lane;
			}
		}
		throw new IllegalArgumentException("QLane Id " + laneId + "on link Id" + link.getLink().getId() + "  not found. Check configuration!");
	}

	
	
}
