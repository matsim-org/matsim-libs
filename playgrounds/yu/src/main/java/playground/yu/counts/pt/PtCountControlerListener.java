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

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
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
	private final Counts boardCounts, alightCounts;
	final String MODULE_NAME = "ptCounts";
	private final OccupancyAnalyzer oa;

	public PtCountControlerListener(final Config config, OccupancyAnalyzer oa) {
		this.config = config;
		this.boardCounts = new Counts();
		this.alightCounts = new Counts();
		this.oa = oa;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		// MatsimCountsReader counts_parser = new
		// MatsimCountsReader(this.counts);
		// counts_parser.readFile(this.config.counts().getCountsFileName());
		new MatsimCountsReader(this.boardCounts).readFile(this.config
				.findParam(MODULE_NAME, "inputBoardCountsFile"));
		new MatsimCountsReader(this.alightCounts).readFile(this.config
				.findParam(MODULE_NAME, "inputAlightCountsFile"));
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Controler controler = event.getControler();
		int iter = event.getIteration();
		if ((iter % 10 == 0) && (iter > controler.getFirstIteration())) {
			controler.stopwatch.beginOperation("compare with counts");

			PtCountsComparisonAlgorithm ccaBoard = new PtBoardCountComparisonAlgorithm(
					this.oa, this.boardCounts, controler.getNetwork());
			PtCountsComparisonAlgorithm ccaAlight = new PtAlightCountComparisonAlgorithm(
					this.oa, this.alightCounts, controler.getNetwork());

			Double distanceFilter = new Double(this.config.findParam(
					MODULE_NAME, "distanceFilter"));
			String distanceFilterCenterNodeId = this.config.findParam(
					MODULE_NAME, "distanceFilterCenterNode");
			if ((distanceFilter
			// .counts().getDistanceFilter()
					!= null)
					&& (distanceFilterCenterNodeId
					// counts().getDistanceFilterCenterNode()
					!= null)) {
				ccaBoard.setDistanceFilter(distanceFilter,
						distanceFilterCenterNodeId);
				ccaAlight.setDistanceFilter(distanceFilter,
						distanceFilterCenterNodeId);
			}

			ccaBoard.setCountsScaleFactor(Double.parseDouble(this.config
					.findParam(MODULE_NAME, "countsScaleFactor"))
			// counts().getCountsScaleFactor()
					);
			ccaAlight.setCountsScaleFactor(Double.parseDouble(this.config
					.findParam(MODULE_NAME, "countsScaleFactor")));

			ccaBoard.run();
			ccaAlight.run();
			// if (this.config.counts().getOutputFormat().contains("html")
			// || this.config.counts().getOutputFormat().contains("all")) {
			// // html and pdf output
			// boolean htmlset = true;
			// boolean pdfset = true;
			// CountsGraphWriter cgw = new CountsGraphWriter(Controler
			// .getIterationPath(event.getIteration()), cca
			// .getComparison(), event.getIteration(), htmlset, pdfset);
			// cgw.setGraphsCreator(new CountsSimRealPerHourGraphCreator(
			// "sim and real volumes"));
			// cgw.setGraphsCreator(new CountsErrorGraphCreator("errors"));
			// cgw.setGraphsCreator(new CountsLoadCurveGraphCreator(
			// "link volumes"));
			// cgw.setGraphsCreator(new CountsSimReal24GraphCreator(
			// "average working day sim and count volumes"));
			// cgw.createGraphs();
			// }
			String outputFormat = this.config.findParam(MODULE_NAME,
					"outputformat");
			if (outputFormat
			// counts().getOutputFormat()
					.contains("kml") || outputFormat.contains("all")) {
				String filename = event.getControler().getControlerIO()
						.getIterationFilename(iter, "countscompare.kmz");
				PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(
						ccaBoard.getComparison(), ccaAlight.getComparison(),
						TransformationFactory.getCoordinateTransformation(
								this.config.global().getCoordinateSystem(),
								TransformationFactory.WGS84), this.boardCounts,
						this.alightCounts);
				kmlWriter.setIterationNumber(iter);
				kmlWriter.writeFile(filename);
			}
			// if (this.config.counts().getOutputFormat().contains("txt")
			// || this.config.counts().getOutputFormat().contains("all")) {
			// String filename =
			// event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(),"countscompare.txt");
			// CountSimComparisonTableWriter ctw = new
			// CountSimComparisonTableWriter(
			// cca.getComparison(), Locale.ENGLISH);
			// ctw.writeFile(filename);
			// }
			controler.stopwatch.endOperation("compare with counts");
		}
	}
}
