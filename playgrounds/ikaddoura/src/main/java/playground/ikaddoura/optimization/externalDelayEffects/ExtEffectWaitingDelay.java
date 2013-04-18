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
package playground.ikaddoura.optimization.externalDelayEffects;

import org.matsim.api.core.v01.Id;

/**
 * Collects the information of the external waiting delay effect for each person.
 * Saves the marginal (boarding or alighting) delay in seconds and the Id of the delayed public vehicle.
 * During tracking the external delay effect (person and vehicle) the number of affected agents will be increased.
 * Every other agent entering or leaving that delayed public vehicle is affected.
 * 
 * @author ikaddoura
 *
 */
public class ExtEffectWaitingDelay {
	
	private Id personId;
	private double affectedAgents;
	private Id affectedVehicle;
	private double transferDelay;
	
	public double getTransferDelay() {
		return transferDelay;
	}
	public void setDelay(double transferDelay) {
		this.transferDelay = transferDelay;
	}
	public Id getAffectedVehicle() {
		return affectedVehicle;
	}
	public void setAffectedVehicle(Id affectedVehicle) {
		this.affectedVehicle = affectedVehicle;
	}
	public double getAffectedAgents() {
		return affectedAgents;
	}
	public void setAffectedAgents(double d) {
		this.affectedAgents = d;
	}
	public Id getPersonId() {
		return personId;
	}
	public void setPersonId(Id personId) {
		this.personId = personId;
	}
	
	@Override
	public String toString() {
		return "PersonId = " + this.personId + "; AffectedAgents = " + this.affectedAgents + "; AffectedVehicle = " + this.affectedVehicle + "; TransferDelay = " + this.transferDelay;
	}
	
}
