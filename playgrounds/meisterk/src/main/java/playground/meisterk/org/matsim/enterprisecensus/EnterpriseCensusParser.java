/* *********************************************************************** *
 * project: org.matsim.*
 * EnterpriseCensusParser.java
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

package playground.meisterk.org.matsim.enterprisecensus;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;

public class EnterpriseCensusParser {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

//	public static final int FIRST_DATA_COL_INDEX = 3;
	
	private static Logger log = Logger.getLogger(EnterpriseCensusParser.class);

	//////////////////////////////////////////////////////////////////////
	// constructor
	//////////////////////////////////////////////////////////////////////

	public EnterpriseCensusParser(EnterpriseCensus ec) {
	}

	public void parse(EnterpriseCensus ec) {
		this.readPresenceCodes(ec);
		this.readHectareAggregations(ec);
	}

	private final void readPresenceCodes(EnterpriseCensus ec) {

		log.info("Reading the presence code file...");

		int lineCounter = 0;
		int skip = 1;

		String filename = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_PRESENCECODEFILE);
		String separator = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_PRESENCECODESEPARATOR);

		File file = new File(filename);

		LineIterator it = null;
		String line = null;
		String[] tokens = null;
		String reli = null;

		try {
			it = FileUtils.lineIterator(file, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (it.hasNext()) {
				line = it.nextLine();
				tokens = line.split(separator);

				if (lineCounter == 0) {
					log.info("Processing header line...");
					for (String token : tokens) {
						ec.addPresenceCodeNOGAType(token.replaceAll("\"", ""));
					}
					log.info("Processing header line...done.");
				} else {

					reli = tokens[0];
					for (int pos = 0; pos < tokens.length; pos++) {
						if (Pattern.matches("1", tokens[pos])) {
							ec.addPresenceCode(
									reli,
									ec.getPresenceCodeNOGAType(pos));
						}
					}					
				}

				lineCounter++;
				if (lineCounter % skip == 0) {
					log.info("Processed hectares: " + Integer.toString(lineCounter));
					skip *= 2;
				}
			}
		} finally {
			LineIterator.closeQuietly(it);
		}

		log.info("Processed hectares: " + Integer.toString(lineCounter));

		log.info("Reading the presence code file...done.");

	}

	private final void readHectareAggregations(EnterpriseCensus ec) {

		log.info("Reading the hectare aggregation file...");

		String filename = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_INPUTHECTAREAGGREGATIONFILE);
		String separator = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_INPUTHECTAREAGGREGATIONSEPARATOR);
		File file = new File(filename);

		LineIterator it = null;
		String line = null;
		String[] tokens = null;
		String reli = null;
		int lineCounter = 0, skip = 1;

		try {
			it = FileUtils.lineIterator(file, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while (it.hasNext()) {
				line = it.nextLine();
				tokens = line.split(separator);

				if (lineCounter == 0) {
					log.info("Processing header line...");
					for (String token : tokens) {
						ec.addhectareAggregationNOGAType(token.replaceAll("\"", ""));
					}
					log.info("Processing header line...done.");
				} else {

					reli = tokens[0];
					for (int pos = 0; pos < tokens.length; pos++) {
						if (!Pattern.matches("0", tokens[pos])) {
							ec.addHectareAggregationInformation(
									reli,
									ec.getHectareAggregationNOGAType(pos),
									Double.parseDouble(tokens[pos]));
						}
					}					
				}

				lineCounter++;
				if (lineCounter % skip == 0) {
					log.info("Processed hectares: " + Integer.toString(lineCounter));
					skip *= 2;
				}
			}
		} finally {
			LineIterator.closeQuietly(it);
		}

		log.info("Processed hectares: " + Integer.toString(lineCounter));

		log.info("Reading the hectare aggregation file...done.");

	}
}
