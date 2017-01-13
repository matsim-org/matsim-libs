/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.laemmer.model;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.lanes.data.Lane;

import playground.dgrether.signalsystems.LinkSensorManager;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class LaemmerSignalController  extends AbstractSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(LaemmerSignalController.class);
	
	public static final String IDENTIFIER = "LaemmerSignalControl";
	
	public final static class Builder {
		private final LinkSensorManager sensorManager;
		private final SignalsData signalsData;
		private final Network network;

		public Builder(LinkSensorManager sensorManager, SignalsData signalsData, Network network) {
			this.sensorManager = sensorManager;
			this.signalsData = signalsData;
			this.network = network;
		}

		public LaemmerSignalController build(SignalSystem signalSystem) {
			return new LaemmerSignalController(sensorManager, signalsData, network, signalSystem);
		}
	}	
	
	private LinkSensorManager sensorManager;
	private SignalsData signalsData;
	private Network network;

	
	private LaemmerSignalController(LinkSensorManager sensorManager, SignalsData signalsData, Network network, SignalSystem system){
		super(system) ;
		this.sensorManager = sensorManager;
		this.signalsData = signalsData;
		this.network = network;
	}
	
	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		this.initializeSensoring();

		for (SignalGroup group : this.system.getSignalGroups().values()){
			this.system.scheduleDropping(simStartTimeSeconds, group.getId());
		}
		
	}
	
	@Override
	public void updateState(double timeSeconds) {
		
		for (SignalGroup group : this.system.getSignalGroups().values()){
			this.calculatePriorityIndex(timeSeconds);
			
		}
		
	}

	
	private void calculatePriorityIndex(double timeSeconds) {
		this.getNumberOfExpectedVehicles(timeSeconds);
		
	}

	/**
	 * 
	 * @return \hat{n_i} (t)
	 */
	private int getNumberOfVehiclesForClearance(){
		
		return 0;
	}
	
	
	/**
	 * Zeitreihe der erwarteten Ankuenfte an der Haltelinie
	 * 
	 * N_i^{exp}(t + \hat(g)_i))
	 * 
	 */
	private int getNumberOfExpectedVehicles(double timeSeconds){
		return 0;
	}
	
	
	@Override
	public void reset(Integer iterationNumber) {
	}

	
	private void initializeSensoring(){
		for (SignalGroup group :  this.system.getSignalGroups().values() ){
			for (Signal signal : group.getSignals().values()) {
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()){
					//					log.error("system: " + this.system.getId() + " signal: " + signal.getId() + " has no lanes...");
					this.sensorManager.registerNumberOfCarsMonitoring(signal.getLinkId());
				}
				else {
					for (Id<Lane> laneId : signal.getLaneIds()){
						//TODO check this part again concerning implementation of CarsOnLaneHandler
						this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
					}
				}

			}
		}
	}
	
	

}
