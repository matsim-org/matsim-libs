/* *********************************************************************** *
 * project: org.matsim.*
 * TripDurationHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.events.AgentArrivalEvent;
import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.AgentArrivalEventHandler;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class TripDurationHandler implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	// private final NetworkLayer network;

	private final Population plans;

	private double travelTimes, carTravelTimes, ptTravelTimes,
			otherTravelTimes;

	private int arrCount, carArrCount, ptArrCount, otherArrCount;
	/**
	 * @param arg0
	 *            - String agentId
	 * @param arg1
	 *            - Double departure time
	 */
	private final HashMap<String, Double> tmpDptTimes = new HashMap<String, Double>();

	public TripDurationHandler(
	// final NetworkLayer network,
			final Population plans) {
		// this.network = network;
		this.plans = plans;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		tmpDptTimes.put(event.agentId, event.time);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		double time = event.time;
		String agentId = event.agentId;
		Double dptTime = tmpDptTimes.get(agentId);
		if (dptTime != null) {
			double travelTime = time - dptTime;
			travelTimes += travelTime;
			arrCount++;
			tmpDptTimes.remove(agentId);

			if (event.agent == null) {
				// rebuild event
				event.agent = this.plans.getPerson(event.agentId);
			}

			// Type planType = event.agent.getSelectedPlan().getType();d
			Plan selectedPlan = event.agent.getSelectedPlan();
			if (
			// planType != null && Plan.Type.UNDEFINED != planType
			!PlanModeJudger.useUndefined(selectedPlan)) {
				if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan)) {
					carTravelTimes += travelTime;
					carArrCount++;
				} else if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan)) {
					ptTravelTimes += travelTime;
					ptArrCount++;
				}
			} else {
				otherTravelTimes += travelTime;
				otherArrCount++;
			}
		}
	}

	public void write(final String filename) {
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(filename);
			bw.write(toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "\ttrips\tcar trips\tpt trips\tother trips\nnumber\t" + arrCount
				+ "\t" + carArrCount + "\t" + ptArrCount + "\t" + otherArrCount
				+ "\nTripDuration (sum, s)\t" + travelTimes + "\t"
				+ carTravelTimes + "\t" + ptTravelTimes + "\t"
				+ otherTravelTimes + "\nTripDuration (avg., s)\t"
				+ travelTimes / arrCount + "\t" + carTravelTimes / carArrCount
				+ "\t" + ptTravelTimes / ptArrCount + "\t" + otherTravelTimes
				/ otherArrCount;
	}

	/**
	 * @param arg0
	 *            networkfile
	 * @parma arg1 plansfile
	 * @param arg2
	 *            eventsfile
	 * @param arg3
	 *            outputFilename
	 */
	public static void main(final String[] args) {
		final String netFilename = args[0];
		final String plansFilename = args[1];
		final String eventsFilename = args[2];
		final String outFilename = args[3];

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		Events events = new Events();

		TripDurationHandler tdh = new TripDurationHandler(
		// network,
				population);
		events.addHandler(tdh);

		System.out.println("-->reading evetsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		tdh.write(outFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	public void reset(final int iteration) {

	}

}
