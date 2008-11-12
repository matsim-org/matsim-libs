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

package org.matsim.basic.lightsignalsystemsconfig;

import org.matsim.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicLightSignalSystemConfiguration {

	private final Id lightSignalSystemId;

	private BasicPlanBasedLightSignalSystemControlInfo controlInfo;

	public BasicLightSignalSystemConfiguration(final Id lightSignalSystemId) {
		this.lightSignalSystemId = lightSignalSystemId;
	}

	public void setLightSignalSystemControlInfo(
			final BasicPlanBasedLightSignalSystemControlInfo controlInfo) {
		this.controlInfo = controlInfo;
	}

	public Id getLightSignalSystemId() {
		return this.lightSignalSystemId;
	}

	public BasicLightSignalSystemControlInfo getControlInfo() {
		return this.controlInfo;
	}



}
