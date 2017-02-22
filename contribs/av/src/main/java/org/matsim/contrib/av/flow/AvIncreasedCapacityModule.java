/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.av.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vehicles.*;

import com.google.inject.name.Names;

public class AvIncreasedCapacityModule extends AbstractModule {
	private final double flowEfficiencyFactor;

	public AvIncreasedCapacityModule(double flowEfficiencyFactor) {
		this.flowEfficiencyFactor = flowEfficiencyFactor;
	}

	@Override
	public void install() {
		VehicleType avType = new VehicleTypeImpl(Id.create("autonomousVehicleType", VehicleType.class));
		avType.setFlowEfficiencyFactor(flowEfficiencyFactor);
		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSource.DVRP_VEHICLE_TYPE)).toInstance(avType);
	}
}
