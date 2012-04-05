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
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.StringUtils;

/**parses a output text file containing counts comparisons*/
public class CountsReader {

	private final static Logger log = Logger.getLogger(CountsReader.class);

	final String STOP_ID_STRING_0 = "StopId :";
	final String HEAD_STRING_0 = "hour";
	final String ZERO = "0.0";

	String countsTextFile;
	Map<Id, Map<String, double[]>> count = new TreeMap<Id, Map<String, double[]>>();

	public CountsReader(final String countsTextFile){
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

			Id id = new IdImpl(values[1]);
			while (row != null) {
				row = bufferedReader.readLine();
				if (row != null && row != "") {
					values = StringUtils.explode(row, '\t');
					if (values[0].equals(this.STOP_ID_STRING_0)) {
						id = new IdImpl(values[1]);
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

	public double[]getSimulatedValues(final Id stopId) {
		return this.getCountValues(stopId, 0);
	}

	public double[]getSimulatedScaled(final Id stopId) {
		return this.getCountValues(stopId, 1);
	}

	public double[]getRealValues(final Id stopId) {
		return this.getCountValues(stopId, 2);
	}

	private double[]getCountValues(final Id stopId, final int col) {
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

	/**
	 * @return returns a id set of stops listed in the text file
	 */
	public Set<Id> getStopsIds(){
		return this.count.keySet();
	}

}
