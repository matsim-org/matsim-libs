/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGraph.java
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

import org.jfree.chart.JFreeChart;
import org.matsim.counts.CountSimComparison;


public abstract class CountsGraph {
	protected List<CountSimComparison> ccl_;
	protected JFreeChart chart_;
	protected int iteration_;
	/**
	 * The title of the chart, may contain whitespaces
	 */
	private String chartTitle_;
	/**
	 * the filename of the chart should not contain any whitespaces 
	 */
	private String filename;

	public CountsGraph() {
	}
	
	public CountsGraph(final List<CountSimComparison> ccl, final int iteration, final String filename, final String chartTitle) {	
		this.ccl_=ccl;
		this.iteration_=iteration;
		this.chartTitle_=chartTitle;
		this.filename = filename;
	}	
	
	public String getChartTitle() {
		return this.chartTitle_;
	}
	public JFreeChart getChart() {
		return this.chart_;
	}
	public void setChartTitle(final String chartTitle) {
		this.chartTitle_=chartTitle;
	}
		
	public abstract JFreeChart createChart(int nbr);

	
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return this.filename;
	}
	
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(final String filename) {
		this.filename = filename;
	}	
}