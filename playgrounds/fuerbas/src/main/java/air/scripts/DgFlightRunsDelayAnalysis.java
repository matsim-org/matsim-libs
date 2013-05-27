/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightRunsDelayAnalysis
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
package air.scripts;

import air.analysis.delay.DgDelayAnalysis;
import air.analysis.delay.SfFlightDelayAnalysis;


/**
 * @author dgrether
 *
 */
public class DgFlightRunsDelayAnalysis {

	public void runSfDelayAnalysis() throws Exception{
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
//				 "1804", 
//				"1805",
				 "1809"
				};
		String euTimesBase = baseDirectory + "shared-svn/studies/countries/eu/flight/";
		String[] scheduledTimes = {
				euTimesBase + "dg_oag_flight_model_2_runways_3600vph/oag_flights.txt",
				euTimesBase + "dg_oag_flight_model_2_runways_60vph/oag_flights.txt",
		};
		
		for (int i = 0; i < runs.length; i++){
			String rundir = baseDirectory + "runs-svn/run" + runs[i] + "/";
			String actualTimes = rundir + "ITERS/it.0/" + runs[i] + ".0.statistic.csv";
			String delayOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.delay.csv";
			String delaySingleFlight = rundir + "ITERS/it.0/" + runs[i] + ".0.delay_by_flight.csv";
			new SfFlightDelayAnalysis().analyzeDelays(scheduledTimes[i], actualTimes, delayOutput, delaySingleFlight);
		}
		
	}
	
	public static void main(String[] args) throws Exception {
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
//				 "1804", 
//				"1805",
//				 "1809",
//				 "1810",
//				 "1811",
//				 "1812",
//				 "1813",
				 "1814",
				 "1815",
				 "1816",
				 "1817",
				 "1818"
				};
		String euTimesBase = baseDirectory + "shared-svn/studies/countries/eu/flight/";
		String[] scheduledTimes = {
//				euTimesBase + "dg_oag_flight_model_2_runways_3600vph/oag_flights.txt",
//				euTimesBase + "dg_oag_flight_model_2_runways_60vph/oag_flights.txt",
//				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_airport_capacities_www/oag_flights.txt",
//				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_60vph/oag_flights.txt",
//				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_3600vph/oag_flights.txt",
//				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_3600vph/oag_flights.txt",
//				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_airport_capacities_www/oag_flights.txt",
				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/oag_flights.txt",
				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_60vph_storage_restriction/oag_flights.txt",
				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_3600vph_storage_restriction/oag_flights.txt",
				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_3600vph_storage_restriction/oag_flights.txt",
				euTimesBase + "dg_oag_tuesday_flight_model_2_runways_airport_capacities_www_storage_restriction/oag_flights.txt",
				
		};
		
		for (int i = 0; i < runs.length; i++){
			String rundir = baseDirectory + "runs-svn/run" + runs[i] + "/";
			String eventsFilename = rundir + "ITERS/it.0/" + runs[i] + ".0.events.xml.gz";
			String delayOutput = rundir + "ITERS/it.0/" + runs[i] + ".0.dgdelay.csv";
			String delaySingleFlight = rundir + "ITERS/it.0/" + runs[i] + ".0.dgdelay_by_flight.csv";
			String arrivalDelayByOriginAirport = rundir + "ITERS/it.0/" + runs[i] + ".0.arrival_delay_by_origin_airport.csv";
			String arrivalDelayByDestinationAirport = rundir + "ITERS/it.0/" + runs[i] + ".0.arrival_delay_by_destination_airport.csv";
			DgDelayAnalysis ana = new DgDelayAnalysis();
			ana.analyzeDelays(scheduledTimes[i], eventsFilename);
			ana.writeArrivalDelaysByMinutes(delayOutput);
			ana.writeArrivalDelaysByOriginAirport(arrivalDelayByOriginAirport);
			ana.writeArrivalDelaysByOriginAirport(arrivalDelayByDestinationAirport);
			ana.writeDelayByFlight(delaySingleFlight);
		}

		
	}

}
