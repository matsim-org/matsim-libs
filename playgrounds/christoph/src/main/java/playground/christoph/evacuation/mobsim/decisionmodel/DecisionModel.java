/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionModel.java
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

package playground.christoph.evacuation.mobsim.decisionmodel;

/**
 * 
 * @author cdobler
 */
public interface DecisionModel {

	public static final String newLine = "\n";
	public static final String delimiter = "\t";
	
	public void printStatistics();
	
	public void writeDecisionsToFile(String file);
	
	public void readDecisionsFromFile(String file);
}
