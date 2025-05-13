/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.extension.edrt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.VrpPathEnergyConsumptions;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * @author michalm
 */
public class EDrtTaskFactoryImpl implements DrtTaskFactory {
	@Override
	public EDrtDriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path, DrtTaskType taskType) {
		ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
		double totalEnergy = VrpPathEnergyConsumptions.calcTotalEnergy(ev, path, path.getDepartureTime());
		return new EDrtDriveTask(path, taskType, totalEnergy);
	}

	@Override
	public EDrtStopTask createStopTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
		double auxEnergy = ev.getAuxEnergyConsumption()
				.calcEnergyConsumption(beginTime, endTime - beginTime, link.getId());
		return new EDrtStopTask(beginTime, endTime, link, auxEnergy);
	}

	@Override
	public EDrtStayTask createStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		return new EDrtStayTask(beginTime, endTime, link, 0);// no energy consumption during STAY
	}

	@Override
	public DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
		return createStayTask(vehicle, beginTime, endTime, link);
	}

	public EDrtChargingTask createChargingTask(DvrpVehicle vehicle, double beginTime, double endTime, Charger charger,
			double totalEnergy, ChargingStrategy chargingStrategy) {
		return new EDrtChargingTask(beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(),
				totalEnergy, chargingStrategy);
	}
}
