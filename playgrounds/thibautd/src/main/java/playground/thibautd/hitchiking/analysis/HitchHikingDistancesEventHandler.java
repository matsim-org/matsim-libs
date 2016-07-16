/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingDistancesEventHandler.java
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
package playground.thibautd.hitchiking.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.hitchiking.qsim.events.PassengerDepartsWithDriverEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author thibautd
 */
public class HitchHikingDistancesEventHandler implements BasicEventHandler {
	private final Network network;
	private final BufferedWriter writer;
	private final Map<Id, Distance> tripDistances = new HashMap<Id, Distance>();

	public HitchHikingDistancesEventHandler(
			final Network n,
			final String outfile) {
		this.network = n;
		this.writer = IOUtils.getBufferedWriter( outfile );

		try {
			writer.write( "agentType\tagentId\tdistance" );
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	@Override
	public void reset(int iteration) {
		// just used for a posteriori analysis: do nothing
	}

	@Override
	public void handleEvent(final Event event) {
		if (event.getEventType().equals( PassengerDepartsWithDriverEvent.TYPE )) {
			handlePassengerEvent( event );
		}
		else if (event instanceof LinkEnterEvent) {
			handleLinkEvent( (LinkEnterEvent) event );
		}
		else if (event instanceof PersonArrivalEvent) {
			handleArrivalEvent( (PersonArrivalEvent) event );
		}
	}

	private void handlePassengerEvent(final Event event) {
		Id<Person> driver = Id.create( event.getAttributes().get( PassengerDepartsWithDriverEvent.ATTRIBUTE_DRIVER ) , Person.class );
		Id<Person> passenger = Id.create( event.getAttributes().get( PassengerDepartsWithDriverEvent.ATTRIBUTE_PERSON ) , Person.class);

		Distance val = tripDistances.get( driver );

		if (val == null) {
			val = new Distance();
			tripDistances.put( driver , val );
		}

		val.passengers.add( passenger );
	}

	private void handleLinkEvent(final LinkEnterEvent event) {
		Distance dist = tripDistances.get( event.getVehicleId() );
		if (dist == null) return;

		dist.distance += network.getLinks().get( event.getLinkId() ).getLength();
	}

	private void handleArrivalEvent(final PersonArrivalEvent event) {
		Distance dist = tripDistances.remove( event.getPersonId() );
		if (dist == null) return;

		try {
			writer.newLine();
			writer.write( "driver\t"+event.getPersonId()+"\t"+dist.distance );

			for (Id p : dist.passengers) {
				writer.newLine();
				writer.write( "passenger\t"+p+"\t"+dist.distance );
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}

	}

	private static class Distance {
		final List<Id> passengers = new ArrayList<Id>();
		double distance = 0;
	}
}
