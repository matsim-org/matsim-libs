/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.toi;

public class LaneType {

	private int forwardLanes;
	private int backwardLanes;

	public LaneType(String lanetype) {
		this.forwardLanes = 0;
		this.backwardLanes = 0;
		
		for(int i = 0; i<10; i++){
			if(lanetype.contains(Integer.toString((i*2+1)))){
				this.forwardLanes = i+1;
			}
			if(lanetype.contains(Integer.toString(i*2))){
				this.backwardLanes = i;
			}
		}
	}

	public Double getNumberOfForwardLanes() {
		return 1.0 *forwardLanes;
	}

	public Double getNumberOfBackLanes() {
		return 1.0*backwardLanes;
	}

}
