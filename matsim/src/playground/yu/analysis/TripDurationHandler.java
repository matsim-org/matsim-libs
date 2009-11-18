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

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class TripDurationHandler implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	// private final NetworkLayer network;

	private final PopulationImpl plans;

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
			final PopulationImpl plans) {
		// this.network = network;
		this.plans = plans;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		this.tmpDptTimes.put(event.getPersonId().toString(), event.getTime());
	}

	public void handleEvent(final AgentArrivalEvent event) {
		double time = event.getTime();
		String agentId = event.getPersonId().toString();
		Double dptTime = this.tmpDptTimes.get(agentId);
		if (dptTime != null) {
			double travelTime = time - dptTime;
			this.travelTimes += travelTime;
			this.arrCount++;
			this.tmpDptTimes.remove(agentId);

			// Type planType = event.agent.getSelectedPlan().getType();d
			Plan selectedPlan = plans.getPersons().get(
					new IdImpl(event.getPersonId().toString()))
					.getSelectedPlan();
			if (
			// planType != null && Plan.Type.UNDEFINED != planType
			!PlanModeJudger.useUndefined(selectedPlan)) {
				if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan)) {
					this.carTravelTimes += travelTime;
					this.carArrCount++;
				} else if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan)) {
					this.ptTravelTimes += travelTime;
					this.ptArrCount++;
				}
			} else {
				this.otherTravelTimes += travelTime;
				this.otherArrCount++;
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
		return "\ttrips\tcar trips\tpt trips\tother trips\nnumber\t"
				+ this.arrCount + "\t" + this.carArrCount + "\t"
				+ this.ptArrCount + "\t" + this.otherArrCount
				+ "\nTripDuration (sum, s)\t" + this.travelTimes + "\t"
				+ this.carTravelTimes + "\t" + this.ptTravelTimes + "\t"
				+ this.otherTravelTimes + "\nTripDuration (avg., s)\t"
				+ this.travelTimes / this.arrCount + "\t"
				+ this.carTravelTimes / this.carArrCount + "\t"
				+ this.ptTravelTimes / this.ptArrCount + "\t"
				+ this.otherTravelTimes / this.otherArrCount;
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

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();
		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population, network).readFile(plansFilename);

		EventsManagerImpl events = new EventsManagerImpl();

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
