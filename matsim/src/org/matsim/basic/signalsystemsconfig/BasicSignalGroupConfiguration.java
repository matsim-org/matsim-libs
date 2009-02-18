/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.basic.signalsystemsconfig;

import org.matsim.interfaces.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalGroupConfiguration {

	private Id referencedSignalGroupId;


	private double roughCast;
	private double dropping;

	private Double interimTimeRoughcast = null;
	private Double interimTimeDropping = null;
	
	
	public BasicSignalGroupConfiguration(Id referencedSignalGroupId) {
		this.referencedSignalGroupId = referencedSignalGroupId;
	}


	
	public double getRoughCast() {
		return roughCast;
	}


	
	public void setRoughCast(double roughCast) {
		this.roughCast = roughCast;
	}


	
	public double getDropping() {
		return dropping;
	}


	
	public void setDropping(double dropping) {
		this.dropping = dropping;
	}


	
	public Double getInterimTimeRoughcast() {
		return interimTimeRoughcast;
	}


	
	public void setInterimTimeRoughcast(Double interimTimeRoughcast) {
		this.interimTimeRoughcast = interimTimeRoughcast;
	}


	
	public Double getInterimTimeDropping() {
		return interimTimeDropping;
	}


	
	public void setInterimTimeDropping(Double interimTimeDropping) {
		this.interimTimeDropping = interimTimeDropping;
	}


	
	public Id getReferencedSignalGroupId() {
		return referencedSignalGroupId;
	}
	

}
