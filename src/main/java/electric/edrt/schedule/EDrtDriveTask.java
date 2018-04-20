/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

/**
 * @author michalm
 */
public class EDrtDriveTask extends DrtDriveTask implements EDrtTask {
	private final double consumedEnergy;

	public EDrtDriveTask(VrpPathWithTravelData path, double consumedEnergy) {
		super(path);
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return consumedEnergy;
	}
}
