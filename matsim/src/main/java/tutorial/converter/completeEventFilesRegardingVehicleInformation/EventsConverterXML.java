/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package tutorial.converter.completeEventFilesRegardingVehicleInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;

/**
 * This class reads and completes event files regarding vehicle ids.
 * 
 * If the Wait2LinkEvents contain vehicle ids different from null, this ids are added to the LinkEnter- and LinkLeaveEvents.
 * If not, the person id is used as vehicle id in Wait2Link-, LinkLeave- and LinkEnterEvents.
 * 
 * Additionally, this class adds VehicleLeavesTrafficEvents if they are not existing.
 * 
 * @author tthunig
 *
 */
public class EventsConverterXML extends MatsimXmlParser{

	private static final String EVENT = "event";
	private static final String ATTRIBUTE_PERSON = "person";

	private final EventsManager events;
	private final EventsReaderXMLv1 basicEventsReader;
	private boolean containsVehicleLeavesTrafficEvents = false;
	private Map<Id<Person>, Id<Vehicle>> driverToVeh = new HashMap<>();
	private Map<Id<Person>, String> personToLegMode = new HashMap<>();

	public EventsConverterXML(final EventsManager events) {
		this.events = events;
		this.setValidating(false);// events-files have no DTD, thus they cannot validate
		this.basicEventsReader = new EventsReaderXMLv1(events);
	}

	/**
	 * Parses the specified events file. This method calls {@link #parse(String)}, but handles all
	 * possible exceptions on its own.
	 *
	 * @param filename The name of the file to parse.
	 * @throws UncheckedIOException
	 */
	public void readFile(final String filename) throws UncheckedIOException {
		parse(filename);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if ( EVENT.equals(name)) {
			// (we are essentially ignoring the xml header)
			
			double time = Double.parseDouble(atts.getValue("time"));
			String eventType = atts.getValue("type");

			switch (eventType){
			case PersonDepartureEvent.EVENT_TYPE:
				// remember person to (leg) mode relation
				personToLegMode.put(Id.createPersonId(atts.getValue(PersonDepartureEvent.ATTRIBUTE_PERSON)), atts.getValue(PersonDepartureEvent.ATTRIBUTE_LEGMODE));
				break;
			case VehicleEntersTrafficEvent.EVENT_TYPE:
			case "wait2link":
				// (assumes that the reader has already converted the wait2link events)
				Id<Person> driverId = Id.createPersonId(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_DRIVER));
				Id<Vehicle> vehicleId;
				if (atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE) == null || atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE).equals("null")){
					// use the person id as vehicle id
					vehicleId = Id.create(driverId, Vehicle.class);
				} else {
					vehicleId = Id.create(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
				}
				assert vehicleId != null;

				String networkMode = atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_NETWORKMODE);
				// use the (leg) mode from the departure event if this event does not contain a (network) mode
				if (networkMode == null){
					networkMode = personToLegMode.get(driverId);
				}
				
				// remember driver to vehicle relation
				driverToVeh.put(driverId, vehicleId);				
				// process event with correct vehicleId
				this.events.processEvent(new VehicleEntersTrafficEvent(time, driverId, Id.createLinkId(atts.getValue(VehicleEntersTrafficEvent.ATTRIBUTE_LINK)), 
						vehicleId, networkMode, 1.0));
				break;
			case LinkEnterEvent.EVENT_TYPE:
				if (atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE) == null || atts.getValue(LinkEnterEvent.ATTRIBUTE_VEHICLE).equals("null")){
					// create an link enter event with the correct vehicle id
					this.events.processEvent(new LinkEnterEvent(time, driverToVeh.get(Id.createPersonId(atts.getValue(ATTRIBUTE_PERSON))), 
							Id.create(atts.getValue(LinkEnterEvent.ATTRIBUTE_LINK), Link.class)));
				} else {
					// the event already contains the vehicle id and can be processed normally
					this.basicEventsReader.startTag(name, atts, context);
				}
				break;
			case LinkLeaveEvent.EVENT_TYPE:
				if (atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE) == null || atts.getValue(LinkLeaveEvent.ATTRIBUTE_VEHICLE).equals("null")){
					// create an link leave event with the correct vehicle id
					final Id<Person> personId = Id.createPersonId(atts.getValue(ATTRIBUTE_PERSON));
					assert personId != null;
					final Id<Link> linkId = Id.create(atts.getValue(LinkLeaveEvent.ATTRIBUTE_LINK), Link.class);
					assert linkId != null;
					vehicleId = driverToVeh.get(personId);
					assert vehicleId != null;
					this.events.processEvent(new LinkLeaveEvent(time, vehicleId, linkId));
				} else {
					// the event already contains the vehicle id and can be processed normally
					this.basicEventsReader.startTag(name, atts, context);
				}
				break;
			case VehicleLeavesTrafficEvent.EVENT_TYPE:
				this.containsVehicleLeavesTrafficEvents  = true;
				this.basicEventsReader.startTag(name, atts, context);
				break;
			case PersonLeavesVehicleEvent.EVENT_TYPE:
				if (driverToVeh.containsKey(Id.createPersonId(atts.getValue(PersonLeavesVehicleEvent.ATTRIBUTE_PERSON))) && !this.containsVehicleLeavesTrafficEvents){
					/* if the person is a driver and the vehicle leaves traffic event is missing ...
					 * ... process the event later (during person arrival) to be able to collect 
					 * some information about the missing vehicle leaves traffic event before */
				} else {
					// process normally
					this.basicEventsReader.startTag(name, atts, context);
				}
				break;
			case PersonArrivalEvent.EVENT_TYPE:
				Id<Person> personId = Id.createPersonId(atts.getValue(PersonArrivalEvent.ATTRIBUTE_PERSON));
				Id<Link> linkId = Id.createLinkId(atts.getValue(PersonArrivalEvent.ATTRIBUTE_LINK));
				String mode = atts.getValue(PersonArrivalEvent.ATTRIBUTE_LEGMODE);

				// create a vehicle leaves traffic event if it is missing
				if (driverToVeh.containsKey(personId) && !this.containsVehicleLeavesTrafficEvents){
					Id<Vehicle> vehicleIdOfDriver = driverToVeh.get(personId);

					this.events.processEvent(new VehicleLeavesTrafficEvent(time, personId, linkId, vehicleIdOfDriver, mode, 1.0));
					this.events.processEvent(new PersonLeavesVehicleEvent(time, personId, vehicleIdOfDriver));
				}
				// process the PersonArrivalEvent normally, independently of missing VehicleLeavesTrafficEvents
				this.basicEventsReader.startTag(name, atts, context);
				break;
			default:
				this.basicEventsReader.startTag(name, atts, context);
				break;
			}
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

}
