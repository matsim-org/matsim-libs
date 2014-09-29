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
package org.matsim.contrib.wagonSim.schedule;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.schedule.OTTDataContainer.Locomotive;
import org.matsim.contrib.wagonSim.schedule.OTTDataContainer.StationData;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author balmermi @ Senozon AG
 * @since 2013-07-08
 */
public class OTTScheduleParser {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	private final OTTDataContainer dataContainer;

	private static final String VERKEHRSTAG = "Verkehrstag";
	private static final String ZUGNUMMER = "Zugnummer";
	private static final String IST_AN = "IST AN";
	private static final String IST_AB = "IST AB";
	private static final String VERSPAETUNG_AN = "Verspaetung AN";
	private static final String VERSPAETUNG_AB = "Verspaetung AB";
	private static final String BETRIEBSSTELLE = "Betriebstelle";
	private static final String ZUGART = "Zugart";
	
	private static final double DEFAULTFLAG = 999999.0;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public OTTScheduleParser(OTTDataContainer dataContainer) {
		this.dataContainer = dataContainer;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public final void parse(String ottFile, Map<Id<Node>, Id<Node>> nodeMap) throws IOException {

		BufferedReader br = IOUtils.getBufferedReader(ottFile);
		int currRow = 0;
		String curr_line;

		// read header an build lookup
		curr_line = br.readLine(); currRow++;
		String [] header = curr_line.split(";");
		Map<String, Integer> lookup = new LinkedHashMap<String,Integer>(header.length);
		for (int i=0; i<header.length; i++) { lookup.put(Utils.removeSurroundingQuotes(header[i].trim()),i); }

		// to check for undefined time 
		Date dateZero = new Date(0);

		// parse rows and store nodes
		StationData prevStationData = null;
		while ((curr_line = br.readLine()) != null) {
			currRow++;
			String [] row = curr_line.split(";");

			Date day = null;
			try { day = WagonSimConstants.DATE_FORMAT_DDMMYYYYHHMMSS.parse(Utils.removeSurroundingQuotes(row[lookup.get(VERKEHRSTAG)].trim())); }
			catch (ParseException e) { throw new RuntimeException("row "+currRow+": Column '"+VERKEHRSTAG+"' not well fomatted. Bailing out."); }
			Integer locNr = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(ZUGNUMMER)].trim()));
			Date arrival = null;
			try { arrival = WagonSimConstants.DATE_FORMAT_YYYYMMDDHHMMSS.parse(Utils.removeSurroundingQuotes(row[lookup.get(IST_AN)].trim())); }
			catch (ParseException e) { throw new RuntimeException("row "+currRow+": Column '"+IST_AN+"' not well fomatted. Bailing out."); }
			Date departure = null;
			try { departure = WagonSimConstants.DATE_FORMAT_YYYYMMDDHHMMSS.parse(Utils.removeSurroundingQuotes(row[lookup.get(IST_AB)].trim())); }
			catch (ParseException e) { throw new RuntimeException("row "+currRow+": Column '"+IST_AB+"' not well fomatted. Bailing out."); }
			double delayArrival = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(VERSPAETUNG_AN)].trim()));
			double delayDeparture = Double.parseDouble(Utils.removeSurroundingQuotes(row[lookup.get(VERSPAETUNG_AB)].trim()));
			Id<TransitStopFacility> station = Id.create(Utils.removeSurroundingQuotes(row[lookup.get(BETRIEBSSTELLE)].trim()), TransitStopFacility.class);
			int locType = Integer.parseInt(Utils.removeSurroundingQuotes(row[lookup.get(ZUGART)].trim()));
			
			Locomotive locomotive = dataContainer.locomotives.get(Id.create(locNr.longValue(), Locomotive.class));
			if (locomotive == null) { locomotive = new Locomotive(locNr,locType); dataContainer.locomotives.put(locomotive.id,locomotive); prevStationData = null; }
			if (locomotive.type.intValue() != locType) { throw new RuntimeException("row "+currRow+": alredy given locomotive type="+locomotive.type+" does not fit with locType="+locType+". Bailing out."); }

			// adapt default arrival to the given departure
			if (delayArrival == DEFAULTFLAG) {
				if (!arrival.before(dateZero)) { throw new RuntimeException("row "+currRow+": default arrival ("+delayArrival+") does not fit to arrival time ("+arrival.toString()+"). Bailing out."); }
				if (delayDeparture == DEFAULTFLAG) { throw new RuntimeException("row "+currRow+": default given for arrival ("+delayArrival+") and departure ("+delayDeparture+"). Bailing out."); }
				arrival = new Date(departure.getTime());
			}
			
			// adapt default departure to the given arrival
			if (delayDeparture == DEFAULTFLAG) {
				if (!departure.before(dateZero)) { throw new RuntimeException("row "+currRow+": default departure ("+delayDeparture+") does not fit to departure time ("+departure.toString()+"). Bailing out."); }
				if (delayArrival == DEFAULTFLAG) { throw new RuntimeException("row "+currRow+": default given for arrival ("+delayArrival+") and departure ("+delayDeparture+"). Bailing out."); }
				departure = new Date(arrival.getTime());
			}

			// check if there is the same time twice
			if (locomotive.trips.containsKey(arrival)) { throw new RuntimeException("row "+currRow+": locomotive id="+locomotive.id+" already arrived at a station at "+IST_AN+"="+arrival.toString()+". Bailing out."); }
			
			// reset departure and delayDeparture if it's earlier than arrival
			if (departure.before(arrival)) {
				long delta = (arrival.getTime() - departure.getTime())/1000l; // delta in seconds
				delayDeparture += delta;
				departure = new Date(arrival.getTime());
			}
			
			// replace station ids according to nodeMap
			Id<Node> mappedStationId = nodeMap.get(Id.create(station, Node.class));
			if (mappedStationId != null) {
				station = Id.create(mappedStationId.toString(), TransitStopFacility.class);
			}
			
			// take care that there is no DEFAULTFLAG ANYMORE
			if (delayArrival == DEFAULTFLAG) { delayArrival = delayDeparture; }
			if (delayDeparture == DEFAULTFLAG) { delayDeparture = delayArrival; }

			if ((prevStationData != null) && (prevStationData.stationId.equals(station))) {
				prevStationData.delayDeparture = delayDeparture;
				prevStationData.departure = departure;
				System.out.println("row "+currRow+": station "+station.toString()+" is the same as before. Merging station data into one (to remove loop trips).");
			}
			else {
				StationData stationData = new StationData();
				stationData.stationId = station;
				stationData.delayArrival = delayArrival;
				stationData.arrival = arrival;
				stationData.delayDeparture = delayDeparture;
				stationData.departure = departure;
				locomotive.trips.put(stationData.arrival,stationData);
				prevStationData = stationData;
			}
		}
	}
}
