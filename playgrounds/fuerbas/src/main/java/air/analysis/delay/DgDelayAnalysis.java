/* *********************************************************************** *
 * project: org.matsim.*
 * DgDelayAnalysis
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
package air.analysis.delay;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import air.scenario.oag.DgOagFlightsData;
import air.scenario.oag.DgOagFlightsReader;



/**
 * @author dgrether
 *
 */
public class DgDelayAnalysis {

	private static final Logger log = Logger.getLogger(DgDelayAnalysis.class);

	private Map<String, DgFlightDelay> delays= null;

	public void analyzeDelays(String oagFlightsFilename, 
			String eventsFilename) {

		DgOagFlightsData oagFlights = new DgOagFlightsData();
		new DgOagFlightsReader(oagFlights).readFile(oagFlightsFilename);
		
		DgDelayEventHandler handler = new DgDelayEventHandler(oagFlights);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		log.info("Reading events from: " + eventsFilename);
		eventsReader.readFile(eventsFilename);
		
		this.delays = handler.getFlightDelaysByFlightDesignatorMap();
		log.info("delays are analyzed");
	}

	public void writeDelayByFlight(String arrivalDelayByFlightFilename) throws IOException {
		BufferedWriter bw = IOUtils.getBufferedWriter(arrivalDelayByFlightFilename);
		//write header
		StringBuilder header = new StringBuilder();
		header.append("flight designator");
		header.append("\t");
		header.append("scheduled departure[s]");
		header.append("\t");
		header.append("scheduled arrival[s]");
		header.append("\t");
		header.append("actual departure[s]");
		header.append("\t");
		header.append("actual arrival[s]");
		header.append("\t");
		header.append("departure delay[s]");
		header.append("\t");
		header.append("arrival delay[s]");
		bw.write(header.toString());
		bw.newLine();
		for (DgFlightDelay fd : this.delays.values()){
			StringBuilder line = new StringBuilder();
			line.append(fd.getFlight().getFlightDesignator());
			line.append("\t");
			line.append(fd.getFlight().getDepartureTime());
			line.append("\t");
			line.append(fd.getFlight().getDepartureTime() + fd.getFlight().getScheduledDuration());
			line.append("\t");
			line.append(fd.getActualDepartureTime());
			line.append("\t");
			line.append(fd.getActualArrivalTime());
			line.append("\t");
			line.append(fd.getDepartureDelay());
			line.append("\t");
			line.append(fd.getArrivalDelay());
			bw.write(line.toString());
			bw.newLine();
		}
		bw.flush();
		bw.close();
	}
	
	public void writeArrivalDelaysByMinutes(String delayOutput) throws IOException {
		log.info("Writing arrival delays by minutes to " + delayOutput + "...");
		SortedMap<Integer, Integer> delay = new TreeMap<Integer, Integer>();
		for (DgFlightDelay fd : this.delays.values()){
			Integer delayMin = (int) (fd.getArrivalDelay() / 60);
			if (!delay.containsKey(delayMin)) {
				delay.put(delayMin, 0);
			}
			int soFar = delay.get(delayMin);
			soFar++;
			delay.put(delayMin, soFar);
		}
		BufferedWriter bw = IOUtils.getBufferedWriter(delayOutput);
		//write header
		StringBuilder header = new StringBuilder();
		header.append("arrival delay [min]");
		header.append("\t");
		header.append("Number of delayed flights");
		bw.write(header.toString());
		bw.newLine();
		for (int i = delay.firstKey() - 2; i < delay.lastKey() + 2; i ++) {
			Integer numberFlights = delay.get(i);
			if (numberFlights == null) {
				numberFlights = 0;
			}
			bw.write(Integer.toString(i) +"\t" + numberFlights);
			bw.newLine();
		}

		bw.flush();
		bw.close();
		log.info("  done.");
	}

