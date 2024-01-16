
/* *********************************************************************** *
 * project: org.matsim.*
 * CppEventFileParser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.jdeqsim.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.jdeqsim.EventLog;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.utils.io.IOUtils;

/**
 * This parser can read the event output files of the C++ DEQSim.
 *
 * @author rashid_waraich
 */
public class CppEventFileParser {

	private static final Logger LOG = LogManager.getLogger(CppEventFileParser.class);

	public static ArrayList<EventLog> parseFile(final String filePath) {
		int counter = 0;
		ArrayList<EventLog> rows = new ArrayList<>();
		try (BufferedReader br = IOUtils.getBufferedReader(filePath)) {
			String line = null;
			StringTokenizer tokenizer = null;
			line = br.readLine();
			while (line != null) {
				counter++;

				if (counter % 1000000 == 0) {
					System.out.println("noOfLines Read:" + counter);
				}

				tokenizer = new StringTokenizer(line);
				String token = null;
				token = tokenizer.nextToken();
				double first = Double.parseDouble(token);
				token = tokenizer.nextToken();
				int second = Integer.parseInt(token);
				token = tokenizer.nextToken();
				int third = Integer.parseInt(token);
				token = tokenizer.nextToken();
				int fourth = Integer.parseInt(token);
				token = tokenizer.nextToken();
				int fifth = Integer.parseInt(token);
				token = tokenizer.nextToken();
				int sixth = Integer.parseInt(token);
				token = tokenizer.nextToken();
				String eventType = token;

				// there is one eventType called 'enter net' => it is split into
				// two tockens, so need to take that into account
				if (tokenizer.hasMoreTokens()) {
					token = tokenizer.nextToken();
					eventType += " " + token;
				}

				// change type label
				if (eventType.equalsIgnoreCase("starting")) {
					eventType = JDEQSimConfigGroup.START_LEG;
				} else if (eventType.equalsIgnoreCase("end")) {
					eventType = JDEQSimConfigGroup.END_LEG;
				} else if (eventType.equalsIgnoreCase("enter")) {
					eventType = JDEQSimConfigGroup.ENTER_LINK;
				} else if (eventType.equalsIgnoreCase("leave")) {
					eventType = JDEQSimConfigGroup.LEAVE_LINK;
				}

				// ignore 'enter net' events (which seem useless)
				if (!eventType.equalsIgnoreCase("enter net")) {
					rows.add(new EventLog(first, second, third, fourth, fifth, sixth, eventType));
				}

				line = br.readLine();
			}
		} catch (IOException ex) {
			LOG.error("error reading events", ex);
		}

		return rows;
	}

	/**
	 * Compares events produced by java and by C++ simulation
	 * @param personId TODO
	 * @deprecated Use {@link #equals(Event,EventLog)} instead
	 */
	@Deprecated
	public static boolean equals(final Event personEvent, Id<Person> personId, final EventLog deqSimEvent) {
		return equals(personEvent, deqSimEvent);
	}

	/**
	 * Compares events produced by java and by C++ simulation
	 */
	public static boolean equals(final Event personEvent, final EventLog deqSimEvent) {

		if (personEvent.getTime() != deqSimEvent.getTime()) {
			CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
			return false;
		}

		if (personEvent instanceof PersonDepartureEvent) {
			if (Integer.parseInt(((PersonDepartureEvent) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (Integer.parseInt(((PersonDepartureEvent) personEvent).getPersonId().toString()) != deqSimEvent.getVehicleId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (!deqSimEvent.getType().equalsIgnoreCase(JDEQSimConfigGroup.START_LEG)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkEnterEvent) {
			if (Integer.parseInt(((LinkEnterEvent) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (Integer.parseInt(((LinkEnterEvent) personEvent).getVehicleId().toString()) != deqSimEvent.getVehicleId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (!deqSimEvent.getType().equalsIgnoreCase(JDEQSimConfigGroup.ENTER_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkLeaveEvent) {
			if (Integer.parseInt(((LinkLeaveEvent) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (Integer.parseInt(((LinkLeaveEvent) personEvent).getVehicleId().toString()) != deqSimEvent.getVehicleId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (!deqSimEvent.getType().equalsIgnoreCase(JDEQSimConfigGroup.LEAVE_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof PersonArrivalEvent) {
			if (Integer.parseInt(((PersonArrivalEvent) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
			if (Integer.parseInt(((PersonArrivalEvent) personEvent).getPersonId().toString()) != deqSimEvent.getVehicleId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(JDEQSimConfigGroup.END_LEG)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		return true;
	}

	private static void printNotEqualEvents(final Event personEvent, final EventLog deqSimEvent) {
		System.out.println("POSSIBLE PROBLEM: EVENTS NOT EQUAL");
		System.out.println(personEvent.toString());
		deqSimEvent.print();
	}

}
