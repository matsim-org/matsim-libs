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

import org.matsim.signalsystems.model.SignalController;

import playground.dgrether.signalsystems.utils.DgAbstractSignalController;


/**
 * @author dgrether
 *
 */
public class DgTaController  extends DgAbstractSignalController implements SignalController {

	public static final String IDENTIFIER = "LaemmerSignalSystemController";
	
	@Override
	public void updateState(double timeSeconds) {
		
		
		// for all approaches
		this.calculatePriorityIndex(timeSeconds);
		
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

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
	}
	
	

}
