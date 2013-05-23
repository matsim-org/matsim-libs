/* *********************************************************************** *
 * project: org.matsim.*
 * SfUTCOffsetDb
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
package air.scenario.utcoffsets;


/**
 * @author dgrether
 *
 */
public class SfUTCOffsetDb {

	public double getOffsetUTC(String originCountry) {

		if (originCountry.equalsIgnoreCase("AD"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AM"))
			return 5;
		else if (originCountry.equalsIgnoreCase("AT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("AX"))
			return 3;
		else if (originCountry.equalsIgnoreCase("AZ"))
			return 5;
		else if (originCountry.equalsIgnoreCase("BA"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("BG"))
			return 3;
		else if (originCountry.equalsIgnoreCase("BY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CH"))
			return 2;
		else if (originCountry.equalsIgnoreCase("CY"))
			return 3;
		else if (originCountry.equalsIgnoreCase("CZ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("DK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("EE"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ES"))
			return 2;
		else if (originCountry.equalsIgnoreCase("FI"))
			return 3;
		else if (originCountry.equalsIgnoreCase("FO"))
			return 1;
		else if (originCountry.equalsIgnoreCase("FR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GB"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("GE"))
			return 4;
		else if (originCountry.equalsIgnoreCase("GG"))
			return 1;
		else if (originCountry.equalsIgnoreCase("GR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("HR"))
			return 2;
		else if (originCountry.equalsIgnoreCase("HU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("IE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IM"))
			return 1;
		else if (originCountry.equalsIgnoreCase("IS"))
			return 0;
		else if (originCountry.equalsIgnoreCase("IT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("JE"))
			return 1;
		else if (originCountry.equalsIgnoreCase("KZ"))
			return 6;
		else if (originCountry.equalsIgnoreCase("LI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LT"))
			return 3;
		else if (originCountry.equalsIgnoreCase("LU"))
			return 2;
		else if (originCountry.equalsIgnoreCase("LV"))
			return 3;
		else if (originCountry.equalsIgnoreCase("MC"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MD"))
			return 3;
		else if (originCountry.equalsIgnoreCase("ME"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("MT"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NL"))
			return 2;
		else if (originCountry.equalsIgnoreCase("NO"))
			return 2;
		else if (originCountry.equalsIgnoreCase("PL"))
			return 2;
		// Azores are UTC, while mainland and Madeira are UTC+1
		else if (originCountry.equalsIgnoreCase("PT"))
			return 1;
		else if (originCountry.equalsIgnoreCase("RO"))
			return 3;
		else if (originCountry.equalsIgnoreCase("RS"))
			return 0;
		// Russia with Moscow time zone offset UTC+4
		else if (originCountry.equalsIgnoreCase("RU"))
			return 4;
		else if (originCountry.equalsIgnoreCase("SE"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SI"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SJ"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SK"))
			return 2;
		else if (originCountry.equalsIgnoreCase("SM"))
			return 2;
		else if (originCountry.equalsIgnoreCase("TR"))
			return 3;
		else if (originCountry.equalsIgnoreCase("UA"))
			return 3;
		else if (originCountry.equalsIgnoreCase("VA"))
			return 2;

		throw new RuntimeException("No UTC offset for country " + originCountry
				+ " found in lookup table. Please add offset first!");
	}
	
}
