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

package playground.michalm.drt.optimizer.insertion;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import playground.michalm.drt.optimizer.DrtOptimizerContext;
import playground.michalm.drt.run.DrtConfigGroup;
import playground.michalm.drt.schedule.NDrtTask;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class InsertionDrtOptimizerWithDepots extends InsertionDrtOptimizer {
	private final Set<Link> startLinks = new HashSet<>();

	public InsertionDrtOptimizerWithDepots(DrtOptimizerContext optimContext, DrtConfigGroup drtCfg,
			InsertionDrtOptimizerParams params) {
		super(optimContext, drtCfg, params);

		for (Vehicle v : optimContext.fleet.getVehicles().values()) {
			startLinks.add(v.getStartLink());
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		super.nextTask(vehicle);

		NDrtTask currentTask = (NDrtTask)vehicle.getSchedule().getCurrentTask();

		// current task is STAY
		if (currentTask != null && currentTask.getDrtTaskType() == NDrtTaskType.STAY) {
			int previousTaskIdx = currentTask.getTaskIdx() - 1;

			// previous task is STOP
			if (previousTaskIdx >= 0 && ((NDrtTask)vehicle.getSchedule().getTasks().get(previousTaskIdx))
					.getDrtTaskType() == NDrtTaskType.STOP) {

				VrpPathWithTravelData relocation = calculateBestRelocation(vehicle);
				if (relocation != null) {
					getOptimContext().scheduler.relocateEmptyVehicle(vehicle, null);
				}
			}
		}
	}

	private VrpPathWithTravelData calculateBestRelocation(Vehicle vehicle) {
		// TODO add something more sophisticated than no relocation:)
		return null;
	}
}
