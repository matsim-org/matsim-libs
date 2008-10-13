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

import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.AgentDepartureEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class CarDepartureCounter implements AgentDepartureEventHandler {
	private Population ppl;

	private int cdc = 0;

	public CarDepartureCounter(Population ppl) {
		this.ppl = ppl;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.plans.xml.gz";
		final String eventsFilename = "../data/ivtch/carPt_opt_run266/ITERS/it.100/100.events.txt.gz";
		final String outputFilename = "../data/ivtch/....carDeparture.txt";

		Gbl.createConfig(null);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		Gbl.getWorld().setNetworkLayer(network);
		Population ppl = new Population();

		System.out.println("->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(ppl).readFile(plansFilename);

		Events events = new Events();
		CarDepartureCounter cdc = new CarDepartureCounter(ppl);
		events.addHandler(cdc);
		System.out.println("-> reading eventsfile: " + eventsFilename);
		new MatsimEventsReader(events).readFile(eventsFilename);

		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outputFilename);
			out.write("network :\t" + netFilename + "\n");
			out.write("plansfile :\t" + plansFilename + "\n");
			out.write("events :\t" + eventsFilename + "\n");
			out.write("car departure :\t" + cdc.getCdc() + "\n");
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

	public void handleEvent(AgentDepartureEvent event) {
		Person p = ppl.getPerson(event.agentId);
		if (PlanModeJudger.useCar(p.getSelectedPlan()))
			cdc++;
	}

	public void reset(int iteration) {
		cdc = 0;
	}

	public int getCdc() {
		return cdc;
	}

}
