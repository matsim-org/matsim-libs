/* *********************************************************************** *
 * project: org.matsim.*
 * Demography2QGIS.java
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
package playground.yu.utils.qgis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.utils.io.IOUtils;

/**
 * this class quote codes from <class>org.matsim.run.CompareSelectedPlansTable</class>
 * offered a simplified function
 * 
 * @author ychen
 * 
 */
public class Demography2QGIS {
	private Population plans;
	private String header = "personId;sex;age;license;caravail;employed;homex;homey;homelink;"
			+ "planType;planScore;departureTime;"
			+ "planTravelTime;planTravelDistance;numberOfTrips";
	private NetworkLayer network;

	/**
	 * @param args
	 *            array with 3 entries: {path to network file, path to plans
	 *            file, name of output file}
	 */
	public static void main(String[] args) {

		if (args.length < 3) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}

		Gbl.startMeasurement();

		new Demography2QGIS().run(args[0], args[1], args[2]);

		Gbl.printElapsedTime();
	}

	private void init(String networkPath) {
		this.plans = new Population(false);

		System.out.println("  reading the network...");
		this.network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(this.network).readFile(networkPath);
	}

	private void readFiles(String plansfilePath) {
		System.out.println("  reading file " + plansfilePath);
		PopulationReader plansReader0 = new MatsimPopulationReader(this.plans);
		plansReader0.readFile(plansfilePath);
	}

	private void writeSummaryFile(String outfile) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(this.header);
			out.newLine();

			for (Id person_id : this.plans.getPersons().keySet()) {

				// method person.toString() not appropriate
				out.write(person_id.toString() + ";");
				Person person = this.plans.getPerson(person_id);
				out.write(person.getSex() + ";");
				out.write(person.getAge() + ";");
				out.write(person.getLicense() + ";");
				out.write(person.getCarAvail() + ";");
				out.write(person.getEmployed() + ";");

				Plan sp = person.getSelectedPlan();
				Act fa = sp.getFirstActivity();
				if (fa.getType().substring(0, 1).equals("h")) {
					out.write(fa.getCoord().getX() + ";");
					out.write(fa.getCoord().getY() + ";");
					out.write(fa.getLinkId() + ";");
				} else {
					// no home activity in the plan -> no home activity in the
					// knowledge
					out.write("-;-;-;");
				}

				out.write(sp.getType().toString() + ";" + sp.getScore() + ";"
						+ fa.getEndTime() + ";");
				out.write(getTravelTime(person) + ";");
				out.write(getTravelDist(person) + ";");
				out.write(getNumberOfTrips(person));
				out.newLine();
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	/*
	 * TODO: Put the next three methods into a "stats" class and use traveltime,
	 * numberOfTrips and traveldist as attributes. At the moment it is "nicer"
	 * to have everything in one single class
	 */

	private double getTravelTime(Person person) {

		double travelTime = 0.0;
		LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			Leg leg = (Leg) leg_it.next();
			travelTime += leg.getTravTime();
		}
		return travelTime;
	}

	private double getTravelDist(Person person) {

		double travelDist = 0.0;
		LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();

		while (leg_it.hasNext()) {
			Leg leg = (Leg) leg_it.next();
			travelDist += leg.getRoute().getDist();
		}
		return travelDist;
	}

	private int getNumberOfTrips(Person person) {

		int numberOfLegs = 0;
		LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			leg_it.next();
			numberOfLegs++;
		}
		return numberOfLegs;
	}

	// --------------------------------------------------------------------------

	private void run(String networkPath, String plansfilePath, String outfile) {
		this.init(networkPath);
		readFiles(plansfilePath);
		writeSummaryFile(outfile);
		System.out.println("finished");
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("CompareSelectedPlansTable:");
		System.out.println();
		System.out
				.println("Creates an agent-based table including all agent \n"
						+ "attributes, the selected plan score and the total travel time");
		System.out.println();
		System.out.println("usage: Demography2QGIS args");
		System.out.println(" arg 0: path to network file (required)");
		System.out.println(" arg 1: path to plans file (required)");
		System.out.println(" arg 2: name of output file (required)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

}
