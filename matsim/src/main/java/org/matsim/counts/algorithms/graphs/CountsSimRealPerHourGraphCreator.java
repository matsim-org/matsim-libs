/* *********************************************************************** *
 * project: org.matsim.*
 * CountsSimRealPerHourGraphCreator.java
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


public class CountsSimRealPerHourGraphCreator extends CountsGraphsCreator {

	
	public CountsSimRealPerHourGraphCreator(final String sectionTitle) {
		super(sectionTitle);
	}
	
	
	@Override
	public List<CountsGraph> createGraphs(final List<CountSimComparison> ccl, final int iteration) {	

		List<CountsGraph> graphList=new Vector<CountsGraph>();
		for (int i=1; i<25; i++) {
			
			String fileName="simVsRealVolumesHour"+(i)+"Iteration"+iteration;
			CountsSimRealPerHourGraph sg=new CountsSimRealPerHourGraph(ccl, iteration, fileName );
			sg.createChart(i);
			graphList.add(sg);
			this.section.addURL(new MyURL(fileName+".html", "hour: "+(i)));
		}
		
		return graphList;
	}
}
