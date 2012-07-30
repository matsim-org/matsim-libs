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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;

/**
 * @author nagel
 * @author mrieser
 */
/*package*/ final class CadytsBuilder {

	private final static Logger log = Logger.getLogger(CadytsBuilder.class);

	private CadytsBuilder() {
		// private Constructor, should not be instantiated
	}

	/*package*/ static MATSimUtilityModificationCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc) {
		Config config = sc.getConfig();

		// get default regressionInertia, used as parameter in the constructor
		String regressionInertiaValue = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "regressionInertia");
		double regressionInertia = MATSimUtilityModificationCalibrator.DEFAULT_REGRESSION_INERTIA;
		if (regressionInertiaValue != null) {
			regressionInertia = Double.parseDouble(regressionInertiaValue);
		}

		MATSimUtilityModificationCalibrator<TransitStopFacility> matsimCalibrator =
				new MATSimUtilityModificationCalibrator<TransitStopFacility>(MatsimRandom.getLocalInstance(), regressionInertia); // a logfile could optionally be passed

		// Set default standard deviation
		{
			String value = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "minFlowStddevVehH");
			if (value != null) {
				double stddev_veh_h = Double.parseDouble(value);
				matsimCalibrator.setMinStddev(stddev_veh_h, TYPE.FLOW_VEH_H);
				log.info("BSE:\tminFlowStddevVehH\t=\t" + stddev_veh_h);
			}
		}

		// SET MAX DRAWS
		/*
			{
				final String maxDrawStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "maxDraws");
				if (maxDrawStr != null) {
					final int maxDraws = Integer.parseInt(maxDrawStr);
					log.info("BSE:\tmaxDraws=" + maxDraws);
					calibrator.setMaxDraws(maxDraws);
				}
			}
		 */

		// SET FREEZE ITERATION
		{
			final String freezeIterationStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "freezeIteration");
			if (freezeIterationStr != null) {
				final int freezeIteration = Integer.parseInt(freezeIterationStr);
				log.info("BSE:\tfreezeIteration\t= " + freezeIteration);
				matsimCalibrator.setFreezeIteration(freezeIteration);
			}
		}

		// SET Preparatory Iterations
		{
			final String preparatoryIterationsStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "preparatoryIterations");
			if (preparatoryIterationsStr != null) {
				final int preparatoryIterations = Integer.parseInt(preparatoryIterationsStr);
				log.info("BSE:\tpreparatoryIterations\t= " + preparatoryIterations);
				matsimCalibrator.setPreparatoryIterations(preparatoryIterations);
			}
		}

		// SET varianceScale
		{
			final String varianceScaleStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "varianceScale");
			if (varianceScaleStr != null) {
				final double varianceScale = Double.parseDouble(varianceScaleStr);
				log.info("BSE:\tvarianceScale\t= " + varianceScale);
				matsimCalibrator.setVarianceScale(varianceScale);
			}
		}

		//SET useBruteForce
		{
			final String useBruteForceStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "useBruteForce");
			if (useBruteForceStr != null) {
				final boolean useBruteForce = Boolean.parseBoolean(useBruteForceStr);
				log.info("BSE:\tuseBruteForce\t= " + useBruteForce);
				matsimCalibrator.setBruteForce(useBruteForce);
			}
		}

		//set statistic file in output directory
		matsimCalibrator.setStatisticsFile(config.controler().getOutputDirectory() + "calibration-stats.txt");

		// SET countsScale
//		NewPtBsePlanStrategy.countsScaleFactor = Double.parseDouble(config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "countsScaleFactor"));
//		log.info("BSE:\tusing the countsScaleFactor of " + NewPtBsePlanStrategy.countsScaleFactor + " as packetSize from config.");
		//will be used in inner class SimResultsContainerImpl.getSimValue with "return values[hour] * countsScaleFactor;"

		// pt counts data were already read by ptContolerListener of controler. Can that information get achieved from here?
		// Should be in Scenario or ScenarioImpl.  If it is not there, it should be added there.  kai, oct'10

		//add a module in config not in file but "in execution"

		String countsFilename = config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "inputOccupancyCountsFile");
		if (countsFilename == null) {
			throw new RuntimeException("could not get counts filename from config; aborting");
		}

		Counts occupCounts = new Counts() ;
		new MatsimCountsReader(occupCounts).readFile(countsFilename);
		if (occupCounts.getCounts().size() == 0) {
			throw new RuntimeException("BSE requires counts-data.");
		}

		//add occupCounts as scenario element, so that it must not read again in ptBsePlanStrategy. Manuel apr12
		sc.addScenarioElement(occupCounts) ;
		
		int arStartTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "startTime"));
		int arEndTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "endTime"));

		NewPtBsePlanStrategy.trSched = sc.getTransitSchedule();

		//add counts data into calibrator
		for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
			TransitStopFacility stop= NewPtBsePlanStrategy.trSched.getFacilities().get(entry.getKey());
			for (Volume volume : entry.getValue().getVolumes().values()){
				if (volume.getHourOfDayStartingWithOne() >= arStartTime && volume.getHourOfDayStartingWithOne() <= arEndTime) {    //add volumes for each hour to calibrator
					int start_s = (volume.getHourOfDayStartingWithOne() - 1) * 3600;
					int end_s = volume.getHourOfDayStartingWithOne() * 3600 - 1;
					double val_passager_h = volume.getValue();
					matsimCalibrator.addMeasurement(stop, start_s, end_s, val_passager_h, SingleLinkMeasurement.TYPE.FLOW_VEH_H);
				}
			}
		}

		return matsimCalibrator;
	}

}
