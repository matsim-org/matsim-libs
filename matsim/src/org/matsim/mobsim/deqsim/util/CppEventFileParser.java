package org.matsim.mobsim.deqsim.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.PersonEvent;
import org.matsim.mobsim.deqsim.EventLog;
import org.matsim.mobsim.deqsim.SimulationParameters;

public class CppEventFileParser {

	private static ArrayList<EventLog> eventLog = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventFileName = args[0];
		CppEventFileParser eventFileParser = new CppEventFileParser();
		eventFileParser.parse(eventFileName);
	}

	public void parse(String eventFileName) {
		setEventLog (CppEventFileParser.parseFile(eventFileName));
		for (int i = 0; i < eventLog.size(); i++) {
			// eventLog.get(i).print();
		}
	}

	public static ArrayList<EventLog> parseFile(String filePath) {
		int counter = 0;
		ArrayList<EventLog> rows = new ArrayList<EventLog>();
		try {
			FileReader fr = new FileReader(filePath);
			BufferedReader br = new BufferedReader(fr);
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
					EventLog eventLog = new EventLog(first, second, third, fourth, fifth, sixth, eventType);
					rows.add(eventLog);
				}

				line = br.readLine();
			}
			br.close();
		} catch (IOException ex) {
			System.out.println(ex);
		}

		return rows;
	}

	/*
	 * Compares events produced by java and by C++ simulation
	 */
	public static boolean equals(PersonEvent personEvent, EventLog deqSimEvent) {
		if (Integer.parseInt(personEvent.agentId) != deqSimEvent.getVehicleId()) {
			CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
			return false;
		}
		if (personEvent.time != deqSimEvent.getTime()) {
			CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
			return false;
		}

		if (personEvent instanceof AgentDepartureEvent) {
			if (Integer.parseInt(((AgentDepartureEvent) personEvent).linkId) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.START_LEG)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkEnterEvent) {
			if (Integer.parseInt(((LinkEnterEvent) personEvent).linkId) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.ENTER_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof LinkLeaveEvent) {
			if (Integer.parseInt(((LinkLeaveEvent) personEvent).linkId) != deqSimEvent.getLinkId()) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}

			if (!deqSimEvent.getType().equalsIgnoreCase(SimulationParameters.LEAVE_LINK)) {
				CppEventFileParser.printNotEqualEvents(personEvent, deqSimEvent);
				return false;
			}
		}

		if (personEvent instanceof AgentArrivalEvent) {
			if (Integer.parseInt(((AgentArrivalEvent) personEvent).linkId) != deqSimEvent.getLinkId()) {
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

	private static void printNotEqualEvents(PersonEvent personEvent, EventLog deqSimEvent) {
		System.out.println("POSSIBLE PROBLEM: EVENTS NOT EQUAL");
		System.out.println(personEvent.toString());
		deqSimEvent.print();
	}

	public static ArrayList<EventLog> getEventLog() {
		return eventLog;
	}

	public static void setEventLog(ArrayList<EventLog> eventLog) {
		CppEventFileParser.eventLog = eventLog;
	}

}
