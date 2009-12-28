/* *********************************************************************** *
 * project: org.matsim.*
 * SnowballAttributes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2;

/**
 * @author illenberger
 *
 */
public class SnowballAttributes {

	private int detected = -1;
	
	private int sampled = -1;
	
	public void detect(int iteration) {
		detected = iteration;
	}
	
	public int getIterationDeteted() {
		return detected;
	}
	
	public boolean isDetected() {
		return (detected > -1);
	}
	
	public void sample(int iteration) {
		sampled = iteration;
	}
	
	public int getIterationSampled() {
		return sampled;
	}
	
	public boolean isSampled() {
		return sampled > -1;
	}
}
