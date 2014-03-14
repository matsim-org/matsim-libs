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

/**
 * 
 */
package playground.johannes.gsv.visum;

/**
 * @author johannes
 *
 */
public class UnitTransformation {

	private final String unit;
	
	private final double factor;
	
	public UnitTransformation(String unit, double factor) {
		this.unit = unit;
		this.factor = factor;
	}
	
	public String getUnit() {
		return unit;
	}
	
	public double getFactor() {
		return factor;
	}
	
	public double transform(String str) {
		return Double.parseDouble(str.replace(unit, "")) * factor;
	}
}
