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
public class BasicSignalSystemConfiguration {

	private final Id lightSignalSystemId;

	private BasicPlanBasedSignalSystemControlInfo controlInfo;

	public BasicSignalSystemConfiguration(final Id lightSignalSystemId) {
		this.lightSignalSystemId = lightSignalSystemId;
	}

	public void setLightSignalSystemControlInfo(
			final BasicPlanBasedSignalSystemControlInfo controlInfo) {
		this.controlInfo = controlInfo;
	}

	public Id getLightSignalSystemId() {
		return this.lightSignalSystemId;
	}

	public BasicSignalSystemControlInfo getControlInfo() {
		return this.controlInfo;
	}



}
