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
package playground.dgrether.signalsystems.laemmer.model;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;

import playground.dgrether.signalsystems.DgSensorManager;
import playground.dgrether.signalsystems.utils.DgAbstractSignalController;


/**
 * @author dgrether
 *
 */
public class LaemmerSignalController  extends DgAbstractSignalController implements SignalController {

	private static final Logger log = Logger.getLogger(LaemmerSignalController.class);
	
	public static final String IDENTIFIER = "LaemmerSignalSystemController";
	
	private DgSensorManager sensorManager = null;

	
	public LaemmerSignalController(DgSensorManager sensorManager){
		this.sensorManager = sensorManager;
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
					for (Id laneId : signal.getLaneIds()){
						//TODO check this part again concerning implementation of CarsOnLaneHandler
						this.sensorManager.registerNumberOfCarsMonitoringOnLane(signal.getLinkId(), laneId);
					}
				}

			}
		}
	}
	
	

}
