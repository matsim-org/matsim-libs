/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.cadyts.pt;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;

/**
 * @author nagel
 * @author mrieser
 */
/*package*/ final class CadytsBuilder {

	private CadytsBuilder() {
		// private Constructor, should not be instantiated
	}

	/*package*/ static MATSimUtilityModificationCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc, final Counts occupCounts) {
		CadytsPtConfigGroup config = (CadytsPtConfigGroup) sc.getConfig().getModule(CadytsPtConfigGroup.GROUP_NAME);

		if (occupCounts.getCounts().size() == 0) {
			throw new RuntimeException("CadytsPt requires counts-data.");
		}

		double regressionInertia = config.getRegressionInertia();

		MATSimUtilityModificationCalibrator<TransitStopFacility> matsimCalibrator =
				new MATSimUtilityModificationCalibrator<TransitStopFacility>(
						sc.getConfig().controler().getOutputDirectory() + "/cadyts.log",
						MatsimRandom.getLocalInstance(),
						regressionInertia);

		matsimCalibrator.setMinStddev(config.getMinFlowStddev_vehPerHour(), TYPE.FLOW_VEH_H);
		matsimCalibrator.setFreezeIteration(config.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(config.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(config.getVarianceScale());
		matsimCalibrator.setBruteForce(config.useBruteForce());
		matsimCalibrator.setStatisticsFile(sc.getConfig().controler().getOutputDirectory() + "calibration-stats.txt");


		int arStartTime = config.getStartHour();
		int arEndTime = config.getEndHour();

		TransitSchedule schedule = sc.getTransitSchedule();

		//add counts data into calibrator
		for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
			TransitStopFacility stop = schedule.getFacilities().get(entry.getKey());
			for (Volume volume : entry.getValue().getVolumes().values()){
				if (volume.getHour() >= arStartTime && volume.getHour() <= arEndTime) {    //add volumes for each hour to calibrator
					int start_s = (volume.getHour() - 1) * 3600;
					int end_s = volume.getHour() * 3600 - 1;
					double val_passager_h = volume.getValue();
					matsimCalibrator.addMeasurement(stop, start_s, end_s, val_passager_h, SingleLinkMeasurement.TYPE.FLOW_VEH_H);
				}
			}
		}

		return matsimCalibrator;
	}
}
