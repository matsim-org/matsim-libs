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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.io.IOUtils;

import playground.yu.analysis.PlanModeJudger;
import playground.yu.utils.CompareSelectedPlansTable;

/**
 * this class quote codes from
 * <class>org.matsim.run.CompareSelectedPlansTable</class> offered a simplified
 * function
 * 
 * @author ychen
 * 
 */
public class Demography2QGIS extends CompareSelectedPlansTable {
	private PopulationImpl plans;
	private static final String HEADER = "personId;sex;age;license;caravail;employed;homex;homey;homelink;"
			+ "planType;planScore;departureTime;"
			+ "planTravelTime;planTravelDistance;numberOfTrips";
	private NetworkLayer network;

	/**
	 * @param args
	 *            array with 3 entries: {path to network file, path to plans
	 *            file, name of output file}
	 */
	public static void main(final String[] args) {

		if (args.length < 3) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}

		Gbl.startMeasurement();

		new Demography2QGIS().run(args[0], args[1], args[2]);

		Gbl.printElapsedTime();
	}

	private void init(final String networkPath) {
		this.plans = new PopulationImpl();

		System.out.println("  reading the network...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(networkPath);
	}

	private void readFiles(final String plansfilePath) {
		System.out.println("  reading file " + plansfilePath);
		PopulationReader plansReader0 = new MatsimPopulationReader(this.plans,
				this.network);
		plansReader0.readFile(plansfilePath);
	}

	private void writeSummaryFile(final String outfile) {
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(Demography2QGIS.HEADER);
			out.newLine();

			for (Id person_id : this.plans.getPersons().keySet()) {

				// method person.toString() not appropriate
				out.write(person_id.toString() + ";");
				PersonImpl person = (PersonImpl) this.plans.getPersons().get(person_id);
				out.write(person.getSex() + ";");
				out.write(person.getAge() + ";");
				out.write(person.getLicense() + ";");
				out.write(person.getCarAvail() + ";");
				out.write(person.isEmployed() + ";");

				Plan sp = person.getSelectedPlan();
				ActivityImpl fa = ((PlanImpl) sp).getFirstActivity();
				if (fa.getType().substring(0, 1).equals("h")) {
					Coord coord = fa.getCoord();
					out.write(coord.getX() + ";");
					out.write(coord.getY() + ";");
					out.write(fa.getLinkId() + ";");
				} else {
					// no home activity in the plan -> no home activity in the
					// knowledge
					out.write("-;-;-;");
				}

				out.write(PlanModeJudger.getMode(sp) + ";"
						+ sp.getScore().doubleValue() + ";" + fa.getEndTime()
						+ ";");
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

	// --------------------------------------------------------------------------

	private void run(final String networkPath, final String plansfilePath,
			final String outfile) {
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
