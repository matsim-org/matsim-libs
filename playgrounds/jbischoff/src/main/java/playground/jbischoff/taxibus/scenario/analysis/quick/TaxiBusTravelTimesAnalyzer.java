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

import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;

import playground.jbischoff.taxibus.algorithm.utils.TaxibusUtils;

/**
 * @author  jbischoff
 *
 */
public class TaxiBusTravelTimesAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler{

	
	double traveltimes = 0;
	double waittimes = 0;
	double departures = 0;
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
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
		System.out.println("Taxibus stats");
		System.out.println("Rides:\t"+this.departures);
		System.out.println("Average Waiting Time:\t"+this.waittimes/departures);
		System.out.println("Average Travel Time:\t"+this.traveltimes/departures);
		
		
		
	}

}
