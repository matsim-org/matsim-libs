/* *********************************************************************** *
 * project: org.matsim.*
 * Normalizer.java
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
package playground.johannes.socialnetworks.statistics;

/**
 * @author illenberger
 *
 */
public class Normalizer {

	private final double min;
	
	private final double norm;
	
	public Normalizer(double min, double max) {
		this(min, max, 0.0, 1.0);
	}
	
	public Normalizer(double min, double max, double normMin, double normMax) {
		this.min = min;
		norm = (normMax - normMin)/(max - min);
	}
	
	public double normalize(double value) {
		return (value - min) * norm;
	}
}
