/* *********************************************************************** *
 * project: org.matsim.*
 * WaitOrSeatInBus.java
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

/**
 * 
 */
package playground.yu.analysis.pt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 * 
 */
public class WaitOrSeatInBus implements PersonEntersVehicleEventHandler,
		AgentDepartureEventHandler, VehicleDepartsAtFacilityEventHandler {
	/** saves the waitTimes of every timeBin */
	private Map<Integer, Double> wtobSums = new TreeMap<Integer, Double>(),
			wibSums = new TreeMap<Integer, Double>();
	private Map<Integer, Integer> wtobCounts = new TreeMap<Integer, Integer>(),
			wibCounts = new TreeMap<Integer, Integer>();
	/** saves the newest departure times of every agent/person */
	private Map<Id, Double> departures = new TreeMap<Id, Double>();
	private int binSize;
	/** Map<vehId,List<enterVehTime>> */
	private Map<Id, List<Double>> enterVehTimes = new HashMap<Id, List<Double>>();
	private Map<Id, Integer> vehIdTimeBins = new HashMap<Id, Integer>();

	/**
	 * 
	 */
	public WaitOrSeatInBus(int binSize) {
		this.binSize = binSize;
	}

	public void handleEvent(PersonEntersVehicleEvent event) {
		Id perId = event.getPersonId();
		double enterVehTime/* [s] */= event.getTime();
		Id vehId = event.getVehicleId();

		List<Double> enterVehTimeList = enterVehTimes.get(vehId);
		if (enterVehTimeList == null)
			enterVehTimeList = new ArrayList<Double>();
		enterVehTimeList.add(enterVehTime);
		enterVehTimes.put(vehId, enterVehTimeList);

		try {
			double arrTimeAtStop = departures.remove(perId)/*
															 * departureTime/arrivalTimeAtBusStop
															 */;
			double waitTimeOutOfBus = enterVehTime - arrTimeAtStop;
			int timeBin = (int) arrTimeAtStop / this.binSize;

			vehIdTimeBins.put(vehId, timeBin);

			Double wtobSum = wtobSums.get(timeBin);
			if (wtobSum == null)
				wtobSum = 0.0;
			wtobSums.put(timeBin, waitTimeOutOfBus + wtobSum);
			System.out.println(">>>>>wtobSums put\ttimeBin\t" + timeBin
					+ "\twtobSums\t" + (waitTimeOutOfBus + wtobSum));

			Integer wtobCount = wtobCounts.get(timeBin);
			if (wtobCount == null)
				wtobCount = 0;
			wtobCounts.put(timeBin, ++wtobCount);
			System.out.println(">>>>>wtobCounts put\ttimeBin\t" + timeBin
					+ "\twtobCount\t" + wtobCount);

		} catch (NullPointerException npe) {
			System.err
					.println("WARN:\tthere is not the departure time record of agent:\t"
							+ perId + "\twith event\t" + event.toString());
		}
	}

	public void reset(int iteration) {

	}

	public void handleEvent(AgentDepartureEvent event) {
		Id perId = event.getPersonId();
		double time/* [s] */= event.getTime();
		if (!perId.toString().startsWith("pt"))
			departures.put(perId, time);
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		double time = event.getTime();

		List<Double> enterVehTimeList = enterVehTimes.get(vehId);
		if (enterVehTimeList != null) {
			int size = enterVehTimeList.size();
			double sumOfWaitInBus = size * time - getSum(enterVehTimeList);
			int timeBin = vehIdTimeBins.get(vehId);

			Double wibSum = wibSums.get(timeBin);
			if (wibSum == null)
				wibSum = 0.0;
			wibSums.put(timeBin, wibSum + sumOfWaitInBus);
			System.out.println(">>>>>wibSums put\ttimeBin\t" + timeBin
					+ "\twibSums\t" + (wibSum + sumOfWaitInBus));

			Integer wibCount = wibCounts.get(timeBin);
			if (wibCount == null)
				wibCount = 0;
			wibCounts.put(timeBin, wibCount + size);
			System.out.println(">>>>>wibCounts put\ttimeBin\t" + timeBin
					+ "\twibCount\t" + (size + wibCount));
		}
	}

	private static double getSum(List<Double> list) {
		double sum = 0;
		for (double d : list)
			sum += d;
		return sum;
	}

	public void write(String outputFilename) {
		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("timeBin\twaitTimeOutofBus\twaitTimeInBus");
		for (int timeBin : wtobSums.keySet()) {
			int wtob = (int) (wtobCounts.containsKey(timeBin) ? wtobSums
					.get(timeBin)
					/ wtobCounts.get(timeBin) : 0);
			int wib = (int) (wibCounts.containsKey(timeBin) ? wibSums
					.get(timeBin)
					/ wibCounts.get(timeBin) : 0);
			writer.writeln(Time.writeTime(timeBin * binSize) + "\t"
					+ Time.writeTime(wtob) + "\t" + Time.writeTime(wib));
		}
		writer.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/m2_out_2a/m2_out_2a/ITERS/it.1000/1000.events.xml.gz";

		EventsManager em = new EventsManagerImpl();
		WaitOrSeatInBus wosib = new WaitOrSeatInBus(300);
		em.addHandler(wosib);

		new MatsimEventsReader(em).readFile(eventsFilename);

		wosib
				.write("../berlin-bvg09/pt/m2_schedule_delay/m2_out_2a/m2_out_2a/ITERS/it.1000/1000.wosib.txt");
	}
}
