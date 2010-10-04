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
package org.matsim.signalsystems.model;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.ptproject.qsim.interfaces.QLink;
import org.matsim.ptproject.qsim.interfaces.QNetworkI;
import org.matsim.ptproject.qsim.interfaces.QSimI;
import org.matsim.ptproject.qsim.netsimengine.QLane;
import org.matsim.ptproject.qsim.netsimengine.QLinkImpl;
import org.matsim.ptproject.qsim.netsimengine.QLinkLanesImpl;


/**
 * @author dgrether
 *
 */
public class QSimSignalEngine implements SignalEngine {

	private SignalSystemsManager signalManager;

	public QSimSignalEngine(SignalSystemsManager signalManager) {
		this.signalManager = signalManager;
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		this.initializeSignalizedItems(((QSimI)e.getQueueSimulation()));
	}


	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		this.signalManager.requestControlUpdate(e.getSimulationTime());
	}
	
	private void initializeSignalizedItems(QSimI qSim) {
		QNetworkI net = qSim.getQNetwork();
		for (SignalSystem system : this.signalManager.getSignalSystems().values()){
			for (Signal signal : system.getSignals().values()){
				QLink link = net.getLinks().get(signal.getLinkId());
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					QLinkImpl l = (QLinkImpl) link;
					//TODO l.setSignalized()
					signal.addSignalizeableItem((SignalizeableItem)l);
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
		}
	}

	public QLane getQLane(Id laneId, QLinkLanesImpl link){
		for (QLane lane : link.getQueueLanes()){
			if (lane.getId().equals(laneId)){
				return lane;
			}
		}
		throw new IllegalArgumentException("QLane not found");
	}

	
	
}
