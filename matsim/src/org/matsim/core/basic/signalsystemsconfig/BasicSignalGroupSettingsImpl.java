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

package org.matsim.core.basic.signalsystemsconfig;

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalGroupSettingsImpl implements BasicSignalGroupSettings {

	private Id referencedSignalGroupId;


	private Integer roughCast;
	private Integer dropping;

	private Integer interimTimeRoughcast = null;
	private Integer interimTimeDropping = null;
	
	
	public BasicSignalGroupSettingsImpl(Id referencedSignalGroupId) {
		this.referencedSignalGroupId = referencedSignalGroupId;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#getRoughCast()
	 */
	public Integer getRoughCast() {
		return roughCast;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#setRoughCast(java.lang.Integer)
	 */
	public void setRoughCast(Integer roughCast) {
		this.roughCast = roughCast;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#getDropping()
	 */
	public Integer getDropping() {
		return dropping;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#setDropping(java.lang.Integer)
	 */
	public void setDropping(Integer dropping) {
		this.dropping = dropping;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#getInterimGreenTimeRoughcast()
	 */
	public Integer getInterimGreenTimeRoughcast() {
		return interimTimeRoughcast;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#setInterGreenTimeRoughcast(java.lang.Integer)
	 */
	public void setInterGreenTimeRoughcast(Integer interimTimeRoughcast) {
		this.interimTimeRoughcast = interimTimeRoughcast;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#getInterGreenTimeDropping()
	 */
	public Integer getInterGreenTimeDropping() {
		return interimTimeDropping;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#setInterGreenTimeDropping(java.lang.Integer)
	 */
	public void setInterGreenTimeDropping(Integer interimTimeDropping) {
		this.interimTimeDropping = interimTimeDropping;
	}


	
	/**
	 * @see org.matsim.core.basic.signalsystemsconfig.BasicSignalGroupSettings#getReferencedSignalGroupId()
	 */
	public Id getReferencedSignalGroupId() {
		return referencedSignalGroupId;
	}
	

}
