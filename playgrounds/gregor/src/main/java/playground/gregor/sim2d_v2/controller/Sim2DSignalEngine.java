/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DSignalEngine
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.controller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.signalsystems.model.Signal;
import org.matsim.signalsystems.model.SignalSystem;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.gregor.sim2d_v2.simulation.HybridQ2DMobsimFactory;


/**
 * @author dgrether
 *
 */
public class Sim2DSignalEngine implements SignalEngine {

	private static final Logger log = Logger.getLogger(Sim2DSignalEngine.class);

	private final SignalSystemsManager signalManager;

	private final HybridQ2DMobsimFactory hQ2DFac;

	public Sim2DSignalEngine(SignalSystemsManager signalManager, HybridQ2DMobsimFactory hQ2DFac) {
		this.signalManager = signalManager;
		this.hQ2DFac = hQ2DFac;
	}


	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.initializeSignalizedItems(((Netsim)e.getQueueSimulation()));
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		this.signalManager.requestControlUpdate(e.getSimulationTime());
	}

	private void initializeSignalizedItems(Netsim qSim) {

		Network net = qSim.getNetsimNetwork().getNetwork();

		for (SignalSystem system : this.signalManager.getSignalSystems().values()){
			for (Signal signal : system.getSignals().values()){
				log.debug("initializing signal " + signal.getId() + " on link " + signal.getLinkId());

				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					PedestrianSignal sig = new PedestrianSignal(signal.getLinkId(),net.getLinks().get(signal.getLinkId()).getToNode().getOutLinks().keySet());
					sig.setSignalized(true);
					signal.addSignalizeableItem(sig);
					this.hQ2DFac.getSim2DEngine().addSignal(sig);
				} else {
					throw new RuntimeException("not yet implemented");
				}
			}
			system.simulationInitialized(qSim.getSimTimer().getTimeOfDay());
		}
	}


}
