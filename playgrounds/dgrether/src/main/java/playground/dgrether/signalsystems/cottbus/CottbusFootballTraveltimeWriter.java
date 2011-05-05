/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFootballTraveltimeWriter
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
package playground.dgrether.signalsystems.cottbus;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFootballStrings;


/**
 * @author dgrether
 *
 */
public class CottbusFootballTraveltimeWriter {

	
	private static final Logger log = Logger.getLogger(CottbusFootballTraveltimeWriter.class);
	

	public void exportLatestArrivals(CottbusFootballTraveltimeHandler traveltimeHandler, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			writer.append(CottbusFootballStrings.CB2FB);
			writer.append(CottbusFootballStrings.SEPARATOR);
			writer.append(CottbusFootballStrings.SPN2FB);
			writer.append(CottbusFootballStrings.SEPARATOR);
			writer.append(CottbusFootballStrings.FB2CB);
			writer.append(CottbusFootballStrings.SEPARATOR);
			writer.append(CottbusFootballStrings.FB2SPN);
			writer.append(CottbusFootballStrings.SEPARATOR);
			writer.newLine();
			if (! traveltimeHandler.getArrivalTimesCB2FB().isEmpty() && ! traveltimeHandler.getArrivalTimesSPN2FB().isEmpty()){
				writer.append(Collections.max(traveltimeHandler.getArrivalTimesCB2FB().values()) + CottbusFootballStrings.SEPARATOR
						+ Collections.max(traveltimeHandler.getArrivalTimesSPN2FB().values()) + CottbusFootballStrings.SEPARATOR
						+ Collections.max(traveltimeHandler.getArrivalTimesFB2CB().values()) + CottbusFootballStrings.SEPARATOR
						+ Collections.max(traveltimeHandler.getArrivalTimesFB2SPN().values()) + CottbusFootballStrings.SEPARATOR);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeMapToCsv(Map<Id, Double> atmap, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);

			for (Entry<Id, Double> e : atmap.entrySet()) {
				writer.append(e.getKey().toString() + CottbusFootballStrings.SEPARATOR + e.getValue());
				writer.newLine();
			}
			writer.flush();
			writer.close();
			log.info("Wrote " + filename);
		} catch (IOException e1) {
			log.error("cannot write to file: " + filename);
			e1.printStackTrace();
		}

	}

}
