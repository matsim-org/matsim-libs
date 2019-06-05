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

package org.matsim.contrib.ev.dvrp;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * @author michalm
 */
public class VrpPathEnergyConsumptions {
	public static double calcTotalEnergy(ElectricVehicle ev, VrpPath path, double beginTime) {
		return calcRemainingTotalEnergy(ev, path, 0, beginTime);
	}

	public static double calcRemainingTotalEnergy(ElectricVehicle ev, VrpPath path, int currentLinkIdx,
			double currentLinkEnterTime) {
		DriveEnergyConsumption consumption = ev.getDriveEnergyConsumption();
		AuxEnergyConsumption auxConsumption = ev.getAuxEnergyConsumption();

		double energy = 0;
		double linkEnterTime = currentLinkEnterTime + (currentLinkIdx == 0 ? 1 : 0);// skip first link
		for (int i = Math.max(currentLinkIdx, 1); i < path.getLinkCount(); i++) {// skip first link
			Link link = path.getLink(i);
			double linkTravelTime = path.getLinkTravelTime(i);
			energy += consumption.calcEnergyConsumption(link, linkTravelTime, linkEnterTime)
					+ auxConsumption.calcEnergyConsumption(linkEnterTime, linkTravelTime, link.getId());
			linkEnterTime += linkTravelTime;
		}
		return energy;
	}
}
