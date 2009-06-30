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

import org.matsim.api.basic.v01.Id;

/**
 * @author dgrether
 */
public class BasicSignalSystemConfigurationsBuilder {

	public BasicSignalSystemConfigurationsBuilder(){}
	
	public BasicSignalSystemConfiguration createSignalSystemConfiguration(
			Id refId) {
		return new BasicSignalSystemConfigurationImpl(refId);
	}

	public BasicPlanBasedSignalSystemControlInfo createPlanBasedSignalSystemControlInfo() {
		return new BasicPlanBasedSignalSystemControlInfoImpl();
	}

	public BasicSignalSystemPlan createSignalSystemPlan(Id id) {
		return new BasicSignalSystemPlanImpl(id);
	}

	public BasicSignalGroupSettings createSignalGroupSettings(
			Id refid) {
		return new BasicSignalGroupSettingsImpl(refid);
	}

	public BasicAdaptiveSignalSystemControlInfo createAdaptiveSignalSystemControlInfo() {
		return new BasicAdaptiveSignalSystemControlInfoImpl();
	}

	public BasicAdaptivePlanbasedSignalSystemControlInfoImpl createAdaptivePlanbasedSignalSystemControlInfo() {
		return new BasicAdaptivePlanbasedSignalSystemControlInfoImpl();
	}

}
