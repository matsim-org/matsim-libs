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

/**
 * 
 */
package playground.jbischoff.csberlin.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.FFCSUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WalkLegPlotter implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private Network network;
	private Map<Id<Person>,String> lastDeparture = new HashMap<>();
	private Map<Id<Person>,String> lastMode = new HashMap<>();
	private List<String> walkLegs = new ArrayList<>();
	private static String SEP = ";";
	/**
	 * 
	 */
	@Inject
	public WalkLegPlotter(Network network) {
		this.network = network;
	}
	
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.walkLegs.clear();
		this.lastMode.clear();
		this.lastDeparture.clear();
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {

		if (event.getLegMode().equals(TransportMode.access_walk)){
			Coord coord = network.getLinks().get(event.getLinkId()).getCoord();
			String s = this.lastDeparture.get(event.getPersonId())+SEP+event.getTime()+SEP+event.getLinkId()+SEP+coord.getX()+SEP+coord.getY();
			this.lastDeparture.put(event.getPersonId(),s);
			
		} else if (event.getLegMode().equals(TransportMode.egress_walk)){
			Coord coord = network.getLinks().get(event.getLinkId()).getCoord();
			String s = this.lastDeparture.get(event.getPersonId())+SEP+event.getTime()+SEP+event.getLinkId()+SEP+coord.getX()+SEP+coord.getY()+SEP+this.lastMode.remove(event.getPersonId());
			this.walkLegs.add(s);
		} 
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.access_walk)){
			Coord coord = network.getLinks().get(event.getLinkId()).getCoord();
			String s = event.getPersonId()+SEP+event.getTime()+SEP+event.getLinkId()+SEP+coord.getX()+SEP+coord.getY();
			this.lastDeparture.put(event.getPersonId(), s);
		} else if (event.getLegMode().equals(TransportMode.egress_walk)){
			Coord coord = network.getLinks().get(event.getLinkId()).getCoord();
			String s = event.getPersonId()+SEP+event.getTime()+SEP+event.getLinkId()+coord.getX()+SEP+coord.getY();
			this.lastDeparture.put(event.getPersonId(), s);
		} else if (event.getLegMode().equals(TransportMode.car)|event.getLegMode().equals(FFCSUtils.FREEFLOATINGMODE)){
			String s = this.lastDeparture.remove(event.getPersonId())+SEP+event.getLegMode();
			this.lastMode.put(event.getPersonId(), event.getLegMode());
			this.walkLegs.add(s);
		}
		
	}
	
	public void plotWalkLegs(String filename){
		String head = "Person;DepartureTime;DepartureLink;DepartureX;DepartureY;ArrivalTime;ArrivalLink;ArrivalX;ArrivalY;CorrMode";
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write(head);
			for (String s : this.walkLegs){
				bw.newLine();
				bw.write(s);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	

}
