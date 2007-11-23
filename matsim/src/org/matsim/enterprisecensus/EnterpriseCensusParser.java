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

package org.matsim.enterprisecensus;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.gbl.Gbl;

public class EnterpriseCensusParser {

    //////////////////////////////////////////////////////////////////////
    // member variables
    //////////////////////////////////////////////////////////////////////

    //////////////////////////////////////////////////////////////////////
    // constructor
    //////////////////////////////////////////////////////////////////////

    public EnterpriseCensusParser(EnterpriseCensus enterpriseCensus) {
    }

    public void parse(EnterpriseCensus enterpriseCensus) {
		//this.createHelperMapping();
		//this.readPresenceCodes(enterpriseCensus);
		this.readHectareAggregations(enterpriseCensus);
		//this.readMunicipalityAggregations(enterpriseCensus);
    }

//    private final void createHelperMapping() {
//		System.out.println("\tEnterpriseCensusParser.createHelperMapping(): Creating the helper mapping...");
//
//		Integer hectare_id;
//		String RELI;
//		Coord lowerLeftCorner = new Coord(0, 0);
//
//		Layer l = World.getSingleton().getLayer("hektar");
//		if (l == null) {
//			Gbl.errorMsg(this.getClass(),"EnterpriseCensusParser(...)","[layer_type=hektar does not exist]");
//		}
//		TreeMap z = l.getLocations();
//		Iterator it = z.keySet().iterator();
//		Iterator it = l.getLocations().iterator();
//		while (it.hasNext()) {
//			Zone z = (Zone)it.next();
//			lowerLeftCorner = z.getMin();
//			RELI = getRELI(lowerLeftCorner);
//			//System.out.println(RELI + "\t" + hectare_id.toString());
//			coord2Hectare.put(RELI, z.getId().toString());
//		}
//		System.out.println("\tEnterpriseCensusParser.createHelperMapping(): Creating the helper mapping...DONE.");
//
//		// test the helper mapping
//
//		System.out.println("\tEnterpriseCensusParser.createHelperMapping: Testing the helper mapping...");
//		ii = coord2Hectare.keySet().iterator();
//		while (ii.hasNext()) {
//		    RELI = (Integer)ii.next();
//		    //lowerLeftCorner = (Coord)ii.next();
//		    int i = ((Integer)coord2Hectare.get(RELI)).intValue();
//		    if (i <= 791) {
//		        System.out.println(RELI + "\t" + (Integer)coord2Hectare.get(RELI));
//		    }
//		}
//		System.out.println("\tEnterpriseCensusParser.createHelperMapping: Testing the helper mapping...DONE.");
//
//    }

//    private final void readPresenceCodes(EnterpriseCensus enterpriseCensus) throws Exception {
//
//		System.out.println("\tEnterpriseCensusParser::readPresenceCodes(): Entering in the presence code file...");
//
//		String file = Config.getSingleton().getParam(Config.EC, Config.EC_PRESENCECODEFILE);
//		String separator = Config.getSingleton().getParam(Config.EC, Config.EC_PRESENCECODESEPARATOR);
//		BufferedReader in = new BufferedReader(new FileReader(file));
//		String[] aCSVLine;
//
//		String hectare_id, RELI, str, errMsg;
//		Coord lowerLeftCorner;
//
//		final int firstNOGAColumn = Integer.valueOf(Config.getSingleton().getParam(Config.EC, Config.EC_FIRSTNOGACOLUMN)).intValue();
//		final int numAttributes = Integer.valueOf(Config.getSingleton().getParam(Config.EC, Config.EC_NUMATTRIBUTES)).intValue();
//		final int numNOGACodes = numAttributes - firstNOGAColumn;
//		final int nogaCodeLength = Integer.valueOf(Config.getSingleton().getParam(Config.EC, Config.EC_NOGACODELENGTH)).intValue();
//
//		// enumerate the NOGA codes from the first line in the presence codes file
//		System.out.println("\t\tEnterpriseCensusParser::readPresenceCodes(): Enumerating NOGA codes...");
//		aCSVLine = (in.readLine()).split(separator);
//
//		// check the correct number of attributes in presence code file
//		if (aCSVLine.length != numAttributes) {
//
//			errMsg = "\n";
//			errMsg = errMsg.concat("Wrong number of attributes:\n");
//			errMsg = errMsg.concat("Is " + aCSVLine.length + ", but should be " + numAttributes + ".\n");
//			errMsg = errMsg.concat("Aborting...\n");
//			Gbl.errorMsg(this.getClass(), "readPresenceCodes(...)", errMsg);
//		}
//
//		for (int ii=0; ii < numNOGACodes; ii++) {
//			str = aCSVLine[ii + firstNOGAColumn];
//			str = removeQuotationMarks(str, '\"');
//			str = str.substring(3, str.length());
//			if (str.length() == nogaCodeLength)
//			{
//				enterpriseCensus.addNOGACode(str);
//			}
//			else
//			{
//				errMsg = "\n";
//				errMsg = errMsg.concat("NOGA codes must have a length of " + nogaCodeLength + " characters.\n");
//				errMsg = errMsg.concat("This one (" + str + ") has a length of " + str.length() + " characters.\n");
//				errMsg = errMsg.concat("Aborting...\n");
//				Gbl.errorMsg(this.getClass(), "readPresenceCodes(...)", errMsg);
//			}
//			//System.out.println(str);
//		}
//
//		System.out.println("\t\tEnterpriseCensusParser::readPresenceCodes(): Enumerating NOGA codes...DONE.");
//
//		// now read the presence codes
//		System.out.println("\t\tEnterpriseCensusParser::readPresenceCodes(): Reading presence codes...");
//		int cnt = 0, skip = 1;
//		while ((str = in.readLine()) != null)
//		{
//			aCSVLine = str.split(separator);
//
//			// check the correct length
//			if (aCSVLine.length != numAttributes) {
//
//				errMsg = "\n";
//				errMsg = errMsg.concat("Line " + new Integer(cnt + 1) + ":\n");
//				errMsg = errMsg.concat("Wrong number of attributes.\n");
//				errMsg = errMsg.concat("Is " + aCSVLine.length + ", but should be " + numAttributes + ".\n");
//				errMsg = errMsg.concat("Aborting...\n");
//				Gbl.errorMsg(this.getClass(), "readPresenceCodes(...)", errMsg);
//			}
//
//			lowerLeftCorner = new Coord(aCSVLine[1], aCSVLine[2]);
//			RELI = getRELI(lowerLeftCorner);
//
//			if (coord2Hectare.containsKey(RELI))
//			{
//				hectare_id = (String)coord2Hectare.get(RELI);
//
//				enterpriseCensus.addPresenceCodeLine(hectare_id, aCSVLine);
//
//				//System.out.println("Inserted hectare nr. " + RELI + "\t" + hectare_id + "\t" + " into presence codes.");
//			}
//
//			cnt++;
//			if ((cnt % skip) == 0) {
//				System.out.println("\t\t\tBrowsed through " + cnt + " hectares.");
//				skip *= 2;
//			}
//		}
//		System.out.println("\t\t\tBrowsed through " + cnt + " hectares.");
//		System.out.println("\t\tEnterpriseCensusParser::readPresenceCodes(): Reading presence codes...DONE.");
//
//		in.close();
//		System.out.println("\tEnterpriseCensusParser::readPresenceCodes(): Reading in the presence code file...DONE.");
//
//    }

//    private final void readMunicipalityAggregations(EnterpriseCensus enterpriseCensus) throws Exception {
//
//		String file = Config.getSingleton().getParam(Config.EC, Config.EC_MUNICIPALITYAGGREGATIONFILE);
//		String separator = Config.getSingleton().getParam(Config.EC, Config.EC_MUNICIPALITYAGGREGATIONSEPARATOR);
//		BufferedReader in = new BufferedReader(new FileReader(file));
//		String[] aCSVLine;
//		String str;
//		int cnt = 0, skip = 1;
//
//		System.out.println("\tEnterpriseCensusParser::readMunicipalityAggregations(): Reading the municipality aggregation file ( " + file + " )...");
//
//		while ((str = in.readLine()) != null)
//		{
//			aCSVLine = str.split(separator);
//
//			enterpriseCensus.addMunicipalityAggregationLine(aCSVLine);
//			cnt++;
//			if ((cnt % skip) == 0) {
//				System.out.println("\t\t\tBrowsed through " + cnt + " municipality / NOGA code pairs.");
//				skip *= 2;
//			}
//		}
//		in.close();
//
//		System.out.println("\tEnterpriseCensusParser::readMunicipalityAggregations(): Reading the municipality aggregation file...DONE.");
//    }

