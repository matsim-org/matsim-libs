/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.eMobility.v2.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author droeder
 *
 */
public class EmobVehicle{
	private Id id;
	private Id position;
	private boolean changed = false;
	private double soc;
	private Id disChargingMode;
	
	public EmobVehicle(Id id,Link position, double soc){
		this.id = id;
		this.position = position.getId(); 
		this.soc = 0.0;
		this.disChargingMode = new IdImpl("NONE");
	}

	/**
	 * @return
	 */
	public Id getId() {
		return this.id;
	}
	
	public Id getPositionLinkId(){
		return this.position;
	}
	
	public double getCurrentSoC(){
		return this.soc;
	}
	
	public void setSoC(Double soc){
		this.changed = true;
		this.soc = soc;
	}
	
	public boolean changedSoC(){
		if(this.changed){
			this.changed = false;
			return true;
		}
		return false;
	}

	public void setCurrentDisChargingMode(Id id){
		this.disChargingMode = id;
	}
	public Id getCurrentDischargingMode() {
		return this.disChargingMode;
	}

}
