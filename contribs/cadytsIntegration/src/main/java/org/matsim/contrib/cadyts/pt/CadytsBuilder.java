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

import cadyts.calibrators.analytical.AnalyticalCalibrator;
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

//	/*package*/ static AnalyticalCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc, final Counts occupCounts) {
//		return buildCalibrator(sc,occupCounts,3600) ;
//	}

	/*package*/ static AnalyticalCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc, final Counts occupCounts, 
			int timeBinSize_s ) {
		CadytsPtConfigGroup cadytsPtConfig = (CadytsPtConfigGroup) sc.getConfig().getModule(CadytsPtConfigGroup.GROUP_NAME);

		if (occupCounts.getCounts().size() == 0) {
			throw new RuntimeException("CadytsPt requires counts-data.");
		}

		AnalyticalCalibrator<TransitStopFacility> matsimCalibrator = new AnalyticalCalibrator<TransitStopFacility>(
				sc.getConfig().controler().getOutputDirectory() + "/cadyts.log",
				MatsimRandom.getLocalInstance().nextLong(),
				timeBinSize_s ) ;
		matsimCalibrator.setRegressionInertia(cadytsPtConfig.getRegressionInertia()) ;

		matsimCalibrator.setMinStddev(cadytsPtConfig.getMinFlowStddev_vehPerHour(), TYPE.FLOW_VEH_H);
		matsimCalibrator.setFreezeIteration(cadytsPtConfig.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(cadytsPtConfig.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(cadytsPtConfig.getVarianceScale());
		matsimCalibrator.setBruteForce(cadytsPtConfig.useBruteForce());
		matsimCalibrator.setStatisticsFile(sc.getConfig().controler().getOutputDirectory() + "/calibration-stats.txt");


		int arStartTime_s = 3600*cadytsPtConfig.getStartHour()-3600 ;
		int arEndTime_s = 3600*cadytsPtConfig.getEndHour()-1 ;
		// yyyy would be better to fix this; see email to balmermi and rieser 23/jul/2012 by kai & manuel

		TransitSchedule schedule = sc.getTransitSchedule();

		//add counts data into calibrator
		for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
			TransitStopFacility stop = schedule.getFacilities().get(entry.getKey());
			for (Volume volume : entry.getValue().getVolumes().values()){
				int startTimeOfBin_s = (volume.getTimeBinIndexStartingWithOne()-1)*timeBinSize_s ;
				int endTimeOfBin_s   = volume.getTimeBinIndexStartingWithOne()*timeBinSize_s - 1 ;
				if (startTimeOfBin_s >= arStartTime_s && endTimeOfBin_s <= arEndTime_s) {    //add volumes for each bin to calibrator
//					int start_s = (volume.getTimeBinIndexStartingWithOne() - 1) * timeBinSize_s ;
//					int end_s = volume.getTimeBinIndexStartingWithOne() * timeBinSize_s - 1;
					double val_passager_h = volume.getValue();
					matsimCalibrator.addMeasurement(stop, startTimeOfBin_s, endTimeOfBin_s, val_passager_h, 
							SingleLinkMeasurement.TYPE.FLOW_VEH_H);
				}
			}
		}

		return matsimCalibrator;
	}
}
