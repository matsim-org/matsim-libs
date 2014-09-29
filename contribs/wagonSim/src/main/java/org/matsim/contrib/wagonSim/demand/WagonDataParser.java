/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.demand;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.demand.WagonDataContainer.Wagon;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author balmermi
 * @since 2013-08-19
 */
public class WagonDataParser {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final WagonDataContainer dataContainer;
	private final Date referenceDate;

//	private static final String GV_ART_NEU = "GV_Art_neu";
	private static final String DATUM_VS = "\"Datum_VS\"";
//	private static final String UICBAHN_VS = "UICBahn_VS";
//	private static final String BHFNR_VS = "BhfNr_VS";
//	private static final String BHFNR_EM = "BhfNr_EM";
//	private static final String TRANSPORT_ART = "Transport_Art";
//	private static final String BEF_ART = "Bef_Art";
	private static final String NETTO_GEW = "\"Netto_Gew\"";
//	private static final String BRUTTO_GEW = "Brutto_Gew";
	private static final String VONZELLE = "\"vonzelle\"";
	private static final String NACHZELLE = "\"nachzelle\"";
	private static final String WAGENLAENGE = "\"Wagenlaenge\"";
	private static final String TARA_WAGEN = "\"Tara_Wagen\"";
//	private static final String TYP = "Typ";
//	private static final String NETTO_NEU = "netto_neu";
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public WagonDataParser(WagonDataContainer dataContainer, Date referenceDate) {
		this.dataContainer = dataContainer;
		this.referenceDate = referenceDate;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("deprecation")
	public final void parse(String wagonDataFile) throws IOException {
		
		BufferedReader br = IOUtils.getBufferedReader(wagonDataFile);
		int currRow = 0;
		String curr_line;

		// read header and build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(header[i].trim(),i); }
		
		Map<String,Integer> currIdIndices = new HashMap<String, Integer>();

		// parse rows and store nodes
		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			Date day = null;
			try { day = WagonSimConstants.DATE_FORMAT_DDMMYYYYHHMMSS.parse(row[lookup.get(DATUM_VS)].trim()); }
			catch (ParseException e) { throw new RuntimeException("row "+currRow+": Column '"+DATUM_VS+"' not well fomatted. Bailing out."); }

			// only the same day
			if (referenceDate.getYear() != day.getYear()) { continue; }
			if (referenceDate.getMonth() != day.getMonth()) { continue; }
			if (referenceDate.getDate() != day.getDate()) { continue; }
			
			Double weightLoad = Double.parseDouble(row[lookup.get(NETTO_GEW)].trim());
			String fromZoneId = row[lookup.get(VONZELLE)].trim();
			String toZoneId = row[lookup.get(NACHZELLE)].trim();
			Double length = Double.parseDouble(row[lookup.get(WAGENLAENGE)].trim())/100.0;
			Double weight = Double.parseDouble(row[lookup.get(TARA_WAGEN)].trim());
			
			String idString = fromZoneId.toString()+"-"+toZoneId.toString();
			Integer index = currIdIndices.get(idString);
			if (index == null) { index = 0; currIdIndices.put(idString,index); }
			currIdIndices.put(idString,index+1);
			idString += "_"+index;
			
			Wagon wagon = new Wagon(idString);
			wagon.weight = weight;
			wagon.length = length;
			wagon.weightLoad = weightLoad;
			wagon.fromZoneId = fromZoneId;
			wagon.toZoneId = toZoneId;

			// setting depTime to the next day
			wagon.depTime = (referenceDate.getHours()+24)*3600.0 + referenceDate.getMinutes()*60.0 + referenceDate.getSeconds();

			dataContainer.wagons.put(wagon.id,wagon);
		}
	}
}
