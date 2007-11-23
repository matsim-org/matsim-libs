/* *********************************************************************** *
 * project: org.matsim.*
 * EnterpriseCensusWriter.java
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

package org.matsim.enterprisecensus;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.utils.io.IOUtils;

public class EnterpriseCensusWriter {

	public EnterpriseCensusWriter() {
	}

	public void write(EnterpriseCensus enterpriseCensus) {

		this.writeHectareAggregations(enterpriseCensus);

	}

	private final void writeHectareAggregations(EnterpriseCensus enterpriseCensus) {

		String file = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_OUTPUTHECTAREAGGREGATIONFILE);
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(file);
			int cnt = 0, skip = 1;
			String line;

			System.out.println("\tEnterpriseCensusParser::readHectareAggregations(): Writing the hectare aggregation file ( " + file + " )...");

			System.out.println("\tWrite hectare attribute identifiers...");

			line = enterpriseCensus.getHectareAggregationHeaderLine();
			out.write(line);
			System.out.println("\tWrite hectare attribute identifiers...DONE.");

			System.out.println("\tWriting out the hectares...");
			TreeMap<String, double[]> ecHectars = enterpriseCensus.getEnterpriseCensusHectareAggregation();
			Iterator<String> it = ecHectars.keySet().iterator();
			while (it.hasNext()) {
				line = enterpriseCensus.getHectareAggregationLine(it.next());
				out.write(line);

				cnt++;
				if ((cnt % skip) == 0) {
					System.out.println("\t\t\tBrowsed through " + cnt + " hectares.");
					skip *= 2;
				}
			}
			System.out.println("\tWriting out the hectares...DONE.");

			out.close();
			System.out.println("\tEnterpriseCensusParser::readHectareAggregations(): Writing the hectare aggregation file...DONE.");
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
