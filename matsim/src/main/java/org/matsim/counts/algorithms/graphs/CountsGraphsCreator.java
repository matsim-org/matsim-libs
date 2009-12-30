/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGraphsCreator.java
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

import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.graphs.helper.Section;


public abstract class CountsGraphsCreator {
	protected Section section;
	
	public CountsGraphsCreator(final String sectionTitle) {	
		this.section=new Section(sectionTitle);
	}
	
	public Section getSection() {
		return this.section;
	}
		
	public abstract List<CountsGraph> createGraphs(List<CountSimComparison> ccl, int iteration);	
}