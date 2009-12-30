package org.matsim.core.mobsim.jdeqsim.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.mobsim.jdeqsim.EventLog;
import org.matsim.core.mobsim.jdeqsim.SimulationParameters;

/**
 * This parser can read the event output files of the C++ DEQSim.
 * 
 * @author rashid_waraich
 */
public class CppEventFileParser {

	private static ArrayList<EventLog> eventLog = null;

	public static void main(final String[] args) {
		String eventFileName = args[0];
		CppEventFileParser eventFileParser = new CppEventFileParser();
		eventFileParser.parse(eventFileName);
	}

	public void parse(final String eventFileName) {
		setEventLog(CppEventFileParser.parseFile(eventFileName));
		// for (int i = 0; i < eventLog.size(); i++) {
		// eventLog.get(i).print();
		// }
	}

	public static ArrayList<EventLog> parseFile(final String filePath) {
		int counter = 0;
		ArrayList<EventLog> rows = new ArrayList<EventLog>();
		BufferedReader br = null;
		try {
			FileReader fr = new FileReader(filePath);
			br = new BufferedReader(fr);
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
					eventType = SimulationParameters.START_LEG;
				} else if (eventType.equalsIgnoreCase("end")) {
					eventType = SimulationParameters.END_LEG;
				} else if (eventType.equalsIgnoreCase("enter")) {
					eventType = SimulationParameters.ENTER_LINK;
				} else if (eventType.equalsIgnoreCase("leave")) {
					eventType = SimulationParameters.LEAVE_LINK;
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
	 */
	public static boolean equals(final PersonEvent personEvent, final EventLog deqSimEvent) {
		if (Integer.parseInt(personEvent.getPersonId().toString()) != deqSimEvent.getVehicleId()) {
			CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
			return false;
		}
		if (personEvent.getTime() != deqSimEvent.getTime()) {
			CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
			return false;
		}

		if (personEvent instanceof AgentDepartureEventImpl) {
			if (Integer.parseInt(((AgentDepartureEventImpl) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.START_LEG)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkEnterEventImpl) {
			if (Integer.parseInt(((LinkEnterEventImpl) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.ENTER_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkLeaveEventImpl) {
			if (Integer.parseInt(((LinkLeaveEventImpl) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.LEAVE_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof AgentArrivalEventImpl) {
			if (Integer.parseInt(((AgentArrivalEventImpl) personEvent).getLinkId().toString()) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.END_LEG)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		return true;
	}

	private static void printNotEqualEvents(final PersonEvent personEvent, final EventLog deqSimEvent) {
		System.out.println("POSSIBLE PROBLEM: EVENTS NOT EQUAL");
		System.out.println(personEvent.toString());
		deqSimEvent.print();
	}

	public static ArrayList<EventLog> getEventLog() {
		return eventLog;
	}

	public static void setEventLog(final ArrayList<EventLog> eventLog) {
		CppEventFileParser.eventLog = eventLog;
	}

}
