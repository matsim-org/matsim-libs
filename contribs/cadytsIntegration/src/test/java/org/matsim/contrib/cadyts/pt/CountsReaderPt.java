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

package org.matsim.contrib.cadyts.pt;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * parses a output text file containing counts comparisons
 * 
 * this class is only there in order to read the column-oriented output back in for testing. 
 * It should not be used elsewhere without further thinking. kai, sep'14
 * */
final class CountsReaderPt {

	private final static Logger log = Logger.getLogger(CountsReaderPt.class);

	final String STOP_ID_STRING_0 = "StopId :";
	final String HEAD_STRING_0 = "hour";
	final String ZERO = "0.0";

	String countsTextFile;
	Map<Id<TransitStopFacility>, Map<String, double[]>> count = new TreeMap<>();

	CountsReaderPt(final String countsTextFile){
		this.countsTextFile = countsTextFile;
		readValues();
	}

	private void readValues() {
		try {
			FileReader fileReader = new FileReader(this.countsTextFile);
			// ->:correct this : reads first row
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String row = bufferedReader.readLine(); // TODO : include the first row inside the iteration
			String[] values = StringUtils.explode(row, '\t');

			Id<TransitStopFacility> id = Id.create(values[1], TransitStopFacility.class);
			while (row != null) {
				row = bufferedReader.readLine();
				if (row != null && row != "") {
					values = StringUtils.explode(row, '\t');
					if (values[0].equals(this.STOP_ID_STRING_0)) {
						id = Id.create(values[1], TransitStopFacility.class);
					} else if (values[0].equals(this.HEAD_STRING_0)) {
						// it does nothing, correct this condition
					} else {
						if (!this.count.containsKey(id)) {
							this.count.put(id, new TreeMap<String, double[]>());
						}
						this.count.get(id).put(values[0],
								new double[] { Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3]) });
					}
				}
			}
			bufferedReader.close();
			fileReader.close();
		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * I am reasonably sure that the first entry (hour 1) is at array-position 0. kai, sep'14
	 */
	double[]getSimulatedValues(final Id<TransitStopFacility> stopId) {
		return this.getCountValues(stopId, 0);
	}

	/**
	 * I am reasonably sure that the first entry (hour 1) is at array-position 0. kai, sep'14
	 */
	double[]getSimulatedScaled(final Id<TransitStopFacility> stopId) {
		return this.getCountValues(stopId, 1);
	}

	/**
	 * I am reasonably sure that the first entry (hour 1) is at array-position 0. kai, sep'14
	 */
	double[]getRealValues(final Id<TransitStopFacility> stopId) {
		return this.getCountValues(stopId, 2);
	}

	/**
	 * I am reasonably sure that the first entry (hour 1) is at array-position 0. kai, sep'14
	 */
	double[]getCountValues(final Id<TransitStopFacility> stopId, final int col) {
		double[] valueArray = new double[24];
		for (byte i= 0; i<24 ; i++) {
			String hour = String.valueOf(i+1);
			if (this.count.keySet().contains(stopId)) {
				double[] value = this.count.get(stopId).get(hour);
				if (value == null){
					valueArray[i] = 0.0;
				} else {
 					valueArray[i] = value[col] ;  //0 = simulated; 1= simulatedEscaled ;  2=realValues
				}
			} else {
				valueArray = null;
			}
		}
		return valueArray;
	}

}
