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
public class BasicSignalSystemsConfigFactory {

	public BasicSignalSystemConfiguration createLightSignalSystemConfiguration(
			Id refId) {
		return new BasicSignalSystemConfiguration(refId);
	}

	public BasicPlanBasedSignalSystemControlInfo createPlanBasedLightSignalSystemControlInfo() {
		return new BasicPlanBasedSignalSystemControlInfo();
	}

	public BasicSignalSystemPlan createLightSignalSystemPlan(Id id) {
		return new BasicSignalSystemPlan(id);
	}

	public BasicSignalGroupConfiguration createLightSignalGroupConfiguration(
			Id refid) {
		return new BasicSignalGroupConfiguration(refid);
	}

}
