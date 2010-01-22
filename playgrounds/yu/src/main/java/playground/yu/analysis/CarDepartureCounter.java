/* *********************************************************************** *
 * project: org.matsim.*
 * CarTripCounter.java
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
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class CarDepartureCounter implements AgentDepartureEventHandler,
		AgentArrivalEventHandler {
	private final PopulationImpl ppl;

	private int cdc = 0, cac = 0;

	public int getCac() {
		return this.cac;
	}

	public CarDepartureCounter(final PopulationImpl ppl) {
		this.ppl = ppl;
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs/run628/it.500/500.plans.xml.gz";
		final String eventsFilename = "../runs/run628/it.500/500.events.txt.gz";
		final String outputFilename = "../runs/run628/it.500/500.carDeparture.txt";

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		System.out.println("->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		EventsManagerImpl events = new EventsManagerImpl();
		CarDepartureCounter cdc = new CarDepartureCounter(scenario.getPopulation());
		events.addHandler(cdc);
		System.out.println("-> reading eventsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			out.write("network :\t" + netFilename + "\n");
			out.write("plansfile :\t" + plansFilename + "\n");
			out.write("events :\t" + eventsFilename + "\n");
			out.write("car departure :\t" + cdc.getCdc() + "\n");
			out.write("car arrival :\t" + cdc.getCac() + "\n");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("-> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	public void handleEvent(final AgentDepartureEvent event) {
		Person p = this.ppl.getPersons().get(event.getPersonId());
		if (PlanModeJudger.useCar(p.getSelectedPlan()))
			this.cdc++;
	}

	public void reset(final int iteration) {
		this.cdc = 0;
		this.cac = 0;
	}

	public int getCdc() {
		return this.cdc;
	}

	public void handleEvent(final AgentArrivalEvent event) {
		Person p = this.ppl.getPersons().get(event.getPersonId());
		if (PlanModeJudger.useCar(p.getSelectedPlan()))
			this.cac++;

	}

}
