/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeLengthMedianTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

/**
 * @author illenberger
 *
 */
public class EdgeLengthMedianTask extends EdgeLengthSumTask {

	public EdgeLengthMedianTask() {
		setModule(new EdgeLengthMedian());
		setKey("d_median");
	}

	public EdgeLengthMedianTask(EdgeLengthMedian module) {
		setModule(module);
		setKey("d_median");
	}

}
