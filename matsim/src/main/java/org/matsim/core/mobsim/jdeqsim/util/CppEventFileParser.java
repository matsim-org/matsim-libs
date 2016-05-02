package org.matsim.core.mobsim.jdeqsim.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

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

//	private static ArrayList<EventLog> eventLog = null;

	public static void main(final String[] args) {
		String eventFileName = args[0];
		CppEventFileParser eventFileParser = new CppEventFileParser();
		eventFileParser.parse(eventFileName);
	}

	public void parse(final String eventFileName) {
//		CppEventFileParser.eventLog = CppEventFileParser.parseFile(eventFileName);
		// for (int i = 0; i < eventLog.size(); i++) {
		// eventLog.get(i).print();
		// }
	}

	public static ArrayList<EventLog> parseFile(final String filePath) {
		int counter = 0;
		ArrayList<EventLog> rows = new ArrayList<EventLog>();
		BufferedReader br = null;
		try {
			br = IOUtils.getBufferedReader(filePath);
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
			System.out.println(ex);
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

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
