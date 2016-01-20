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

package playground.johannes.gsv.sim.cadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.LookUpItemFromId;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import playground.johannes.gsv.sim.GsvConfigGroup;

import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.sqrt;

/**
 * @author nagel
 * @author mrieser
 */
public final class CadytsBuilder {
	private static Logger log = Logger.getLogger( CadytsBuilder.class ) ;

	private CadytsBuilder() {
		// private Constructor, should not be instantiated
	}

	public static <T> AnalyticalCalibrator<T> buildCalibrator(final Config config, final Counts<T> occupCounts, LookUpItemFromId<T> lookUp, Class<T> idType ) {
		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);

		//get timeBinSize_s and validate it
		int timeBinSize_s = cadytsConfig.getTimeBinSize();
		if(timeBinSize_s != 86400) {
			throw new RuntimeException("Time bin size has to be 86400!");
		}
		
		if (occupCounts.getCounts().size() == 0) {
			log.warn("Counts container is empty.");
		}
		
		AnalyticalCalibrator<T> matsimCalibrator = new AnalyticalCalibrator<T>(
				config.controler().getOutputDirectory() + "/cadyts.log",
				MatsimRandom.getLocalInstance().nextLong(),timeBinSize_s
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
		
		matsimCalibrator.setStatisticsFile(config.controler().getOutputDirectory() + "/calibration-stats.txt");
		
		int linkCnt = 0 ;
		int odCount = 0;
		
		double odWeightFactor = Double.parseDouble(config.getParam(GsvConfigGroup.GSV_CONFIG_MODULE_NAME, "odWeightFactor"));
		for (Map.Entry<Id<T>, Count<T>> entry : occupCounts.getCounts().entrySet()) {
			// (loop over all counting "items" (usually locations/stations)
			
			T item = lookUp.getItem(Id.create(entry.getKey(), idType)) ;
			
			Count count = entry.getValue();
			
			double value = 0;
			for (int i = 1; i < 25; i++) {
				value += count.getVolume(i).getValue();
			}
			
			if(entry.getKey().toString().startsWith(ODCalibrator.VIRTUAL_ID_PREFIX)) {
				final double stddev = max(matsimCalibrator.getMinStddev(SingleLinkMeasurement.TYPE.COUNT_VEH), sqrt(matsimCalibrator.getVarianceScale()	* value));
				matsimCalibrator.addMeasurement(item, 0, 86400, value, stddev/odWeightFactor, SingleLinkMeasurement.TYPE.COUNT_VEH);
				odCount++;
			} else {	
				matsimCalibrator.addMeasurement(item, 0, 86400, value, SingleLinkMeasurement.TYPE.COUNT_VEH);
				linkCnt++;
			}
			
		}

        log.info(String.format("Added %s link measurements to calibrator.", linkCnt));
        log.info(String.format("Added %s OD measurements to calibrator.", odCount));
		
        if ( matsimCalibrator.getProportionalAssignment() ) {
        	throw new RuntimeException("Gunnar says that this may not work so do not set to true. kai, sep'14") ;
        }
		return matsimCalibrator;
	}
}