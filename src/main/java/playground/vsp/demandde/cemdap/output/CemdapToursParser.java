/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.vsp.demandde.cemdap.output;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author dziemke
 */
@Deprecated
public class CemdapToursParser {

	private final static Logger log = Logger.getLogger(CemdapToursParser.class);

	// cemdap tour file columns
	private static final int HH_ID = 0;
	private static final int P_ID = 1;
	private static final int TOUR_ID = 2;
	//private static final int STOP_ID = 3;
	private static final int TOUR_MODE = 4;

	
	public CemdapToursParser() {
	}

	
	// parse method
	public final void parse(String cemdapToursFile, Map<String, String> tourAttributesMap) {
		int lineCount = 0;

		try {
			BufferedReader bufferedReader = IOUtils.getBufferedReader(cemdapToursFile);
			//String currentLine = bufferedReader.readLine();
			String currentLine = null;

			// data
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				lineCount++;
				
				if (lineCount % 1000000 == 0) {
					log.info("line " + lineCount + ": parsed so far.");
					Gbl.printMemoryUsage();
				}
				
				Integer householdId = Integer.parseInt(entries[HH_ID]);
				Integer personId = Integer.parseInt(entries[P_ID]);
				Integer tourId = Integer.parseInt(entries[TOUR_ID]);
				String combinedId = householdId.toString() + "_" + personId.toString() + "_" + tourId.toString();
				
				//Integer tourMode = Integer.parseInt(entries[TOUR_MODE]);
				
				String modeType = transformModeType(Integer.parseInt(entries[TOUR_MODE]));
				
				//tourAttributesMap.put(combinedId, tourMode);
				tourAttributesMap.put(combinedId, modeType);
			}
		} catch (IOException e) {
			log.error(e);
			//Gbl.errorMsg(e);
		}
		log.info(lineCount+" lines parsed.");
	}
	
	
	private final String transformModeType(int modeTypeNumber) {
		switch (modeTypeNumber) {
		// TODO Check if this is correct
		case 0: return "car";
		case 1: return "car";
		case 2: return "walk";
		case 3: return "pt";
		case 4: return "car";
		case 5: return "car";
		case 6: return "pt";
		case 7: return "car";
		default:
			log.error(new IllegalArgumentException("modeTypeNo="+modeTypeNumber+" not allowed."));
			return null;
		}
	}
}