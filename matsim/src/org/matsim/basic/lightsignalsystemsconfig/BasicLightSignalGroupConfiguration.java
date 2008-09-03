/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.basic.lightsignalsystemsconfig;

import org.matsim.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicLightSignalGroupConfiguration {

	private Id referencedSignalGroupId;


	private double roughCast;
	private double dropping;

	private double interimTimeRoughcast = Double.NaN;
	private double interimTimeDropping = Double.NaN;
	
	
	public BasicLightSignalGroupConfiguration(Id referencedSignalGroupId) {
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


	
	public double getInterimTimeRoughcast() {
		return interimTimeRoughcast;
	}


	
	public void setInterimTimeRoughcast(double interimTimeRoughcast) {
		this.interimTimeRoughcast = interimTimeRoughcast;
	}


	
	public double getInterimTimeDropping() {
		return interimTimeDropping;
	}


	
	public void setInterimTimeDropping(double interimTimeDropping) {
		this.interimTimeDropping = interimTimeDropping;
	}


	
	public Id getReferencedSignalGroupId() {
		return referencedSignalGroupId;
	}
	

}
