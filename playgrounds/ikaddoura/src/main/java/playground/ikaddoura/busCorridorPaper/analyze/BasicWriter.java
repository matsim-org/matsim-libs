/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.busCorridorPaper.analyze;

import java.util.List;
import java.util.Map;

/**
 * @author ikaddoura
 *
 */
public abstract class BasicWriter {
	
	private final String outputFolder;
	private final Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana;
	
	public BasicWriter(String outputFolder, Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana) {
		this.outputFolder = outputFolder;
		this.runNr2itNr2ana = runNr2itNr2ana;
	}

	public Double getMin(List<Double> memValues) {
		double min = Double.POSITIVE_INFINITY;
		for (Double value : memValues){
			if (value < min) {
				min = value;
			}
		}
		return min;
	}
	
	public Double getMax(List<Double> memValues) {
		double max = Double.NEGATIVE_INFINITY;
		for (Double value : memValues){
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	public Double getAverage(List<Double> memValues) {
		int n = 0;
		double sum = 0;
		for (Double value : memValues){
			n++;
			sum = sum + value;
		}
		double avg = sum/n;
		return avg;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public Map<Integer, Map<Integer, ExtItAnaInfo>> getRunNr2itNr2ana() {
		return runNr2itNr2ana;
	}
	
}
