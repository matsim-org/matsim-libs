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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.core.utils.misc.Time;


/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TravelTimeWithParkingEvaluator {
	final static String EVENTSFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09_park/bc09_park.output_events.xml.gz";
	final static String NETWORKFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/output_network.xml.gz";
	final static String LINKSFILE = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/klauslinks.txt";
	
	final static String OUTFILE = "D:/runs-svn/bmw_carsharing/basecase/bc09-nopark/klaus-links-use.csv";
//	final static String EVENTSFILE = "D:/runs-svn/bmw_carsharing/run23/run23.output_events.xml.gz";
//	final static String NETWORKFILE = "D:/runs-svn/bmw_carsharing/run23/run23.output_network.xml.gz";
//	final static String LINKSFILE = "C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/parkplaetze.txt";
//	
//	final static String OUTFILE = "D:/runs-svn/bmw_carsharing/run23/emissions_co2.csv";
	// according to http://www.co2online.de/klima-schuetzen/mobilitaet/auto-co2-ausstoss/
	
	public static void main(String[] args) {
		new TravelTimeWithParkingEvaluator().run();
	}
	
	private void run(){
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETWORKFILE);
		EventsManager events = EventsUtils.createEventsManager();
		final Map<Id<Link>,MutableDouble> links = new HashMap<>();
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(LINKSFILE);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.put(linkId, new MutableDouble(0.0));
			}
		
        });
        ParkTravelTimeHandler ph = new ParkTravelTimeHandler(links.keySet());
        events.addHandler(ph);
        
        new MatsimEventsReader(events).readFile(EVENTSFILE);
        ph.printResults();
	}
}


class ParkTravelTimeHandler implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler{

	final Set<Id<Link>> monitoredLinks;
	final Set<Id<Person>> carDepartures = new HashSet<>();
	final Map<Id<Person>,Id<Link>> lastDepartureLocation = new HashMap<>();
	final Map<Id<Person>,Double> lastDepartureTime = new HashMap<>();
	double overallTT = 0.0;
	double overallDepartures = 0.0;
	/**
	 * 
	 */
	public ParkTravelTimeHandler(Set<Id<Link>> monitoredLinks) {
		this.monitoredLinks = monitoredLinks;
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)){
			this.carDepartures.add(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityStartEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityStartEvent)
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!event.getActType().contains("interaction")){
			if (this.carDepartures.contains(event.getPersonId())){
				if ((this.monitoredLinks.contains(event.getLinkId())|(this.monitoredLinks.contains(this.lastDepartureLocation.get(event.getPersonId()))))){
					double travelTime = event.getTime()-this.lastDepartureTime.get(event.getPersonId());
					overallTT+=travelTime;
					overallDepartures++;
				}
			}
			
			this.carDepartures.remove(event.getPersonId());
			this.lastDepartureLocation.remove(event.getPersonId());
			this.lastDepartureTime.remove(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!event.getActType().contains("interaction")){
			this.lastDepartureLocation.put(event.getPersonId(), event.getLinkId());
			this.lastDepartureTime.put(event.getPersonId(), event.getTime());
		}
		
	}
	
	public void printResults(){
		System.out.println("Overall departures/arrivals in area: " + this.overallDepartures);
		System.out.println("Average car travel time for area: " + Time.writeTime( this.overallTT/overallDepartures));
	}
} 
