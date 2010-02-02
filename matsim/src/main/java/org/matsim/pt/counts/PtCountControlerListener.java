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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

/**
 * @author yu
 */
public class PtCountControlerListener implements StartupListener, IterationEndsListener {

	private final static String MODULE_NAME = "ptCounts";

	private final Config config;
	private final Counts boardCounts, alightCounts;
	private final OccupancyAnalyzer oa;

	public PtCountControlerListener(final Config config, OccupancyAnalyzer oa) {
		this.config = config;
		this.boardCounts = new Counts();
		this.alightCounts = new Counts();
		this.oa = oa;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		new MatsimCountsReader(this.boardCounts).readFile(this.config.findParam(MODULE_NAME, "inputBoardCountsFile"));
		new MatsimCountsReader(this.alightCounts).readFile(this.config.findParam(MODULE_NAME, "inputAlightCountsFile"));
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Controler controler = event.getControler();
		int iter = event.getIteration();
		if ((iter % 10 == 0) && (iter > controler.getFirstIteration())) {
			controler.stopwatch.beginOperation("compare with counts");

			PtCountsComparisonAlgorithm ccaBoard = new PtBoardCountComparisonAlgorithm(this.oa, this.boardCounts, controler.getNetwork());
			PtCountsComparisonAlgorithm ccaAlight = new PtAlightCountComparisonAlgorithm(this.oa, this.alightCounts, controler.getNetwork());

			String distanceFilter = this.config.findParam(MODULE_NAME, "distanceFilter");
			String distanceFilterCenterNodeId = this.config.findParam(MODULE_NAME, "distanceFilterCenterNode");
			if ((distanceFilter	!= null) && (distanceFilterCenterNodeId != null)) {
				Double distance = Double.valueOf(distanceFilter);
				ccaBoard.setDistanceFilter(distance, distanceFilterCenterNodeId);
				ccaAlight.setDistanceFilter(distance, distanceFilterCenterNodeId);
			}

			ccaBoard.setCountsScaleFactor(Double.parseDouble(this.config.findParam(MODULE_NAME, "countsScaleFactor")));
			ccaAlight.setCountsScaleFactor(Double.parseDouble(this.config.findParam(MODULE_NAME, "countsScaleFactor")));

			ccaBoard.run();
			ccaAlight.run();
			String outputFormat = this.config.findParam(MODULE_NAME, "outputformat");
			if (outputFormat.contains("kml") || outputFormat.contains("all")) {
				String filename = event.getControler().getControlerIO().getIterationFilename(iter, "countscompare.kmz");
				PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(
						ccaBoard.getComparison(), ccaAlight.getComparison(),
						TransformationFactory.getCoordinateTransformation(
								this.config.global().getCoordinateSystem(),
								TransformationFactory.WGS84), this.boardCounts,
						this.alightCounts);
				kmlWriter.setIterationNumber(iter);
				kmlWriter.writeFile(filename);
			}
			controler.stopwatch.endOperation("compare with counts");
		}
	}
}
