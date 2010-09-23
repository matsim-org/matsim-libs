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

package playground.yu.counts.pt;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.core.config.Config;
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

import playground.yu.analysis.pt.OccupancyAnalyzer;

/**
 * @author dgrether
 */
public class PtCountControlerListener implements StartupListener,
		IterationEndsListener {

	private final Config config;
	private Counts boardCounts = null, alightCounts = null,
			occupancyCounts = null;
	final String MODULE_NAME = "ptCounts";
	private final OccupancyAnalyzer oa;

	public PtCountControlerListener(final Config config, OccupancyAnalyzer oa) {
		this.config = config;

		this.oa = oa;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		// MatsimCountsReader counts_parser = new
		// MatsimCountsReader(this.counts);
		// counts_parser.readFile(this.config.counts().getCountsFileName());
		String boardCountsFilename = this.config.findParam(MODULE_NAME,
				"inputBoardCountsFile");
		if (boardCountsFilename != null) {
			this.boardCounts = new Counts();
			new MatsimCountsReader(this.boardCounts)
					.readFile(boardCountsFilename);
		}

		String alightCountsFilename = this.config.findParam(MODULE_NAME,
				"inputAlightCountsFile");
		if (alightCountsFilename != null) {
			this.alightCounts = new Counts();
			new MatsimCountsReader(this.alightCounts)
					.readFile(alightCountsFilename);
		}

		String occupancyCountsFilename = this.config.findParam(MODULE_NAME,
				"inputOccupancyCountsFile");
		if (occupancyCountsFilename != null) {
			this.occupancyCounts = new Counts();
			new MatsimCountsReader(this.occupancyCounts)
					.readFile(occupancyCountsFilename);
		}
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Controler controler = event.getControler();
		int iter = event.getIteration();
		if ((iter % 10 == 0) && (iter > controler.getFirstIteration())) {
			IterationStopWatch isw = controler.stopwatch;
			isw.beginOperation("compare with counts");

			NetworkImpl net = controler.getNetwork();

			String distanceFilterStr = this.config.findParam(MODULE_NAME,
					"distanceFilter");
			Double distanceFilter = null;
			if (distanceFilterStr != null) {
				distanceFilter = Double.valueOf(distanceFilterStr);
			}

			String distanceFilterCenterNodeId = this.config.findParam(
					MODULE_NAME, "distanceFilterCenterNode");

			String countsScaleFacStr = this.config.findParam(MODULE_NAME,
					"countsScaleFactor");
			Double countsScaleFac = null;
			if (countsScaleFacStr != null) {
				countsScaleFac = Double.valueOf(countsScaleFacStr);
			}

			String outputFormat = this.config.findParam(MODULE_NAME,
					"outputformat");

			if ((distanceFilter != null)
					&& (distanceFilterCenterNodeId != null)
					&& outputFormat != null) {
				if (outputFormat.contains("kml")
						|| outputFormat.contains("all")) {

					PtCountsComparisonAlgorithm ccaBoard = null;
					if (this.boardCounts != null) {
						ccaBoard = new PtBoardCountComparisonAlgorithm(this.oa,
								this.boardCounts, net, countsScaleFac);
						ccaBoard.setDistanceFilter(distanceFilter,
								distanceFilterCenterNodeId);
						// ccaBoard.setCountsScaleFactor(countsScaleFac);
						ccaBoard.run();
					}

					PtCountsComparisonAlgorithm ccaAlight = null;
					if (this.alightCounts != null) {
						ccaAlight = new PtAlightCountComparisonAlgorithm(
								this.oa, this.alightCounts, net, countsScaleFac);
						ccaAlight.setDistanceFilter(distanceFilter,
								distanceFilterCenterNodeId);
						// ccaAlight.setCountsScaleFactor(countsScaleFac);
						ccaAlight.run();
					}

					PtCountsComparisonAlgorithm ccaOccupancy = null;
					if (this.occupancyCounts != null) {
						ccaOccupancy = new PtOccupancyCountComparisonAlgorithm(
								this.oa, this.occupancyCounts, net,
								countsScaleFac);
						ccaOccupancy.setDistanceFilter(distanceFilter,
								distanceFilterCenterNodeId);
						// ccaOccupancy.setCountsScaleFactor(countsScaleFac);
						ccaOccupancy.run();
					}

					ControlerIO ctlIO = controler.getControlerIO();
					String filename = ctlIO.getIterationFilename(iter,
							"countscompare.kmz");
					PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(
							ccaBoard.getComparison(),
							ccaAlight.getComparison(), ccaOccupancy
									.getComparison(), TransformationFactory
									.getCoordinateTransformation(this.config
											.global().getCoordinateSystem(),
											TransformationFactory.WGS84),
							this.boardCounts, this.alightCounts,
							occupancyCounts);
					kmlWriter.setIterationNumber(iter);
					kmlWriter.writeFile(filename);
					if (ccaBoard != null)
						ccaBoard.write(ctlIO.getIterationFilename(iter,
								"simCountCompareBoarding.txt"));
					if (ccaAlight != null)
						ccaAlight.write(ctlIO.getIterationFilename(iter,
								"simCountCompareAlighting.txt"));
					if (ccaOccupancy != null)
						ccaOccupancy.write(ctlIO.getIterationFilename(iter,
								"simCountCompareOccupancy.txt"));
				}
			}

			isw.endOperation("compare with counts");
		}
	}
}
