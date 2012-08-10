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
package air.analysis;


/**
 * @author dgrether
 *
 */
public class DgFlightRunsDelayAnalysis {

	public static void main(String[] args) throws Exception {
		String baseDirectory = "/media/data/work/repos/";
		String[] runs = {
				 "1804", 
				"1805"
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

}
