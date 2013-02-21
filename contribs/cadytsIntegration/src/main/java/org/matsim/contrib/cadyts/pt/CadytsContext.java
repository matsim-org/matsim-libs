/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.cadyts.pt;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.config.PtCountsConfigGroup;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

/**
 * @author nagel
 *
 */
public class CadytsContext implements StartupListener, IterationEndsListener, BeforeMobsimListener, AfterMobsimListener {

	private final static Logger log = Logger.getLogger(CadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	private static final String OCCUPANCYANALYSIS_FILENAME = "cadytsPtOccupancyAnalysis.txt";

	private AnalyticalCalibrator<TransitStopFacility> calibrator = null;
	private SimResultsContainerImpl simResults;
	private final Counts occupCounts = new Counts();
	private final Counts boardCounts = new Counts();
	private final Counts alightCounts = new Counts();
	private CadytsPtOccupancyAnalyzer cadytsPtOccupAnalyzer;
	private PtPlanToPlanStepBasedOnEvents ptStep ;

	private CadytsPtConfigGroup cadytsConfig;

	private final Config config;

	public CadytsContext(Config config) {
		this.config = config;
		cadytsConfig = (CadytsPtConfigGroup) config.getModule(CadytsPtConfigGroup.GROUP_NAME);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		Scenario scenario = event.getControler().getScenario();
		EventsManager events = event.getControler().getEvents();



		this.cadytsPtOccupAnalyzer = new CadytsPtOccupancyAnalyzer(cadytsConfig.getCalibratedLines(), cadytsConfig.getTimeBinSize() );
		events.addHandler(this.cadytsPtOccupAnalyzer);

		this.simResults = new SimResultsContainerImpl(this.cadytsPtOccupAnalyzer, config.ptCounts().getCountsScaleFactor(), 
				cadytsConfig.getTimeBinSize());

		// this collects events and generates cadyts plans from it
		this.ptStep = new PtPlanToPlanStepBasedOnEvents(scenario, cadytsConfig.getCalibratedLines());
		events.addHandler(ptStep);

		String occupancyCountsFilename = config.ptCounts().getOccupancyCountsFileName();
		new MatsimCountsReader(this.occupCounts).readFile(occupancyCountsFilename);

		// build the calibrator. This is a static method, and in consequence has no side effects
		this.calibrator = CadytsBuilder.buildCalibrator(scenario, this.occupCounts );

	}


	@Override
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		this.cadytsPtOccupAnalyzer.reset(event.getIteration());
	}

