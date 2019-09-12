/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.vsp.cadyts.multiModeCadyts;

import java.io.BufferedReader;
import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author amit
 */
public class ModalCountsReader {

	private final String countsTextFile;
	private final Map<Id<Link>, Map<String, double[]>> count = new TreeMap<>();

	public ModalCountsReader(final String countsTextFile){
		this.count.clear();
		this.countsTextFile = countsTextFile;
		readValues();
	}

	private void readValues() {
		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(this.countsTextFile);
			String row = bufferedReader.readLine(); // read first line and do nothing

			while (row != null) {
				row = bufferedReader.readLine();
				if (row != null && !row.equals("")) {
					String[] values = StringUtils.explode(row, '\t');
					Id<Link> id = Id.create(values[0], Link.class);
					String mode = values[1];
					if (!this.count.containsKey(id)) {
						this.count.put(id, new TreeMap<>());
					}

					double vol [] = new double [24];
					for (int index = 2; index <values.length; index++) {
						vol[index-2] = Double.valueOf( values[index] );
					}
					this.count.get(id).put(mode, vol);
				}
			}
			bufferedReader.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not read. Reason "+ e);
		}
	}

	public double[]getSimulatedValues(final Id<Link> locId, final String mode) {
		if (this.count.keySet().contains(locId) && this.count.get(locId).containsKey(mode)) {
			return this.count.get(locId).get(mode);
		} else {
			return null;
		}
	}
}