    private final void readHectareAggregations(EnterpriseCensus enterpriseCensus) {

		String file = Gbl.getConfig().getParam(EnterpriseCensus.EC_MODULE, EnterpriseCensus.EC_INPUTHECTAREAGGREGATIONFILE);
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		String line;
		int cnt = 0, skip = 1;

		System.out.println("\tEnterpriseCensusParser::readHectareAggregations(): Reading the hectare aggregation file ( " + file + " )...");

		try {
			System.out.println("\tEnumerate hectare attribute identifiers...");
			line = in.readLine();
			if (line != null) {
				enterpriseCensus.processHectareAggregationHeaderLine(line);
				System.out.println("\tEnumerate hectare attribute identifiers...DONE.");
			}

			System.out.println("\tReading in the hectares...");
			while ((line = in.readLine()) != null)
			{
				enterpriseCensus.addHectareAggregationLine(line);

				cnt++;
				if ((cnt % skip) == 0) {
					System.out.println("\t\t\tBrowsed through " + cnt + " hectares.");
					skip *= 2;
				}
			}
			System.out.println("\tReading in the hectares...DONE.");

			System.out.println("\tEnterpriseCensusParser::readHectareAggregations(): Reading the hectare aggregation file...DONE.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try { in.close(); } catch (IOException ignored) {}
		}
    }

}
