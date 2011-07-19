/* *********************************************************************** *
 * project: mmoyo
 * CadytsBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioImpl;
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
 *
 */
class CadytsBuilder {
	private CadytsBuilder(){} // should not be instantiated

	static MATSimUtilityModificationCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc) {
			// made this method static so that there cannot be any side effects.  kai, oct'10
	
			Config config = sc.getConfig();
	
			// get default regressionInertia, it is used as parameter in the constructor
	
			// get default regressionInertia
			String regressionInertiaValue = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "regressionInertia");
			double regressionInertia = 0;
			if (regressionInertiaValue == null) {
				regressionInertia = MATSimUtilityModificationCalibrator.DEFAULT_REGRESSION_INERTIA;
			} else {
				regressionInertia = Double.parseDouble(regressionInertiaValue);
				// this works since it is used in the constructor
			}
			//calibrator.setRegressionInertia(regressionInertia);
			
			MATSimUtilityModificationCalibrator<TransitStopFacility> matsimCalibrator = new MATSimUtilityModificationCalibrator <TransitStopFacility>(MatsimRandom.getLocalInstance(), regressionInertia);
			//MATSimUtilityModificationCalibrator<TransitStopFacility> calibrator = new MATSimUtilityModificationCalibrator<TransitStopFacility>("calibration-log.txt", MatsimRandom.getLocalInstance().nextLong(), 3600);
	
			// Set default standard deviation
			{
				String value = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "minFlowStddevVehH");
				if (value != null) {
					double stddev_veh_h = Double.parseDouble(value);
					matsimCalibrator.setMinStddev(stddev_veh_h, TYPE.FLOW_VEH_H);
					System.out.println("BSE:\tminFlowStddevVehH\t=\t" + stddev_veh_h);
				}
			}
	
			//SET MAX DRAWS
			/*
			{
				final String maxDrawStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "maxDraws");
				if (maxDrawStr != null) {
					final int maxDraws = Integer.parseInt(maxDrawStr);
					System.out.println("BSE:\tmaxDraws=" + maxDraws);
					calibrator.setMaxDraws(maxDraws);
				}
			}
			*/
	
			// SET FREEZE ITERATION
			{
				final String freezeIterationStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "freezeIteration");
				if (freezeIterationStr != null) {
					final int freezeIteration = Integer.parseInt(freezeIterationStr);
					System.out.println("BSE:\tfreezeIteration\t= " + freezeIteration);
					matsimCalibrator.setFreezeIteration(freezeIteration);
				}
			}
	
			// SET Preparatory Iterations
			{
				final String preparatoryIterationsStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "preparatoryIterations");
				if (preparatoryIterationsStr != null) {
					final int preparatoryIterations = Integer.parseInt(preparatoryIterationsStr);
					System.out.println("BSE:\tpreparatoryIterations\t= " + preparatoryIterations);
					matsimCalibrator.setPreparatoryIterations(preparatoryIterations);
				}
			}
	
			// SET varianceScale
			{
				final String varianceScaleStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "varianceScale");
				if (varianceScaleStr != null) {
					final double varianceScale = Double.parseDouble(varianceScaleStr);
					System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
					matsimCalibrator.setVarianceScale(varianceScale);
				}
			}
	
			//SET useBruteForce
			{
				final String useBruteForceStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "useBruteForce");
				if (useBruteForceStr != null) {
					//This uses Boolean Instantiation!! -> final boolean useBruteForce = new Boolean(useBruteForceStr).booleanValue();
					final boolean useBruteForce = Boolean.parseBoolean(useBruteForceStr);
					System.out.println("BSE:\tuseBruteForce\t= " + useBruteForce);
					matsimCalibrator.setBruteForce(useBruteForce);
				}
			}
	
			//set statistic file in output directory
			matsimCalibrator.setStatisticsFile(config.controler().getOutputDirectory() + "calibration-stats.txt");
	
			// SET countsScale
			//double countsScaleFactor = config.counts().getCountsScaleFactor(); this is for private autos and we don't have this parameter in config file
			NewPtBsePlanStrategy.countsScaleFactor = Double.parseDouble(config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "countsScaleFactor"));
			System.out.println("BSE:\tusing the countsScaleFactor of " + NewPtBsePlanStrategy.countsScaleFactor + " as packetSize from config.");
			// yyyy how is this information moved into cadyts?
			//in inner class SimResultsContainerImpl.getSimValue with "return values[hour] * countsScaleFactor;"
	
			// pt counts data were already read by ptContolerListener of controler. Can that information get achieved from here?
			// Should be in Scenario or ScenarioImpl.  If it is not there, it should be added there.  kai, oct'10
	
			//add a module in config not in file but "in execution"
	
			String countsFilename = config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "inputOccupancyCountsFile");
			if ( countsFilename==null ) {
				throw new RuntimeException("could not get counts filename from config; aborting" ) ;
			}
	
			Counts occupCounts = new Counts() ;
			new MatsimCountsReader(occupCounts).readFile(countsFilename);
			if (occupCounts.getCounts().size()==0){
				throw new RuntimeException("BSE requires counts-data.");
			}
	
			// set up center and radius of counts stations locations
	//		distanceFilterCenterNodeCoord = network.getNodes().get(new IdImpl(config.findParam("counts", "distanceFilterCenterNode"))).getCoord();
	//		distanceFilter = Double.parseDouble(config.findParam("counts", "distanceFilter"));
			int arStartTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "startTime"));
			int arEndTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "endTime"));
	
			//the trSchedule has not been read by the controler. it is loaded here
			//DataLoader loader = new DataLoader();
			//trSched = loader.readTransitSchedule(sc.getConfig().findParam("network", "inputNetworkFile"), sc.getConfig().findParam("transit", "transitScheduleFile"));
			//Config config = sc.getConfig();
			NewPtBsePlanStrategy.trSched = ((ScenarioImpl)sc).getTransitSchedule();
						
			//add counts data into calibrator
			for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
				TransitStopFacility stop= NewPtBsePlanStrategy.trSched.getFacilities().get(entry.getKey());
				for (Volume volume : entry.getValue().getVolumes().values()){
					if (volume.getHour() >= arStartTime && volume.getHour() <= arEndTime) {    //add volumes for each hour to calibrator
						int start_s = (volume.getHour() - 1) * 3600;
						int end_s = volume.getHour() * 3600 - 1;
						double val_passager_h = volume.getValue();
						matsimCalibrator.addMeasurement(stop, start_s, end_s, val_passager_h, SingleLinkMeasurement.TYPE.FLOW_VEH_H);
					}
				}
			}
	
			return matsimCalibrator ;
		}
	
	

}
