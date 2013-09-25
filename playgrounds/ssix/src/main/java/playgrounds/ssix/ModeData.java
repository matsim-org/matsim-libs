/* *********************************************************************** *
 * project: org.matsim.*
 * ModeData													   *
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

package playgrounds.ssix;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.vehicles.VehicleType;

/**
 * @author ssix
 * A class intended to contain "static" mode-dependent data (vehicle type, speed etc.)
 * as well as dynamic data used in the mobsim (speed, flow of the mode)
 * as well as methods to store and update this data.
 */

public class ModeData {
	
	private Id modeId;
	private VehicleType vehicleType;//TODO Ensure all methods can work without a specific vehicle type (needed for storing global data)
									//TODO Maybe keeping global data in the EventHandler can be smart (ssix, 25.09.13)
	public int numberOfAgents;
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;
	private int speedTableSize;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable;
	
	public ModeData(Id id, VehicleType vT){
		this.modeId = id;
		this.vehicleType = vT;
	}
	
	public void handle(LinkEnterEvent e){
		//TODO
	}
	
	public VehicleType getVehicleType(){
		return this.vehicleType;
	}
	
	public Id getModeId(){
		return this.modeId;
	}
	
	public void setnumberOfAgents(int n){
		this.numberOfAgents = n;
	}
	
	private void initDynamicVariables() {
		// TODO Ensure numberOfAgents for each mode has been initialized at this point
		this.decideSpeedTableSize();
		this.speedTable = new LinkedList<Double>();
		for (int i=0; i<this.speedTableSize; i++){
			this.speedTable.add(0.);
		}
		
		this.flowTime = 0.;
		this.flowTable = new LinkedList<Double>();
		for (int i=0; i<3600; i++){
			this.flowTable.add(0.);
		}
	}

	private void decideSpeedTableSize() {
		//Ensures a significant speed sampling for every mode size
		if (this.numberOfAgents >= 100) {
			this.speedTableSize = 50;
		} else if (this.numberOfAgents >= 60) {
			this.speedTableSize = 30;
		} else if (this.numberOfAgents >= 20) {
			this.speedTableSize = 20;
		} else {
			this.speedTableSize = this.numberOfAgents;
		}
	}

	@Override
	public String toString(){
		VehicleType vT = this.vehicleType;
		String str = "(id="+this.modeId+", max_v="+vT.getMaximumVelocity()+", pcu="+vT.getPcuEquivalents()+")";
		return str;
	}

}
