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

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemConfigurationImpl implements BasicSignalSystemConfiguration {

	private final Id lightSignalSystemId;

	private BasicSignalSystemControlInfo controlInfo;

	public BasicSignalSystemConfigurationImpl(final Id lightSignalSystemId) {
		this.lightSignalSystemId = lightSignalSystemId;
	}

	/**
	 * @see org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration#setSignalSystemControlInfo(org.matsim.basic.signalsystemsconfig.BasicSignalSystemControlInfo)
	 */
	public void setSignalSystemControlInfo(
			final BasicSignalSystemControlInfo controlInfo) {
		this.controlInfo = controlInfo;
	}

	/**
	 * @see org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration#getSignalSystemId()
	 */
	public Id getSignalSystemId() {
		return this.lightSignalSystemId;
	}

	/**
	 * @see org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration#getControlInfo()
	 */
	public BasicSignalSystemControlInfo getControlInfo() {
		return this.controlInfo;
	}



}
