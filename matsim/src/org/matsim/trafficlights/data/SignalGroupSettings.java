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
package org.matsim.trafficlights.data;



/**
 * @author dgrether
 *
 */
public class SignalGroupSettings {

	private SignalGroupDefinition signalGroupDefinition;

	private int roughCast;
	private int dropping;

	private int interimTimeRoughcast;
	private int interimTimeDropping;


	public SignalGroupSettings(SignalGroupDefinition def) {
		this.signalGroupDefinition = def;
	}

	/**
	 * @return the roughCast
	 */
	public int getRoughCast() {
		return this.roughCast;
	}



	/**
	 * @param roughCast the roughCast to set
	 */
	public void setRoughCast(int roughCast) {
		this.roughCast = roughCast;
	}



	/**
	 * @return the dropping
	 */
	public int getDropping() {
		return this.dropping;
	}



	/**
	 * @param dropping the dropping to set
	 */
	public void setDropping(int dropping) {
		this.dropping = dropping;
	}



	/**
	 * @return the interimTimeRoughcast
	 */
	public int getInterimTimeRoughcast() {
		return this.interimTimeRoughcast;
	}



	/**
	 * @param interimTimeRoughcast the interimTimeRoughcast to set
	 */
	public void setInterimTimeRoughcast(int interimTimeRoughcast) {
		this.interimTimeRoughcast = interimTimeRoughcast;
	}



	/**
	 * @return the interimTimeDropping
	 */
	public int getInterimTimeDropping() {
		return this.interimTimeDropping;
	}



	/**
	 * @param interimTimeDropping the interimTimeDropping to set
	 */
	public void setInterimTimeDropping(int interimTimeDropping) {
		this.interimTimeDropping = interimTimeDropping;
	}



	/**
	 * @return the signalGroupDefinition
	 */
	public SignalGroupDefinition getSignalGroupDefinition() {
		return this.signalGroupDefinition;
	}



}
