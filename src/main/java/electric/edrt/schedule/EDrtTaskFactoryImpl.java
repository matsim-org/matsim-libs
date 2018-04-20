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

package electric.edrt.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;
import org.matsim.vsp.ev.dvrp.EvDvrpVehicle;
import org.matsim.vsp.ev.dvrp.TaskEnergyConsumptions;

/**
 * @author michalm
 */
public class EDrtTaskFactoryImpl implements DrtTaskFactory {
	@Override
	public EDrtDriveTask createDriveTask(Vehicle vehicle, VrpPathWithTravelData path) {
		ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
		double totalEnergy = TaskEnergyConsumptions.calcTotalEnergyConsumption(ev, path);
		return new EDrtDriveTask(path, totalEnergy);
	}

	@Override
	public EDrtStopTask createStopTask(Vehicle vehicle, double beginTime, double endTime, Link link) {
		ElectricVehicle ev = ((EvDvrpVehicle)vehicle).getElectricVehicle();
		double auxEnergy = TaskEnergyConsumptions.calcAuxEnergy(ev, beginTime, endTime);
		return new EDrtStopTask(beginTime, endTime, link, auxEnergy);
	}

	@Override
	public EDrtStayTask createStayTask(Vehicle vehicle, double beginTime, double endTime, Link link) {
		return new EDrtStayTask(beginTime, endTime, link, 0);// no energy consumption during STAY
	}

	public EDrtChargingTask createChargingTask(Vehicle vehicle, double beginTime, double endTime, Charger charger,
			double totalEnergy) {
		return new EDrtChargingTask(beginTime, endTime, charger, ((EvDvrpVehicle)vehicle).getElectricVehicle(),
				totalEnergy);
	}
}
