/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGraphWriter.java
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

package org.matsim.counts.algorithms;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import org.matsim.counts.algorithms.graphs.CountsGraphsCreator;
import org.matsim.counts.algorithms.graphs.helper.OutputDelegate;

import java.io.File;
import java.util.List;
import java.util.Vector;

public class CountsHtmlAndGraphsWriter {

	private String iterationPath;
	private List<CountSimComparison> countSimComparisons;
	private int iteration;
	private OutputDelegate outputDelegate;
	private List<CountsGraphsCreator> graphsCreators;

	private static final Logger log = LogManager.getLogger(CountsHtmlAndGraphsWriter.class);

	public CountsHtmlAndGraphsWriter(final String iterationPath, final List<CountSimComparison> countSimComparisons, final int iteration) {
		this.iterationPath = iterationPath + "/graphs/";
		this.countSimComparisons = countSimComparisons;
		this.iteration = iteration;
		this.outputDelegate = new OutputDelegate(this.iterationPath);
		new File(this.iterationPath).mkdir();
		this.graphsCreators=new Vector<CountsGraphsCreator>();
	}

	public OutputDelegate getOutput() {
		return this.outputDelegate;
	}

	public void addGraphsCreator(final CountsGraphsCreator graphsCreator) {
		this.graphsCreators.add(graphsCreator);
	}

	public void createHtmlAndGraphs() {
		log.info("Creating graphs");
        for (CountsGraphsCreator cgc : this.graphsCreators) {
            List<CountsGraph> graphs = cgc.createGraphs(this.countSimComparisons, this.iteration);
            this.outputDelegate.addSection(cgc.getSection());
            for (CountsGraph cg : graphs) {
                this.outputDelegate.addCountsGraph(cg);
            }
        }
		this.outputDelegate.outputHtml();
	}
}
