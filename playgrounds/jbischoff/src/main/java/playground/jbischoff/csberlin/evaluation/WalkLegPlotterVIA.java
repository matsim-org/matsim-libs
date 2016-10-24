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


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WalkLegPlotterVIA implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	private Network network;
	private Map<Id<Person>,String> departureId = new HashMap<>();
	private Map<Id<Person>,String> arrivalId = new HashMap<>();
	private Map<Id<Person>,String> departureStrings = new HashMap<>();
	private List<String> walkLegs = new ArrayList<>();
	private String mode;
	private static String SEP = ";";
	/**
	 * 
	 */
	@Inject
	public WalkLegPlotterVIA(Network network, String monitoredMode) {
		this.network = network;
		this.mode = monitoredMode;
	}
	
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		this.walkLegs.clear();
		this.departureId.clear();
		this.arrivalId.clear();
		this.departureStrings.clear();
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(mode)){
			String departureId = event.getPersonId().toString()+event.getTime()+event.getLegMode();
			this.arrivalId.put(event.getPersonId(), departureId);
			Coord coord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
			this.walkLegs.add(departureId+SEP+(event.getTime()+60.0)+SEP+coord.getX()+SEP+coord.getY());
		
		} else if (event.getLegMode().equals(TransportMode.egress_walk)){
			if (this.arrivalId.containsKey(event.getPersonId())){
			String departureId = this.arrivalId.remove(event.getPersonId());
			Coord coord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
			this.walkLegs.add(departureId+SEP+(event.getTime())+SEP+coord.getX()+SEP+coord.getY());
			}
		}
		else{
			this.arrivalId.remove(event.getPersonId());
		}
		
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.access_walk)){
			String departureId = event.getPersonId().toString()+event.getTime()+event.getLegMode();
			this.departureId.put(event.getPersonId(), departureId);
			Coord coord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
			String departureString = (departureId+SEP+(event.getTime())+SEP+coord.getX()+SEP+coord.getY());
			this.departureStrings.put(event.getPersonId(), departureString);
		
		} else if (event.getLegMode().equals(mode)){
			if (this.departureId.containsKey(event.getPersonId())){
			this.walkLegs.add(departureStrings.get(event.getPersonId()));	
			String departureId = this.departureId.remove(event.getPersonId());
			Coord coord = network.getLinks().get(event.getLinkId()).getToNode().getCoord();
			this.walkLegs.add(departureId+SEP+(event.getTime()-60.0)+SEP+coord.getX()+SEP+coord.getY());
			}
		}
		else{
			this.departureId.remove(event.getPersonId());
		}
		
		
	}
	
	public void plotWalkLegs(String filename){
		String head = "LocId;time;X;Y";
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
