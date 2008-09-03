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
public class BasicLightSignalSystemsConfigFactory {

	public BasicLightSignalSystemConfiguration createLightSignalSystemConfiguration(
			Id refId) {
		return new BasicLightSignalSystemConfiguration(refId);
	}

	public BasicPlanBasedLightSignalSystemControlInfo createPlanBasedLightSignalSystemControlInfo() {
		return new BasicPlanBasedLightSignalSystemControlInfo();
	}

	public BasicLightSignalSystemPlan createLightSignalSystemPlan(Id id) {
		return new BasicLightSignalSystemPlan(id);
	}

	public BasicLightSignalGroupConfiguration createLightSignalGroupConfiguration(
			Id refid) {
		return new BasicLightSignalGroupConfiguration(refid);
	}

}
