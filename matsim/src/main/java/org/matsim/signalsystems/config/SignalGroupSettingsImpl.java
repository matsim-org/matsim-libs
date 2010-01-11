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

package org.matsim.signalsystems.config;

import org.matsim.api.core.v01.Id;

/**
 * @author dgrether
 */
public class SignalGroupSettingsImpl implements SignalGroupSettings {

	private Id referencedSignalGroupId;


	private Integer roughCast;
	private Integer dropping;
	/**
	 * default according to xml v1.1
	 */
	private Integer interGreenTimeRoughcast = 0;
	/**
	 * default according to xml v1.1
	 */
	private Integer interGreenTimeDropping = 0;
	
	
	public SignalGroupSettingsImpl(Id referencedSignalGroupId) {
		this.referencedSignalGroupId = referencedSignalGroupId;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#getRoughCast()
	 */
	public Integer getRoughCast() {
		return roughCast;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#setRoughCast(java.lang.Integer)
	 */
	public void setRoughCast(Integer roughCast) {
		this.roughCast = roughCast;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#getDropping()
	 */
	public Integer getDropping() {
		return dropping;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#setDropping(java.lang.Integer)
	 */
	public void setDropping(Integer dropping) {
		this.dropping = dropping;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#getInterimGreenTimeRoughcast()
	 */
	public Integer getInterimGreenTimeRoughcast() {
		return interGreenTimeRoughcast;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#setInterGreenTimeRoughcast(java.lang.Integer)
	 */
	public void setInterGreenTimeRoughcast(Integer interimTimeRoughcast) {
		this.interGreenTimeRoughcast = interimTimeRoughcast;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#getInterGreenTimeDropping()
	 */
	public Integer getInterGreenTimeDropping() {
		return interGreenTimeDropping;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#setInterGreenTimeDropping(java.lang.Integer)
	 */
	public void setInterGreenTimeDropping(Integer interimTimeDropping) {
		this.interGreenTimeDropping = interimTimeDropping;
	}


	
	/**
	 * @see org.matsim.signalsystems.config.SignalGroupSettings#getReferencedSignalGroupId()
	 */
	public Id getReferencedSignalGroupId() {
		return referencedSignalGroupId;
	}
	

}
