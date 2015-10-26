/* *********************************************************************** *
 * project: org.matsim.*
 * VertexDegreeColorizer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.Degree;

import java.awt.*;

/**
 * A VertexDegreeColorizer colorizes vertices according to their degree. The
 * colors are out of a continous color spectrum white-green-red-blue scaled to
 * the degree distribution.
 * 
 * @author jillenberger
 * 
 */
public class VertexDegreeColorizer implements Colorizable {

	private final int k_min;

	private final int k_max;

	private boolean logscale;

	/**
	 * Creates a colorizer which color spectrum is scaled to the degree
	 * distribution of <tt>graph</tt>.
	 * 
	 * @param graph
	 *            a graph
	 */
	public VertexDegreeColorizer(Graph graph) {
		Degree degree = Degree.getInstance();
		DescriptiveStatistics distr = degree.statistics(graph.getVertices());
		k_min = (int) distr.getMin();
		k_max = (int) distr.getMax();
	}

	/**
	 * Returns whether the color spectrum logarithmic scale.
	 * 
	 * @return <tt>true</tt> if the color spectrum is scaled logarithmically,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean isLogscale() {
		return logscale;
	}

	/**
	 * Sets whether the color spectrum is scaled logarithmically.
	 * 
	 * @param logscale
	 *            <tt>true</tt> to scale the color spectrum logarithmically,
	 *            <tt>false</tt> otherwise.
	 */
	public void setLogscale(boolean logscale) {
		this.logscale = logscale;
	}

	/**
	 * Returns a color from white-green-red-blue according to the degree of <tt>vertex</tt>.
	 * 
	 * @param vertex a vertex
	 */
	@Override
	public Color getColor(Object vertex) {
		int val = ((Vertex) vertex).getEdges().size();
		double color = 0;
		if (logscale) {
			double min2 = Math.log(k_min + 1);
			double max2 = Math.log(k_max + 1);
			color = (Math.log(val + 1) - min2) / (max2 - min2);
		} else {
			color = (val - k_min) / (double) (k_max - k_min);
		}

		return ColorUtils.getGRBColor(color);
	}

}