	@Override
	public void notifyAfterMobsim(final AfterMobsimEvent event) {
		int it = event.getIteration();
		if (isActiveInThisIteration(it, event.getControler())) {
			// Get all stations of all analyzed lines and invoke the method write to get all information of them
			Set<Id> stopIds = new HashSet<Id>();
			for (Id lineId : this.cadytsConfig.getCalibratedLines()) {
				TransitLine line = event.getControler().getScenario().getTransitSchedule().getTransitLines().get(lineId);
				for (TransitRoute route : line.getRoutes().values()) {
					for (TransitRouteStop stop : route.getStops()) {
						stopIds.add(stop.getStopFacility().getId());
					}
				}
			}
			String outFile = event.getControler().getControlerIO().getIterationFilename(it, OCCUPANCYANALYSIS_FILENAME);
			this.cadytsPtOccupAnalyzer.writeResultsForSelectedStopIds(outFile, this.occupCounts, stopIds);
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (cadytsConfig.isWriteAnalysisFile()) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), event.getControler())) {
				analysisFilepath = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}

		this.calibrator.afterNetworkLoading(this.simResults);

		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsPtLinkCostOffsetsXMLFileIO(event.getControler().getScenario().getTransitSchedule()).write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}

		generateAndWriteCountsComparisons(event);
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	private static boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter % controler.getConfig().ptCounts().getPtCountsInterval() == 0);
	}

	private void generateAndWriteCountsComparisons(final IterationEndsEvent event) {
		Config config = event.getControler().getConfig();
		
		if ( this.cadytsConfig.getTimeBinSize()!=3600 ) {
			log.warn("generateAndWriteCountsComparisons() does not work when time bin size != 3600.  See comments in code. Skipping the comparison ..." ) ;
			return ;
			// yyyy there are some conceptual problems behind this which are not resolved:
			// () There should reasonably be two methods: one describing what cadyts _thinks_ it is comparing, and one that just
			// compares the output.  There is one methods writing simCountCompare..., and then this one here 
			// writing cadytsSimCountCompare... .  It is not clarified which one is doing which.
			// () The method that just compares the output should not rely on cadyts but compute its own observations. --
			// Unfortunately, this collides with the fact that the time bin size is part of the cadyts configuration.  This is, in the end, a 
			// consequence of the fact that the Counts format assumes hourly counts (other than cadyts, which reasonably allows the
			// specify the time span for every observation separately).
			// kai, feb'13
		}
		
		
		PtCountsConfigGroup ptCountsConfig = config.ptCounts();
		if (ptCountsConfig.getAlightCountsFileName() != null) { // yyyy this check should reasonably also be done in isActiveInThisIteration. kai,oct'10
			Controler controler = event.getControler();
			int iter = event.getIteration();
			if (isActiveInThisIteration(iter, controler)) {

//				if (config.ptCounts().getPtCountsInterval() != 10)
//					log.warn("yyyy This may not work when the pt counts interval is different from 10 because I think I changed things at two "
//							+ "places but I can't find the other one any more :-(.  (May just be inefficient.)  kai, oct'10");
				// looks like this just means "isActiveInThisIteration()" (maybe changed since above comment was made). kai, feb'13

				controler.stopwatch.beginOperation("compare with pt counts");

				Network network = controler.getNetwork();
				CadytsPtCountsComparisonAlgorithm ccaBoard = new CadytsPtCountsComparisonAlgorithm(this.cadytsPtOccupAnalyzer, this.boardCounts,
						network, config.ptCounts().getCountsScaleFactor());
				CadytsPtCountsComparisonAlgorithm ccaAlight = new CadytsPtCountsComparisonAlgorithm(this.cadytsPtOccupAnalyzer, this.alightCounts,
						network, config.ptCounts().getCountsScaleFactor());
				CadytsPtCountsComparisonAlgorithm ccaOccupancy = new CadytsPtCountsComparisonAlgorithm(this.cadytsPtOccupAnalyzer, this.occupCounts,
						network, config.ptCounts().getCountsScaleFactor());

				Double distanceFilter = ptCountsConfig.getDistanceFilter();
				String distanceFilterCenterNodeId  = ptCountsConfig.getDistanceFilterCenterNode();
				if ((distanceFilter != null) && (distanceFilterCenterNodeId != null)) {
					ccaBoard.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaAlight.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaOccupancy.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
				}

				ccaBoard.calculateComparison();
				ccaAlight.calculateComparison();
				ccaOccupancy.calculateComparison();

				String outputFormat = ptCountsConfig.getOutputFormat();
				if (outputFormat.contains("kml") || outputFormat.contains("all")) {
					OutputDirectoryHierarchy ctlIO = controler.getControlerIO();

					String filename = ctlIO.getIterationFilename(iter, "cadytsPtCountscompare.kmz");
					PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(ccaBoard.getComparison(),
							ccaAlight.getComparison(), ccaOccupancy.getComparison(), TransformationFactory.getCoordinateTransformation(config
									.global().getCoordinateSystem(), TransformationFactory.WGS84), this.boardCounts, this.alightCounts,
									this.occupCounts);

					kmlWriter.setIterationNumber(iter);
					kmlWriter.writeFile(filename);
				}
				
				if (outputFormat.contains("txt") || outputFormat.contains("all")) {
					//  As far as I can tell, this file is written twice, the other times without the "cadyts" part.  kai, feb'13
					//  As far as I can tell, the version here is wrong as soon as the time bin is different from 3600.--?? kai, feb'13
					//  See near beginning of method.  kai, feb'13 
					OutputDirectoryHierarchy ctlIO = controler.getControlerIO();
					ccaBoard.write(ctlIO.getIterationFilename(iter, "cadytsSimCountCompareBoarding.txt"));
					ccaAlight.write(ctlIO.getIterationFilename(iter, "cadytsSimCountCompareAlighting.txt"));
					ccaOccupancy.write(ctlIO.getIterationFilename(iter, "cadytsSimCountCompareOccupancy.txt"));
				}

				controler.stopwatch.endOperation("compare with pt counts");
			}
		}
	}

	/*package*/ static class SimResultsContainerImpl implements SimResults<TransitStopFacility> {
		private static final long serialVersionUID = 1L;
		private CadytsPtOccupancyAnalyzer occupancyAnalyzer = null;
		private final double countsScaleFactor;
		private final int timeBinSize_s;

		SimResultsContainerImpl(final CadytsPtOccupancyAnalyzer oa, final double countsScaleFactor, int timeBinSize_s) {
			this.occupancyAnalyzer = oa;
			this.countsScaleFactor = countsScaleFactor;
			this.timeBinSize_s = timeBinSize_s;
		}

		@Override
		public double getSimValue(final TransitStopFacility stop, final int startTime_s, final int endTime_s, final TYPE type) { // stopFacility or link

//			//				int hour = startTime_s / 3600;
//			int hour = this.occupancyAnalyzer.getTimeSlotIndex(startTime_s) ;
//
//			Id stopId = stop.getId();
//			int[] values = this.occupancyAnalyzer.getOccupancyVolumesForStop(stopId);
//
//			if (values == null) {
//				return 0;
//			}
//
//			return values[hour] * this.countsScaleFactor;
			
			double retval = 0. ;
			switch ( type ) {
			case COUNT_VEH:
				retval = this.occupancyAnalyzer.getOccupancyVolumeForStopAndTime(stop.getId(), startTime_s) * this.countsScaleFactor ;
				break;
			case FLOW_VEH_H:
				int multiple = this.timeBinSize_s / 3600 ; // e.g. "3" when timeBinSize_s = 3*3600 = 10800
				retval = this.occupancyAnalyzer.getOccupancyVolumeForStopAndTime(stop.getId(), startTime_s) * this.countsScaleFactor / multiple ;
				break;
			}
			return retval ;
			
		}

		@Override
		public String toString() {
			final StringBuffer stringBuffer2 = new StringBuffer();
			final String STOPID = "stopId: ";
			final String VALUES = "; values:";
			final char TAB = '\t';
			final char RETURN = '\n';

			for (Id stopId : this.occupancyAnalyzer.getOccupancyStopIds()) { // Only occupancy!
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append(STOPID);
				stringBuffer.append(stopId);
				stringBuffer.append(VALUES);

				boolean hasValues = false; // only prints stops with volumes > 0
				int[] values = this.occupancyAnalyzer.getOccupancyVolumesForStop(stopId);

				for (int ii = 0; ii < values.length; ii++) {
					hasValues = hasValues || (values[ii] > 0);

					stringBuffer.append(TAB);
					stringBuffer.append(values[ii]);
				}
				stringBuffer.append(RETURN);
				if (hasValues)
					stringBuffer2.append(stringBuffer.toString());

			}
			return stringBuffer2.toString();
		}

	}

	AnalyticalCalibrator<TransitStopFacility> getCalibrator() {
		return calibrator;
	}

	PtPlanToPlanStepBasedOnEvents getPtStep() {
		return ptStep;
	}

}
