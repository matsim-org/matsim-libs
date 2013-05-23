/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagReader
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
package air.scenario.oag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DgOagReader {

	private static final Logger log = Logger.getLogger(DgOagReader.class);
	
	public List<DgOagLine> readOagLines(String inputOagFile) throws Exception {
		List<DgOagLine> ret = new ArrayList<DgOagLine>();
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFile)));
		int lines = 0;
		while (br.ready()) {
			String oneLine = br.readLine();
			lines++;
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");
			if (lines > 1) {
				for (int jj = 0; jj < 81; jj++) {
					lineEntries[jj] = lineEntries[jj].replaceAll("\"", "");
				}

				DgOagLine l = new DgOagLine(lineEntries);
				ret.add(l);
			}
			if (lines % 50000 == 0){
				log.info("Read " + lines +  " lines of oag data...");
			}
		}
		log.info("Anzahl der Zeilen mit Flügen: " + (lines - 1));
		return ret;
	}

	
}


