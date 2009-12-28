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

import java.util.Locale;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsGraphWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimReal24GraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;

import playground.yu.analysis.pt.OccupancyAnalyzer;

/**
 * @author dgrether
 */
public class PtBoardCountControlerListener implements StartupListener,
		IterationEndsListener {

	private final Config config;
	private final Counts counts;

	private final OccupancyAnalyzer oa;

	public PtBoardCountControlerListener(final Config config,
			OccupancyAnalyzer oa) {
		this.config = config;
		this.counts = new Counts();
		this.oa = oa;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		MatsimCountsReader counts_parser = new MatsimCountsReader(this.counts);
		counts_parser.readFile(this.config.counts().getCountsFileName());
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {
		Controler controler = event.getControler();
		if ((event.getIteration() % 10 == 0)
				&& (event.getIteration() > controler.getFirstIteration())) {
			controler.stopwatch.beginOperation("compare with counts");
			PtBoardCountsComparisonAlgorithm cca = new PtBoardCountsComparisonAlgorithm(
					this.oa, this.counts, controler.getNetwork());
			if ((this.config.counts().getDistanceFilter() != null)
					&& (this.config.counts().getDistanceFilterCenterNode() != null)) {
				cca.setDistanceFilter(this.config.counts().getDistanceFilter(),
						this.config.counts().getDistanceFilterCenterNode());
			}
			cca.setCountsScaleFactor(this.config.counts()
					.getCountsScaleFactor());
			cca.run();

			if (this.config.counts().getOutputFormat().contains("html")
					|| this.config.counts().getOutputFormat().contains("all")) {
				// html and pdf output
				boolean htmlset = true;
				boolean pdfset = true;
				CountsGraphWriter cgw = new CountsGraphWriter(Controler
						.getIterationPath(event.getIteration()), cca
						.getComparison(), event.getIteration(), htmlset, pdfset);
				cgw.setGraphsCreator(new CountsSimRealPerHourGraphCreator(
						"sim and real volumes"));
				cgw.setGraphsCreator(new CountsErrorGraphCreator("errors"));
				cgw.setGraphsCreator(new CountsLoadCurveGraphCreator(
						"link volumes"));
				cgw.setGraphsCreator(new CountsSimReal24GraphCreator(
						"average working day sim and count volumes"));
				cgw.createGraphs();
			}
			if (this.config.counts().getOutputFormat().contains("kml")
					|| this.config.counts().getOutputFormat().contains("all")) {
				String filename = 	event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(),"countscompare.kmz");
				PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(
						cca.getComparison(), TransformationFactory
								.getCoordinateTransformation(this.config
										.global().getCoordinateSystem(),
										TransformationFactory.WGS84),
						this.counts);
				kmlWriter.setIterationNumber(event.getIteration());
				kmlWriter.writeFile(filename);
			}
			if (this.config.counts().getOutputFormat().contains("txt")
					|| this.config.counts().getOutputFormat().contains("all")) {
				String filename = 	event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(),"countscompare.txt");
				CountSimComparisonTableWriter ctw = new CountSimComparisonTableWriter(
						cca.getComparison(), Locale.ENGLISH);
				ctw.writeFile(filename);
			}
			controler.stopwatch.endOperation("compare with counts");
		}
	}

	public Counts getCounts() {
		return this.counts;
	}
}
