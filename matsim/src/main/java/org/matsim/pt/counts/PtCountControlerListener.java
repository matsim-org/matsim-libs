/* *********************************************************************** *
 * project: org.matsim.*
 * CountControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.pt.counts;

import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

public class PtCountControlerListener implements StartupListener, IterationEndsListener {

	private final static String MODULE_NAME = "ptCounts";

	private final Config config;
	private final Counts boardCounts, alightCounts,occupancyCounts;
	private final OccupancyAnalyzer oa;

	public PtCountControlerListener(final Config config, OccupancyAnalyzer oa) {
		this.config = config;
		this.boardCounts = new Counts();
		this.alightCounts = new Counts();
		this.occupancyCounts = new Counts();
		this.oa = oa;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		Module ptCounts = this.config.getModule(MODULE_NAME);
		if(ptCounts!=null){	
			String boardCountsFilename = this.config.findParam(MODULE_NAME, "inputBoardCountsFile");
			if (boardCountsFilename != null) {
				new MatsimCountsReader(this.boardCounts).readFile(boardCountsFilename);
			}
	
			String alightCountsFilename = this.config.findParam(MODULE_NAME, "inputAlightCountsFile");
			if (alightCountsFilename != null) {
				new MatsimCountsReader(this.alightCounts).readFile(alightCountsFilename);
			}
	
			String occupancyCountsFilename = this.config.findParam(MODULE_NAME, "inputOccupancyCountsFile");
			if (occupancyCountsFilename != null) {
				new MatsimCountsReader(this.occupancyCounts).readFile(occupancyCountsFilename);
			}
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Module ptCounts = this.config.getModule(MODULE_NAME);
		if (ptCounts != null) {
			Controler controler = event.getControler();
			int iter = event.getIteration();
			if ((iter % 10 == 0) && (iter > controler.getFirstIteration())) {
				controler.stopwatch.beginOperation("compare with counts");

				double countsScaleFactor = Double.parseDouble(this.config.getParam(MODULE_NAME, "countsScaleFactor"));
				NetworkImpl network = controler.getNetwork();
				PtCountsComparisonAlgorithm ccaBoard = new PtBoardCountComparisonAlgorithm(this.oa, this.boardCounts, network, countsScaleFactor);
				PtCountsComparisonAlgorithm ccaAlight = new PtAlightCountComparisonAlgorithm(this.oa, this.alightCounts, network, countsScaleFactor);
				PtCountsComparisonAlgorithm ccaOccupancy = new PtOccupancyCountComparisonAlgorithm(this.oa, this.occupancyCounts, network, countsScaleFactor);

				String distanceFilterStr = this.config.findParam(MODULE_NAME, "distanceFilter");
				String distanceFilterCenterNodeId = this.config.findParam(MODULE_NAME, "distanceFilterCenterNode");
				if ((distanceFilterStr != null)
						&& (distanceFilterCenterNodeId != null)) {
					Double distanceFilter = Double.valueOf(distanceFilterStr);
					ccaBoard.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaAlight.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaOccupancy.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
				}

				ccaBoard.setCountsScaleFactor(countsScaleFactor);
				ccaAlight.setCountsScaleFactor(countsScaleFactor);
				ccaOccupancy.setCountsScaleFactor(countsScaleFactor);

				ccaBoard.run();
				ccaAlight.run();
				ccaOccupancy.run();

				String outputFormat = this.config.findParam(MODULE_NAME,
						"outputformat");
				if (outputFormat.contains("kml")
						|| outputFormat.contains("all")) {
					ControlerIO ctlIO=controler.getControlerIO();
					
					String filename = ctlIO.getIterationFilename(iter, "countscompare.kmz");
					PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(ccaBoard.getComparison(), ccaAlight.getComparison(), ccaOccupancy.getComparison(),
							TransformationFactory.getCoordinateTransformation(this.config.global().getCoordinateSystem(),TransformationFactory.WGS84),
							this.boardCounts, this.alightCounts,occupancyCounts);
					
					kmlWriter.setIterationNumber(iter);
					kmlWriter.writeFile(filename);
					if (ccaBoard != null) {
						ccaBoard.write(ctlIO.getIterationFilename(iter, "simCountCompareBoarding.txt"));
					}
					if (ccaAlight != null) {
						ccaAlight.write(ctlIO.getIterationFilename(iter, "simCountCompareAlighting.txt"));
					}
					if (ccaOccupancy != null) {
						ccaOccupancy.write(ctlIO.getIterationFilename(iter,	"simCountCompareOccupancy.txt"));
					}
				}

				controler.stopwatch.endOperation("compare with counts");
			}
		}
	}
}
