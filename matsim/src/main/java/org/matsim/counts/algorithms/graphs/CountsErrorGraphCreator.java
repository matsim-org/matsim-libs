/* *********************************************************************** *
 * project: org.matsim.*
 * CountsErrorGraphCreator.java
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

package org.matsim.counts.algorithms.graphs;

import java.util.List;
import java.util.Vector;

import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.helper.MyURL;

public class CountsErrorGraphCreator extends CountsGraphsCreator {


	public CountsErrorGraphCreator(final String sectionTitle) {
		super(sectionTitle);
	}


	@Override
	public List<CountsGraph> createGraphs(final List<CountSimComparison> ccl, final int iteration) {	

		List<CountsGraph> graphList=new Vector<CountsGraph>();
		{
			String fileName="errors";
			BoxPlotErrorGraph ep=new BoxPlotErrorGraph(ccl, iteration, fileName, fileName);
			ep.createChart(0);
			graphList.add(ep);
			this.section.addURL(new MyURL(fileName+".html", "errors"));
		}
		{
			String fileName="biasErrors";
			BiasErrorGraph ep=new BiasErrorGraph(ccl, iteration, fileName, fileName);
			ep.createChart(0);
			graphList.add(ep);
			this.section.addURL(new MyURL(fileName+".html", "biasErrors"));
		}
		return graphList;
	}
}
