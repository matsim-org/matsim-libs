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
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author dgrether
 */
public class SignalSystemConfigurationsFactory implements MatsimFactory {

	public SignalSystemConfigurationsFactory(){}
	
	public SignalSystemConfiguration createSignalSystemConfiguration(
			Id refId) {
		return new SignalSystemConfigurationImpl(refId);
	}

	public PlanBasedSignalSystemControlInfo createPlanBasedSignalSystemControlInfo() {
		return new PlanBasedSignalSystemControlInfoImpl();
	}

	public SignalSystemPlan createSignalSystemPlan(Id id) {
		return new SignalSystemPlanImpl(id);
	}

	public SignalGroupSettings createSignalGroupSettings(
			Id refid) {
		return new SignalGroupSettingsImpl(refid);
	}

	public AdaptiveSignalSystemControlInfo createAdaptiveSignalSystemControlInfo() {
		return new AdaptiveSignalSystemControlInfoImpl();
	}

	public AdaptivePlanbasedSignalSystemControlInfoImpl createAdaptivePlanbasedSignalSystemControlInfo() {
		return new AdaptivePlanbasedSignalSystemControlInfoImpl();
	}

}
