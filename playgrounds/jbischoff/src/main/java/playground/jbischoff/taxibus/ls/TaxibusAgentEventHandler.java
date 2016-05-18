/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.ls;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author jbischoff
 *
 */
public class TaxibusAgentEventHandler
		implements PersonDepartureEventHandler, ActivityEndEventHandler, ActivityStartEventHandler {
	Map<Id<Person>, Double> departureTimes = new HashMap<>();
	Set<Id<Person>> taxibusCustomers = new HashSet<>();
	List<Double> taxibusTripTravelTimes = new ArrayList<>();

	@Override
	public void reset(int iteration) {
		this.departureTimes.clear();
		this.taxibusCustomers.clear();
		this.taxibusTripTravelTimes.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("pt interaction"))
			return;
		if (event.getActType().equals("taxibus interaction"))
			return;
		double departureTime = departureTimes.remove(event.getPersonId());
		double arrivalTime = event.getTime();
		if (this.taxibusCustomers.contains(event.getPersonId())) {
			double travelTime = arrivalTime - departureTime;
			this.taxibusTripTravelTimes.add(travelTime);
			this.taxibusCustomers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("pt interaction"))
			return;
		if (event.getActType().equals("taxibus interaction"))
			return;

		this.departureTimes.put(event.getPersonId(), event.getTime());

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("taxibus")) {
			this.taxibusCustomers.add(event.getPersonId());
		}
	}

	public void writeListToFile(String filename) {
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			for (Double d : this.taxibusTripTravelTimes) {
				bw.write(Time.writeTime(d));				
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
