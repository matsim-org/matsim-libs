/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.optimization.handler;

import org.matsim.api.core.v01.Id;

/**
 * @author ikaddoura
 *
 */
public class ExtDelayEffect {
	
	private Id personId;
	private double transferDelay;
	private Id affectedVehicle;
	private int affectedAgents;
	
	public double getTransferDelay() {
		return transferDelay;
	}
	public void setTransferDelay(double transferDelay) {
		this.transferDelay = transferDelay;
	}
	public Id getAffectedVehicle() {
		return affectedVehicle;
	}
	public void setAffectedVehicle(Id affectedVehicle) {
		this.affectedVehicle = affectedVehicle;
	}
	public int getAffectedAgents() {
		return affectedAgents;
	}
	public void setAffectedAgents(int affectedAgents) {
		this.affectedAgents = affectedAgents;
	}
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	public double getExtDelay() {
		double extDelay = this.affectedAgents * this.transferDelay;
		return extDelay;
	}

}
