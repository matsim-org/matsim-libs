/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions.events;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import static org.matsim.core.events.EventsReaderXMLv1.*;


/**
 * @author benjamin
 *
 */
public final class EmissionEventsReader implements MatsimReader {

	private MatsimEventsReader delegate ;

	public EmissionEventsReader( EventsManager events ){
		this.delegate = new MatsimEventsReader( events );

		// yyyy should be possible to make these mappers available to other readers (that may want to combine event types that are not in the core).  kai, jan'19

		this.delegate.addCustomEventMapper( WarmEmissionEvent.EVENT_TYPE, (CustomEventMapper<WarmEmissionEvent>) event -> {

			Map<String, String> attributes = event.getAttributes();
			Map<String, Double> warmEmissions = new LinkedHashMap<>();

			double time = Time.getUndefinedTime();
			Id<Link> linkId = null;
			Id<Vehicle> vehicleId = null;

			// the loop is necessary since we do now know which pollutants are in the event.
			for( Map.Entry<String, String> entry : attributes.entrySet() ){

				if( "time".equals( entry.getKey() ) ){
					time = Double.parseDouble( entry.getValue() );
				} else if( "type".equals( entry.getKey() ) ){
					// I don't think that we are doing anything here. kai, jan'19
				} else if( WarmEmissionEvent.ATTRIBUTE_LINK_ID.equals( entry.getKey() ) ){
					linkId = Id.createLinkId( entry.getValue() );
				} else if( WarmEmissionEvent.ATTRIBUTE_VEHICLE_ID.equals( entry.getKey() ) ){
					vehicleId = Id.createVehicleId( entry.getValue() );
				} else{
					String pollutant = entry.getKey();
					Double value = Double.parseDouble( entry.getValue() );
					warmEmissions.put( pollutant, value );
				}
			}

			return new WarmEmissionEvent( time, linkId, vehicleId, warmEmissions );
		} );

		this.delegate.addCustomEventMapper( ColdEmissionEvent.EVENT_TYPE, (CustomEventMapper<ColdEmissionEvent>) event -> {

			Map<String, String> attributes = event.getAttributes();
			Map<String, Double> coldEmissions = new LinkedHashMap<>();

			double time = Time.getUndefinedTime();
			Id<Link> linkId = null;
			Id<Vehicle> vehicleId = null;

			// the loop is necessary since we do now know which pollutants are in the event.
			for( Map.Entry<String, String> entry : attributes.entrySet() ){

				if( "time".equals( entry.getKey() ) ){
					time = Double.parseDouble( entry.getValue() );
				} else if( "type".equals( entry.getKey() ) ){
					// do nothing
				} else if( ColdEmissionEvent.ATTRIBUTE_LINK_ID.equals( entry.getKey() ) ){
					linkId = Id.createLinkId( entry.getValue() );
				} else if( ColdEmissionEvent.ATTRIBUTE_VEHICLE_ID.equals( entry.getKey() ) ){
					vehicleId = Id.createVehicleId( entry.getValue() );
				} else{
					String pollutant = entry.getKey();
					Double value = Double.parseDouble( entry.getValue() );
					coldEmissions.put( pollutant, value );
				}
			}
			return new ColdEmissionEvent( time, linkId, vehicleId, coldEmissions );

		} );
	}

	@Override
	public void readFile( String filename ){
		delegate.readFile( filename );
	}

	@Override
	public void readURL( URL url ){
		delegate.readURL( url );
	}
}
