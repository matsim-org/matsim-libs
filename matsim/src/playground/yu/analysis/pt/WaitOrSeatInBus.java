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

import org.jfree.chart.plot.PlotOrientation;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import playground.yu.utils.charts.StackedBarChart;
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
	private Map<Id, Map<Integer, Double>> WtobSumsOfVeh = new HashMap<Id, Map<Integer, Double>>();

	private Map<Integer, Integer> wtobCounts = new TreeMap<Integer, Integer>(),
			wibCounts = new TreeMap<Integer, Integer>();
	private Map<Id, Map<Integer, Integer>> WtobCountsOfVeh = new HashMap<Id, Map<Integer, Integer>>();

	/** saves the newest departure times of every agent/person */
	private Map<Id, Double> departures = new TreeMap<Id, Double>();
	private int binSize;
	/** Map<vehId,List<enterVehTime>> */
	private Map<Id, List<Tuple<Double, Integer>>> enterVehTimes = new HashMap<Id, List<Tuple<Double, Integer>>>();

	// private Map<Id, Integer> vehIdTimeBins = new HashMap<Id, Integer>();

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

		List<Tuple<Double, Integer>> enterVehTimeList = enterVehTimes
				.get(vehId);
		if (enterVehTimeList == null)
			enterVehTimeList = new ArrayList<Tuple<Double, Integer>>();

		try {
			double arrTimeAtStop = departures.remove(perId)/*
															 * departureTime/arrivalTimeAtBusStop
															 */;

			int timeBin = (int) arrTimeAtStop / this.binSize;
			enterVehTimeList.add(new Tuple<Double, Integer>(enterVehTime,
					timeBin));
			enterVehTimes.put(vehId, enterVehTimeList);

			double waitTimeOutOfBus = enterVehTime - arrTimeAtStop;

			// vehIdTimeBins.put(vehId, timeBin);

			// Double wtobSum = wtobSums.get(timeBin);
			// if (wtobSum == null)
			// wtobSum = 0.0;
			// wtobSums.put(timeBin, waitTimeOutOfBus + wtobSum);

			Map<Integer, Double> tmpWtobSums = WtobSumsOfVeh.get(vehId);
			if (tmpWtobSums == null)
				tmpWtobSums = new TreeMap<Integer, Double>();
			Double tmpWtobSum = tmpWtobSums.get(timeBin);
			if (tmpWtobSum == null)
				tmpWtobSum = 0.0;
			tmpWtobSums.put(timeBin, waitTimeOutOfBus + tmpWtobSum);
			WtobSumsOfVeh.put(vehId, tmpWtobSums);

			// Integer wtobCount = wtobCounts.get(timeBin);
			// if (wtobCount == null)
			// wtobCount = 0;
			// wtobCounts.put(timeBin, ++wtobCount);

			Map<Integer, Integer> tmpWtobCounts = WtobCountsOfVeh.get(vehId);
			if (tmpWtobCounts == null)
				tmpWtobCounts = new TreeMap<Integer, Integer>();
			Integer tmpWtobCount = tmpWtobCounts.get(timeBin);
			if (tmpWtobCount == null)
				tmpWtobCount = 0;
			tmpWtobCounts.put(timeBin, ++tmpWtobCount);
			WtobCountsOfVeh.put(vehId, tmpWtobCounts);

			// System.out.println(">>>>>wtobCounts put\ttimeBin\t" + timeBin
			// + "\twtobCount\t" + wtobCount);

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
		if (event.getFacilityId().toString().equals("812550.1")) {
			System.out.println("facility Id:\t"
					+ event.getFacilityId().toString());
			Id vehId = event.getVehicleId();
			double time = event.getTime();

			List<Tuple<Double, Integer>> enterVehTimeList = enterVehTimes
					.remove(vehId);
			if (enterVehTimeList != null) {
				int size = enterVehTimeList.size();
				int tmpTimeBin = -1;
				for (Tuple<Double, Integer> enterTimeBinTuple : enterVehTimeList) {
					int timeBin = enterTimeBinTuple.getSecond();
					double waitTimeInBus = time - enterTimeBinTuple.getFirst();

					if (tmpTimeBin != timeBin) {
						Map<Integer, Double> tmpWtobSums = WtobSumsOfVeh
								.get(vehId);
						Map<Integer, Integer> tmpWtobCounts = WtobCountsOfVeh
								.get(vehId);

						Double wtobSum = wtobSums.get(timeBin);
						if (wtobSum == null)
							wtobSum = 0.0;
						wtobSums.put(timeBin, tmpWtobSums.remove(timeBin)
								+ wtobSum);

						Integer wtobCount = wtobCounts.get(timeBin);
						if (wtobCount == null)
							wtobCount = 0;
						wtobCounts.put(timeBin, tmpWtobCounts.remove(timeBin)
								+ wtobCount);
					}

					Double wibSum = wibSums.get(timeBin);
					if (wibSum == null)
						wibSum = 0.0;

					Integer wibCount = wibCounts.get(timeBin);
					if (wibCount == null)
						wibCount = 0;

					wibSums.put(timeBin, wibSum + waitTimeInBus);
					// System.out.println(">>>>>wibSums put\ttimeBin\t" +
					// timeBin
					// + "\twibSums\t" + (wibSum + waitTimeInBus));

					wibCounts.put(timeBin, ++wibCount);
					// System.out.println(">>>>>wibCounts put\ttimeBin\t"
					// + timeBin + "\twibCount\t" + wibCount);
					tmpTimeBin = timeBin;
				}

				// double sumOfWaitInBus = size * time -
				// getSum(enterVehTimeList);
				// int timeBin = vehIdTimeBins.get(vehId);
			}
			WtobSumsOfVeh.remove(vehId);
			WtobCountsOfVeh.remove(vehId);
		}
	}

	public void write(String outputFilenameBase) {
		SimpleWriter writer = new SimpleWriter(outputFilenameBase + "txt");
		writer.writeln("timeBin\twaitTimeOutofBus\twaitTimeInBus");
		StackedBarChart chart = new StackedBarChart(
				"arrivalTimeAtBusStop<->WaitTimeOutOfBus or WaitTimeInBus",
				"arrivalTimeAtBusStops", "waitTime[s]",
				PlotOrientation.VERTICAL);
		Object[] bins = wtobSums.keySet().toArray();
		// System.out.println("bins length=\t" + bins.length);
		int firstBin = (Integer) bins[0], lastBin = (Integer) bins[bins.length - 1];
		String[] times = new String[lastBin - firstBin + 1];
		double[][] values = new double[2][lastBin - firstBin + 1];
		for (int i = firstBin; i <= lastBin; i++) {
			// System.out.println(">>>>> i=\t" + i);
			int wtob = (int) (wtobCounts.containsKey(i) ? wtobSums.get(i)
					/ wtobCounts.get(i) : 0);
			int wib = (int) (wibCounts.containsKey(i) ? wibSums.get(i)
					/ wibCounts.get(i) : 0);
			String time = Time.writeTime(i * binSize);
			writer.writeln(time + "\t" + Time.writeTime(wtob) + "\t"
					+ Time.writeTime(wib));
			times[i - firstBin] = time;
			values[0][i - firstBin] = wtob;
			values[1][i - firstBin] = wib;
		}
		writer.close();

		chart.addSeries(new String[] { "waitTimeOutOfBus", "waitTimeInBus" },
				times, values);
		chart.saveAsPng(outputFilenameBase + "png", 1024, 768);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String eventsFilename = "../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/m2_out_100a_opt/ITERS/it.1000/1000.events.xml.gz";

		EventsManager em = new EventsManagerImpl();
		WaitOrSeatInBus wosib = new WaitOrSeatInBus(30);
		em.addHandler(wosib);

		new MatsimEventsReader(em).readFile(eventsFilename);

		wosib
				.write("../berlin-bvg09/pt/m2_schedule_delay/m2_out_100a_opt/m2_out_100a_opt/ITERS/it.1000/m2_out_100a_opt.1000.wosib.");
	}
}
