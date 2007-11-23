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

package org.matsim.counts;

import java.util.Locale;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerStartupEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.controler.listener.ControlerStartupListener;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import org.matsim.counts.algorithms.CountsGraphWriter;
import org.matsim.counts.algorithms.graphs.CountsErrorGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsLoadCurveGraphCreator;
import org.matsim.counts.algorithms.graphs.CountsSimRealPerHourGraphCreator;
import org.matsim.utils.geometry.transformations.TransformationFactory;

/**
 * @author dgrether
 */
public class CountControlerListener implements ControlerStartupListener,
		ControlerFinishIterationListener {

	private final Config config;

	public CountControlerListener(final Config config) {
		this.config = config;
	}

	/**
	 * @see org.matsim.controler.listener.ControlerStartupListener#notifyStartup(org.matsim.controler.events.ControlerStartupEvent)
	 */
	public void notifyStartup(final ControlerStartupEvent controlerStartupEvent) {
		MatsimCountsReader counts_parser = new MatsimCountsReader(Counts.getSingleton());
		counts_parser.readFile(this.config.counts().getCountsFileName());
	}

	/**
	 * @see org.matsim.controler.listener.ControlerFinishIterationListener#notifyIterationFinished(org.matsim.controler.events.ControlerFinishIterationEvent)
	 */
	public void notifyIterationFinished(final ControlerFinishIterationEvent event) {
		if ((event.getIteration() % 10 == 0) && (event.getIteration() > event.getControler().getMinimumIteration())) {
			CountsComparisonAlgorithm cca = new CountsComparisonAlgorithm(event.getControler().getCalcLinkStats(), Counts.getSingleton(), event.getControler().getNetwork());
			if ((this.config.counts().getDistanceFilter() != null) && (this.config.counts().getDistanceFilterCenterNode() != null)) {
				cca.setDistanceFilter(this.config.counts().getDistanceFilter(), this.config.counts().getDistanceFilterCenterNode());
			}
			cca.setCountsScaleFactor(this.config.counts().getCountsScaleFactor());
			cca.run(Counts.getSingleton());

			if (this.config.counts().getOutputFormat().contains("html") ||
					this.config.counts().getOutputFormat().contains("all")) {
				// html and pdf output
				boolean htmlset = true;
				boolean pdfset = true;
				String projectName="Project XY";
				CountsGraphWriter cgw = new CountsGraphWriter(Controler.getIterationPath(event.getIteration()), cca.getComparison(), event.getIteration(), htmlset, pdfset);
				cgw.setProjectName(projectName);
				cgw.setGraphsCreator(new CountsSimRealPerHourGraphCreator("sim and real volumes"));
				cgw.setGraphsCreator(new CountsErrorGraphCreator("errors"));
				cgw.setGraphsCreator(new CountsLoadCurveGraphCreator("link volumes"));
				Counts.getSingleton().addAlgorithm(cgw);
				Counts.getSingleton().runAlgorithms();
			}
			if (this.config.counts().getOutputFormat().contains("kml")||
					this.config.counts().getOutputFormat().contains("all")) {
				String filename = Controler.getIterationFilename("countscompare.kmz");
				CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
						cca.getComparison(), event.getControler().getNetwork(), TransformationFactory.getCoordinateTransformation(this.config.global().getCoordinateSystem(), TransformationFactory.WGS84 ));
				kmlWriter.setIterationNumber(event.getIteration());
				kmlWriter.write(filename);
			}
			if (this.config.counts().getOutputFormat().contains("txt")||
					this.config.counts().getOutputFormat().contains("all")) {
				String filename = Controler.getIterationFilename("countscompare.txt");
				CountSimComparisonTableWriter ctw=new CountSimComparisonTableWriter(cca.getComparison(),Locale.ENGLISH);
				ctw.write(filename);
			}
		}
	}
}
