/* *********************************************************************** *
 * project: org.matsim.*
 * LinkWidthCalculator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.vis.snapshotwriters;


/**
 * @author dgrether
 *
 */
public class SnapshotLinkWidthCalculator {
	/**
	 * Distance (in m) between the two innermost opposing lanes. Setting this to a larger number (e.g. 30) lets you see the
	 * direction in which cars are going. (Setting this to a negative number probably gives you "driving on the left", but lanes
	 * will be wrong (could be fixed), and, more importantly, the simulation still assumes "driving on the right" when locations
	 * (with coordinates) are connected to links.) TODO should be configurable
	 */
	private double widthOfMedian = 30. ; // default

	private double laneWidth = 3.75; //default in NetworkImpl and network_v1.dtd
	
	public void setLaneWidth( double dd ) {
		if ( !Double.isNaN(dd) ) {
			// ( seems that this can be NaN when coming from reading a regular network. kai, jul'16 )
			laneWidth = dd ;
		}
	}
	
	public double getLaneWidth(){
		return laneWidth;
	}
	
	public void setLinkWidthForVis(double linkWidthCorrectionFactor){
		this.widthOfMedian = linkWidthCorrectionFactor;
	}
	
	public double calculateLinkWidth(double nrOfLanes){
//		System.err.println(widthOfMedian + " "  + laneWidth + " " + nrOfLanes);
		return widthOfMedian + laneWidth * nrOfLanes; 
	}
	
	double calculateLanePosition(double lane){
		return 0.5 * widthOfMedian + laneWidth * lane;
	}
	
}
