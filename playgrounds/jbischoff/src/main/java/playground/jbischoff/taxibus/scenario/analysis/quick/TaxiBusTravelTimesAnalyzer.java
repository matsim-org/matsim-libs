/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import java.io.BufferedWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxiBusTravelTimesAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler{

	private static final Logger log = Logger.getLogger(TaxiBusTravelTimesAnalyzer.class);

	double traveltimes = 0;
	double waittimes = 0;
	double departures = 0;
	
	
	@Override
	public void reset(int iteration) {
		log.info("Taxibus statistics for iteration: "+(iteration-1));
		log.info("Taxibus stats");
		log.info("Rides:\t"+this.departures);
		log.info("Average Waiting Time:\t"+this.waittimes/departures);
		log.info("Average Travel Time:\t"+this.traveltimes/departures);
		
		traveltimes = 0;
		waittimes= 0;
		departures = 0;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if ((event.getVehicleId().toString().startsWith("tb"))&&(event.getPersonId().toString().startsWith("BS_WB"))){
			this.waittimes += event.getTime();
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TaxibusUtils.TAXIBUS_MODE)){
			this.traveltimes += event.getTime();

		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
				if (event.getLegMode().equals(TaxibusUtils.TAXIBUS_MODE)){
					this.waittimes -= event.getTime();
					this.traveltimes -= event.getTime();
					this.departures++;
				}
	}
	
	public void printOutput(){
		
		System.out.println(writeOutput());
		
		
		
	}
	
	private String writeOutput(){
		return ("Taxibus stats\n"+
				"Rides:\t"+this.departures+
				"\nAverage Waiting Time:\t"+this.waittimes/departures +
				"\nAverage Travel Time:\t"+this.traveltimes/departures);
	}

	public void writeOutput(String folder) {
		BufferedWriter bw = IOUtils.getBufferedWriter(folder+"/taxibusstats.txt");
		try {
			bw.write(writeOutput());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
