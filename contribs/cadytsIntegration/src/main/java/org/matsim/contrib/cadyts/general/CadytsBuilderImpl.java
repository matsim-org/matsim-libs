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

package org.matsim.contrib.cadyts.general;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;

import java.util.Map;

/**
 * @author nagel
 * @author mrieser
 */
public final class CadytsBuilderImpl {
	private static final  Logger log = LogManager.getLogger( CadytsBuilderImpl.class );

	private CadytsBuilderImpl(){} // do not instantiate

	public static <T extends Identifiable<T>> AnalyticalCalibrator<T> buildCalibratorAndAddMeasurements(final Config config, final Counts<T> occupCounts,
																										LookUpItemFromId<T> lookUp, Class<T> idType) {
		if (occupCounts.getCounts().size() == 0) {
			log.warn("Counts container is empty.");
		}

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);

		AnalyticalCalibrator<T> matsimCalibrator = buildCalibrator(config);

		int multiple = cadytsConfig.getTimeBinSize() / 3600 ; // e.g. "3" when timeBinSize_s = 3*3600 = 10800

		// If I remember correctly, the following is trying to get around the fact that the counts time bins are fixed at hourly, but we want to
		// be able to be more flexible.  As a first step, time bins which are multiples from 1 hour are allowed, say "3".  In order to get a somewhat
		// meaningful connection to the counts format, it will add up all entries from the corresponding 3 hours before giving this to cadyts, i.e.
		// you may keep a higher resolution counts file but tell cadyts to work on longer time intervals. kai, dec'13

		// yyyy However, it seems that some of this did not work: We are using "hourly" counts, and dividing the multi-hour values by
		// the number of hours. ???????

		// yyyyyy I am currently of the opinion that the multi-hour version should be decoupled from the counts format.  There is a
		// cadyts file format which allows setting mult-hour measurements, and that seems a lot more direct than trying to use a file
		// format/data structure which is really not meant for this.  kai, dec'13

		//add counts data into calibrator
		int numberOfAddedMeasurements = 0 ;
		for (Map.Entry<Id<T>, Count<T>> entry : occupCounts.getCounts().entrySet()) {
			// (loop over all counting "items" (usually locations/stations)

			T item = lookUp.getItem(Id.create(entry.getKey(), idType)) ;
			if ( item==null ) {
				throw new RuntimeException("item is null; entry=" + entry + " idType=" + idType ) ;
			}
			int timeBinIndex = 0 ; // starting with zero which is different from the counts file!!!
			int startTimeOfBin_s = -1 ;
			double count = -1 ;
			for (Volume volume : entry.getValue().getVolumes().values()){
				// (loop over the different time slots)

				if ( timeBinIndex%multiple == 0 ) {
					// (i.e. first timeBinIndex belonging to given bin)

					startTimeOfBin_s = (volume.getHourOfDayStartingWithOne()-1)*3600 ;
					count = 0 ;
				}
				count += volume.getValue() ;
				if ( ! ( (timeBinIndex%multiple) == (multiple-1) ) ) {
					log.warn( " NOT adding measurement: timeBinIndex: " + timeBinIndex + "; multiple: " + multiple ) ;
				} else {
					// (i.e. last timeBinIndex belonging to given bin)

					int endTimeOfBin_s   = volume.getHourOfDayStartingWithOne()*3600 - 1 ;
					if ( !( cadytsConfig.getStartTime() <= startTimeOfBin_s && endTimeOfBin_s <= cadytsConfig.getEndTime()) ) {
						log.warn( " NOT adding measurement: cadytsConfigStartTime: " + cadytsConfig.getStartTime() + "; startTimeOfBin_s: " + startTimeOfBin_s +
								"; endTimeOfBin_s: " + endTimeOfBin_s + "; cadytsConfigEndTime: " + cadytsConfig.getEndTime() );
					} else { //add volumes for each bin to calibrator
						numberOfAddedMeasurements++ ;
//						matsimCalibrator.addMeasurement(item, startTimeOfBin_s, endTimeOfBin_s, count/multiple, SingleLinkMeasurement.TYPE.FLOW_VEH_H);
						matsimCalibrator.addMeasurement(item, startTimeOfBin_s, endTimeOfBin_s, count, SingleLinkMeasurement.TYPE.COUNT_VEH );

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

        if ( numberOfAddedMeasurements==0 ) {
			log.warn("No measurements were added.");
        }

        if ( matsimCalibrator.getProportionalAssignment() ) {
        	throw new RuntimeException("Gunnar says that this may not work so do not set to true. kai, sep'14") ;
        }
		return matsimCalibrator;
	}

	public static <T> AnalyticalCalibrator<T> buildCalibrator(final Config config) {
		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class ) ;

		//get timeBinSize_s and validate it
		if ((Time.MIDNIGHT % cadytsConfig.getTimeBinSize())!= 0 ){
			throw new RuntimeException("Cadyts requires a divisor of 86400 as time bin size value .");
		}
		if ( (cadytsConfig.getTimeBinSize() % 3600) != 0 ) {
			throw new RuntimeException("At this point, time bin sizes need to be multiples of 3600.  This is not a restriction " +
					"of Cadyts, but of the counts file format, which only allows for hourly inputs") ;
		}


		AnalyticalCalibrator<T> matsimCalibrator = new AnalyticalCalibrator<>(
				config.controller().getOutputDirectory() + "/cadyts.log",
				MatsimRandom.getLocalInstance().nextLong(),cadytsConfig.getTimeBinSize()
				 ) ;

		matsimCalibrator.setRegressionInertia(cadytsConfig.getRegressionInertia()) ;
		matsimCalibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), TYPE.FLOW_VEH_H);
		matsimCalibrator.setMinStddev(cadytsConfig.getMinFlowStddev_vehPerHour(), TYPE.COUNT_VEH);
		matsimCalibrator.setFreezeIteration(cadytsConfig.getFreezeIteration());
		matsimCalibrator.setPreparatoryIterations(cadytsConfig.getPreparatoryIterations());
		matsimCalibrator.setVarianceScale(cadytsConfig.getVarianceScale());

		matsimCalibrator.setBruteForce(cadytsConfig.useBruteForce());
		// I don't think this has an influence on any of the variants we are using. (Has an influence only when plan choice is left
		// completely to cadyts, rather than just taking the score offsets.) kai, dec'13
		// More formally, one would need to use the selectPlan() method of AnalyticalCalibrator which we are, however, not using. kai, mar'14
		if ( matsimCalibrator.getBruteForce() ) {
			log.warn("setting bruteForce==true for calibrator, but this won't do anything in the way the cadyts matsim integration is set up. kai, mar'14") ;
		}

		matsimCalibrator.setStatisticsFile(config.controller().getOutputDirectory() + "/calibration-stats.txt");
		return matsimCalibrator;
	}

}
