/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author mrieser
 */
public class DynusTDynamicODDemandWriter {

	private final static Logger log = Logger.getLogger(DynusTDynamicODDemandWriter.class);

	private final ZoneIdToIndexMapping zoneMapping;
	private final DynamicODMatrix demand;
	private double multiplyFactor = 1.0;

	public DynusTDynamicODDemandWriter(final DynamicODMatrix demand, final ZoneIdToIndexMapping zoneMapping) {
		this.demand = demand;
		this.zoneMapping = zoneMapping;
	}

	public void setMultiplyFactor(final double multiplyFactory) {
		this.multiplyFactor = multiplyFactory;
	}

	public void writeTo(final String filename) {
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		Formatter formatter = new Formatter(writer);

		int nOfTimeBins = this.demand.getNOfBins();
		int timeBinSize = this.demand.getBinSize();
		int nOfZones = this.zoneMapping.getNumberOfZones();
		String[] zoneIds = this.zoneMapping.getIndexToIdMapping();

		log.info("Writing dynamic demand for Dynus-T, consisting of " + nOfZones + " zones.");
		
		try {
			writer.write(Integer.toString(nOfTimeBins));
			writer.write(" ");
			writer.write("1.0000");//Double.toString(this.multiplyFactor));
			writer.write("\r\n");
			double time = 0.0;
			for (int i = 0; i < nOfTimeBins; i++) {
				writer.write(" ");
				writer.write(Double.toString(time / 60.0)); // time is written in minutes, not seconds!
				time += timeBinSize;
			}
			writer.write(" ");
			writer.write(Double.toString(time / 60.0)); // add an additional, final time; this time step should also be the first line in system.dat! TODO
			writer.write("\r\n");

			time = 0.0;
			for (int i = 0; i < nOfTimeBins; i++) {
				writer.write("Start Time = ");
				writer.write(Double.toString(time / 60.0));
				writer.write("\r\n");
				Map<String, Map<String, Integer>> matrix = this.demand.getMatrixForTimeBin(i);
				for (int fromZoneIndex = 1; fromZoneIndex <= nOfZones; fromZoneIndex++) {
					Map<String, Integer> row = null;
					if (matrix != null) {
						row = matrix.get(zoneIds[fromZoneIndex]);
					}
					int cnt = 0;
					for (int toZoneIndex = 1; toZoneIndex <= nOfZones; toZoneIndex++) {
						Integer value = null;
						if (row != null) {
							value = row.get(zoneIds[toZoneIndex]);
						}
						if (value == null) {
							writer.write("    0.0000");
						} else {
							formatter.format("%10.4f", this.multiplyFactor * value.floatValue());
						}
						cnt++;
						if (cnt == 6 || toZoneIndex == (nOfZones)) {
							cnt = 0;
							writer.write("\r\n");
						}
					}
				}
				time += timeBinSize;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				log.warn("Could not close stream for file " + filename, e);
			}
		}
	}
}
