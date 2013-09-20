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
	
	public Id modeId;
	private VehicleType vehicleType;

	public int numberOfAgents;
	private Map<Id,Double> lastSeenOnStudiedLinkEnter;
	private List<Double> speedTable;
	private Double flowTime;
	private List<Double> flowTable;
	
	/*public ModeData(){
		
	}*/
	
	public ModeData(Id id, VehicleType vT){
		this.modeId = id;
		this.vehicleType = vT;
		this.initDynamicVariables();
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
		// TODO Auto-generated method stub
	}

	@Override
	public String toString(){
		VehicleType vT = this.vehicleType;
		String str = "(id="+this.modeId+", max_v="+vT.getMaximumVelocity()+", pcu="+vT.getPcuEquivalents()+")";
		return str;
	}

}
