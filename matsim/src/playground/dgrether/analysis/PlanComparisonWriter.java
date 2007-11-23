/* *********************************************************************** *
 * project: org.matsim.*
 * PlanComparisonWriter.java
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

package playground.dgrether.analysis;



/**
 * Simple Interface which is implemented by all classes
 * being able to write a PlanComparison to a specific
 * dataformat.
 * @author dgrether
 * 
 */
public interface PlanComparisonWriter {
	/**
	 * Writes the PlanComparison Object to the appropriate format.
	 * @param pc the PlanComparison file to write
	 */
	public void write(PlanComparison pc);
}
