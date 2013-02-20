/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsBuilder.java
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

package playground.christoph.burgdorf.cadyts;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;

/**
 * @author cdobler
 */
/*package*/ final class CadytsBuilder {

	private CadytsBuilder() {
		// private Constructor, should not be instantiated
	}

	/*package*/ static AnalyticalCalibrator<Link> buildCalibrator(final Scenario sc, final Counts occupCounts ) {
		CadytsCarConfigGroup cadytsPtConfig = (CadytsCarConfigGroup) sc.getConfig().getModule(CadytsCarConfigGroup.GROUP_NAME);

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
		
		AnalyticalCalibrator<Link> matsimCalibrator = new AnalyticalCalibrator<Link>(
				sc.getConfig().controler().getOutputDirectory() + "/cadyts.log",
				MatsimRandom.getLocalInstance().nextLong(),timeBinSize_s);

		matsimCalibrator.setRegressionInertia(cadytsPtConfig.getRegressionInertia()) ;
		matsimCalibrator.setMinStddev(cadytsPtConfig.getMinFlowStddev_vehPerHour(), TYPE.FLOW_VEH_H);
		matsimCalibrator.setFreezeIteration(cadytsPtConfig.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(cadytsPtConfig.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(cadytsPtConfig.getVarianceScale());
		matsimCalibrator.setBruteForce(cadytsPtConfig.useBruteForce());
		matsimCalibrator.setStatisticsFile(sc.getConfig().controler().getOutputDirectory() + "/calibration-stats.txt");

		//int arStartTime_s = 3600*cadytsPtConfig.getStartTime()-3600 ;
		//int arEndTime_s = 3600*cadytsPtConfig.getEndTime()-1 ;
		// yyyy would be better to fix this; see email to balmermi and rieser 23/jul/2012 by kai & manuel
		int arStartTime_s = cadytsPtConfig.getStartTime(); // this version gets directly the startTime and endTime directly in seconds from the cadytsPtConfig 
		int arEndTime_s = cadytsPtConfig.getEndTime() ;
		
		int multiple = timeBinSize_s / 3600 ; // e.g. "3" when timeBinSize_s = 3*3600 = 10800

		//add counts data into calibrator
		for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
			Link link = sc.getNetwork().getLinks().get(entry.getKey());
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
						//					int start_s = (volume.getTimeBinIndexStartingWithOne() - 1) * timeBinSize_s ;
						//					int end_s = volume.getTimeBinIndexStartingWithOne() * timeBinSize_s - 1;
						double val = val_passager_h/multiple ;
						matsimCalibrator.addMeasurement(link, startTimeOfBin_s, endTimeOfBin_s, val, 
								SingleLinkMeasurement.TYPE.FLOW_VEH_H);
//								SingleLinkMeasurement.TYPE.COUNT_VEH);
						// changed this from FLOW_VEH_H to COUNT_VEH on 30/jul/2012 since this is no longer "hourly".  
						// kai/manuel, jul'12
					}
				}
				timeBinIndex++ ;
			}
		}

		return matsimCalibrator;
	}
}