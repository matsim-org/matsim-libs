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

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;
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

	/*package*/ static AnalyticalCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc, final Counts occupCounts ) {
		CadytsPtConfigGroup cadytsPtConfig = (CadytsPtConfigGroup) sc.getConfig().getModule(CadytsPtConfigGroup.GROUP_NAME);

		//get timeBinSize_s and validate it
		int timeBinSize_s = cadytsPtConfig.getTimeBinSize();
		if ((Time.MIDNIGHT % timeBinSize_s)!= 0 ){
			throw new RuntimeException("Cadyts requieres a divisor of 86400 as time bin size value .");
		}
		if ( (timeBinSize_s % 3600) != 0 ) {
			throw new RuntimeException("At this point, time bin sizes need to be multiples of 3600.  This is not a restriction " +
					"of Cadyts, but of the counts file format, which only allows for hourly inputs") ;
		}
		
		if (occupCounts.getCounts().size() == 0) {
			throw new RuntimeException("CadytsPt requires counts-data.");
		}
		
		AnalyticalCalibrator<TransitStopFacility> matsimCalibrator = new AnalyticalCalibrator<TransitStopFacility>(
				sc.getConfig().controler().getOutputDirectory() + "/cadyts.log",
				MatsimRandom.getLocalInstance().nextLong(),timeBinSize_s
				 ) ;

		matsimCalibrator.setRegressionInertia(cadytsPtConfig.getRegressionInertia()) ;
		matsimCalibrator.setMinStddev(cadytsPtConfig.getMinFlowStddev_vehPerHour(), TYPE.FLOW_VEH_H);
		matsimCalibrator.setFreezeIteration(cadytsPtConfig.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(cadytsPtConfig.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(cadytsPtConfig.getVarianceScale());
		matsimCalibrator.setBruteForce(cadytsPtConfig.useBruteForce());
		matsimCalibrator.setStatisticsFile(sc.getConfig().controler().getOutputDirectory() + "/calibration-stats.txt");

		int arStartTime_s = cadytsPtConfig.getStartTime(); 
		int arEndTime_s = cadytsPtConfig.getEndTime() ;
		// (this version gets directly the startTime and endTime directly in seconds from the cadytsPtConfig) 
		
		TransitSchedule schedule = sc.getTransitSchedule();
		
		int multiple = timeBinSize_s / 3600 ; // e.g. "3" when timeBinSize_s = 3*3600 = 10800

		//add counts data into calibrator
		for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
			TransitStopFacility stop = schedule.getFacilities().get(entry.getKey());
			int timeBinIndex = 0 ; // starting with zero which is different from the counts file!!!
			int startTimeOfBin_s = -1 ;
			double val_passager_h = -1 ;
			for (Volume volume : entry.getValue().getVolumes().values()){
				if ( timeBinIndex%multiple == 0 ) {
					startTimeOfBin_s = (volume.getHourOfDayStartingWithOne()-1)*3600 ;
					val_passager_h = 0 ;
				}
				val_passager_h += volume.getValue() ;
				if ( (timeBinIndex%multiple) == (multiple-1) ) {
					int endTimeOfBin_s   = volume.getHourOfDayStartingWithOne()*3600 - 1 ;
					if (startTimeOfBin_s >= arStartTime_s && endTimeOfBin_s <= arEndTime_s) {    //add volumes for each bin to calibrator
						double val = val_passager_h/multiple ;
						matsimCalibrator.addMeasurement(stop, startTimeOfBin_s, endTimeOfBin_s, val, 
								SingleLinkMeasurement.TYPE.FLOW_VEH_H);
//								SingleLinkMeasurement.TYPE.COUNT_VEH);
						// changed this from FLOW_VEH_H to COUNT_VEH on 30/jul/2012 since this is no longer "hourly".  
						// kai/manuel, jul'12
						// Despite the above comment, I am finding this with FLOW_VEH_H.  Why?  kai, feb'13
						// yyyyyy For the test case, this seems to produce weird results.  The expected counts are 1 and 5, the expected result is 0 and 4.
						// Possibly, it divides 1 and 5 by 2 and then has 0.5 and 2.5 for the given hour, in which case 1 and 3 would be best?????
						// kai, feb'13
					}
				}
				timeBinIndex++ ;
			}
		}

		return matsimCalibrator;
	}
}