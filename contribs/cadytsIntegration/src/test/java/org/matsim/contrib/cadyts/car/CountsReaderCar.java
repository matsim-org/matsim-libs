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

package org.matsim.contrib.cadyts.car;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.StringUtils;

/**Parses a output text file containing counts comparisons for car
 * This is a modified copy of CountsReader (which is used for the cadyts pt integration)
 * in order to realize the according functionality for the cadyts car integration.
 * At this stage all original pt code is still included here, but outcommeted, to make the adaptions
 * from pt to car well traceable in case of any errors.
 */
class CountsReaderCar {

	private final static Logger log = Logger.getLogger(CountsReaderCar.class);

	// final String STOP_ID_STRING_0 = "StopId :";
	// final String HEAD_STRING_0 = "hour";
	// final String ZERO = "0.0";

	String countsTextFile;
	Map<Id<Link>, Map<String, double[]>> count = new TreeMap<>();

	public CountsReaderCar(final String countsTextFile){
		this.countsTextFile = countsTextFile;
		readValues();
	}

	private void readValues() {
		try {
			FileReader fileReader = new FileReader(this.countsTextFile);
			// ->:correct this : reads first row
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String row = bufferedReader.readLine(); // read first line and do nothing			
			// TODO : include the first row inside the iteration
			// String[] values = StringUtils.explode(row, '\t');

			// Id id = new IdImpl(values[1]);
			while (row != null) {
				row = bufferedReader.readLine();
				if (row != null && row != "") {
					// values = StringUtils.explode(row, '\t');
					String[] values = StringUtils.explode(row, '\t');
					// if (values[0].equals(this.STOP_ID_STRING_0)) {
						//id = new IdImpl(values[1]);
						Id<Link> id = Id.create(values[0], Link.class);
					//}
					// else if (values[0].equals(this.HEAD_STRING_0)) {
						// it does nothing, correct this condition
					//} else {
						if (!this.count.containsKey(id)) {
							this.count.put(id, new TreeMap<String, double[]>());
						}
						// this.count.get(id).put(values[0],
						
						this.count.get(id).put(values[1],
								// new double[] { Double.parseDouble(values[1]), Double.parseDouble(values[2]), Double.parseDouble(values[3]) });
								new double[] { Double.parseDouble(values[2]), Double.parseDouble(values[3])});
					}
				}
			//}
			bufferedReader.close();
			fileReader.close();
		} catch (Exception e) {
			log.error(e);
		}
	}

	// public double[]getSimulatedValues(final Id stopId) {
	public double[]getSimulatedValues(final Id<Link> locId) {
		// return this.getCountValues(stopId, 0);
		return this.getCountValues(locId, 0);
	}

//	public double[]getSimulatedScaled(final Id stopId) {
//		return this.getCountValues(stopId, 1);
//	}

	// public double[]getRealValues(final Id stopId) {
	public double[]getRealValues(final Id<Link> locId) {
		// return this.getCountValues(stopId, 2);
		return this.getCountValues(locId, 1);
	}

	// private double[]getCountValues(final Id stopId, final int col) {
	private double[]getCountValues(final Id<Link> linkId, final int col) {
		double[] valueArray = new double[24];
		for (byte i= 0; i<24 ; i++) {
			String hour = String.valueOf(i+1);
			if (this.count.keySet().contains(linkId)) {
				double[] value = this.count.get(linkId).get(hour);
				if (value == null){
					valueArray[i] = 0.0;
				} else {
 					// valueArray[i] = value[col] ;  //0 = simulated; 1= simulatedEscaled ;  2=realValues
 					valueArray[i] = value[col] ;  //0 = simulated; 1=realValues
				}
			} else {
				valueArray = null;
			}
		}
		return valueArray;
	}

	/**
	 * @return returns a id set of stops listed in the text file
	 */
	public Set<Id<Link>> getStopsIds(){
		return this.count.keySet();
	}

}