	public void writeArrivalDelaysByOriginAirport(String arrivalDelayByOriginAirport) throws IOException {
		log.info("Writing arrival delays by origin airport to " + arrivalDelayByOriginAirport + "...");
		SortedMap<Integer, Map<String, Integer>> map = new TreeMap<Integer, Map<String, Integer>>();
		SortedSet<String> allAirportCodes = new TreeSet<String>();
		for (DgFlightDelay fd : this.delays.values()){
			Integer delayMin = (int) (fd.getArrivalDelay() / 60);
			allAirportCodes.add(fd.getFlight().getOriginCode());
			if (! map.containsKey(delayMin)){
				map.put(delayMin, new HashMap<String, Integer>());
			}
			Map<String, Integer> airportDelays = map.get(delayMin);
			if (! airportDelays.containsKey(fd.getFlight().getOriginCode())){
				airportDelays.put(fd.getFlight().getOriginCode(), 0);
			}
			Integer noDelayedFlights = airportDelays.get(fd.getFlight().getOriginCode());
			airportDelays.put(fd.getFlight().getOriginCode(), noDelayedFlights + 1);
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(arrivalDelayByOriginAirport);
		//write header
		StringBuilder header = new StringBuilder();
		header.append("arrival delay [min]");
		for (String airportCode : allAirportCodes){
			header.append("\t");
			header.append(airportCode);
		}
		bw.write(header.toString());
		bw.newLine();
		//write data
		for (Entry<Integer, Map<String, Integer>> entry : map.entrySet()){
			StringBuilder line = new StringBuilder();
			line.append(entry.getKey());
			Map<String, Integer> airportDelays = entry.getValue();
			for (String airportCode : allAirportCodes){
				line.append("\t");
				if (! airportDelays.containsKey(airportCode)){
					line.append("0");
				}
				else {
					line.append(airportDelays.get(airportCode));
				}
			}
			bw.write(line.toString());
			bw.newLine();
		}
		bw.flush();
		bw.close();
		log.info("  done.");
	}

	
	public void writeArrivalDelaysByDestinationAirport(String arrivalDelayByDestinationAirport) throws IOException {
		log.info("Writing arrival delays by origin airport to " + arrivalDelayByDestinationAirport + "...");
		SortedMap<Integer, Map<String, Integer>> map = new TreeMap<Integer, Map<String, Integer>>();
		SortedSet<String> allAirportCodes = new TreeSet<String>();
		for (DgFlightDelay fd : this.delays.values()){
			Integer delayMin = (int) (fd.getArrivalDelay() / 60);
			allAirportCodes.add(fd.getFlight().getDestinationCode());
			if (! map.containsKey(delayMin)){
				map.put(delayMin, new HashMap<String, Integer>());
			}
			Map<String, Integer> airportDelays = map.get(delayMin);
			if (! airportDelays.containsKey(fd.getFlight().getDestinationCode())){
				airportDelays.put(fd.getFlight().getDestinationCode(), 0);
			}
			Integer noDelayedFlights = airportDelays.get(fd.getFlight().getDestinationCode());
			airportDelays.put(fd.getFlight().getDestinationCode(), noDelayedFlights + 1);
		}
		
		BufferedWriter bw = IOUtils.getBufferedWriter(arrivalDelayByDestinationAirport);
		//write header
		StringBuilder header = new StringBuilder();
		header.append("arrival delay [min]");
		for (String airportCode : allAirportCodes){
			header.append("\t");
			header.append(airportCode);
		}
		bw.write(header.toString());
		bw.newLine();
		//write data
		for (Entry<Integer, Map<String, Integer>> entry : map.entrySet()){
			StringBuilder line = new StringBuilder();
			line.append(entry.getKey());
			Map<String, Integer> airportDelays = entry.getValue();
			for (String airportCode : allAirportCodes){
				line.append("\t");
				if (! airportDelays.containsKey(airportCode)){
					line.append("0");
				}
				else {
					line.append(airportDelays.get(airportCode));
				}
			}
			bw.write(line.toString());
			bw.newLine();
		}
		bw.flush();
		bw.close();
		log.info("  done.");
	}

	
}
