/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.io.IOUtils;

/**
 * Creates a table containing the comparison of two plans files
 *
 * @author anhorni
 */
public class CompareSelectedPlansTable {

	private Plans plans0;
	private Plans plans1;
	private final String header="personid\tsex\tage\tlicense\tcaravail\t" +
			"employed\thomex\thomey\thomelink\tscore0\tscore1\tplantraveltime0\tplantraveltime1\t" +
			"plantraveldistance0\tplantraveldistance1\tnumberoftrips0\tnumberoftrips1";
	private NetworkLayer network;



	/**
	 * @param args array with 4 entries:
	 * {path to plans file 0, path to plans file 1, name of output file, path to network file}
	 */
	public static void main(final String[] args) {

		if (args.length < 4) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}

		Gbl.startMeasurement();
		final CompareSelectedPlansTable table=new CompareSelectedPlansTable();
		table.run(args[0], args[1], args[2], args[3]);

		Gbl.printElapsedTime();
	}

	private void init(final String networkPath) {
		this.plans0=new Plans(false);
		this.plans1=new Plans(false);

		System.out.println("  reading the network...");
		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkPath);
	}

	private void readFiles(final String plansfilePath0, final String plansfilePath1) {
		System.out.println("  reading file "+plansfilePath0);
		final PlansReaderI plansReader0 = new MatsimPlansReader(this.plans0);
		plansReader0.readFile(plansfilePath0);

		System.out.println("  reading file "+plansfilePath1);
		final PlansReaderI plansReader1 = new MatsimPlansReader(this.plans1);
		plansReader1.readFile(plansfilePath1);
	}

	private void writeSummaryFile(final String outfile) {
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			out.write(this.header);
			out.newLine();

			for (final Id person_id : this.plans0.getPersons().keySet()) {

				// method person.toString() not appropriate
				out.write(person_id.toString()+"\t");
				final Person person=this.plans0.getPerson(person_id);
				out.write(person.getSex()+"\t");
				out.write(person.getAge()+"\t");
				out.write(person.getLicense()+"\t");
				out.write(person.getCarAvail()+"\t");
				out.write(person.getEmployed()+"\t");


				if (person.getSelectedPlan().getFirstActivity().getType().substring(0,1).equals("h")) {
					out.write(person.getSelectedPlan().getFirstActivity().getCoord().getX()+"\t");
					out.write(person.getSelectedPlan().getFirstActivity().getCoord().getY()+"\t");
					out.write(person.getSelectedPlan().getFirstActivity().getLinkId()+"\t");
				}
				else {
					// no home activity in the plan -> no home activity in the knowledge
					out.write("-\t-\t-\t");
				}

				out.write(person.getSelectedPlan().getScore()+"\t");

				final Person person_comp=this.plans1.getPerson(person_id);
				out.write(person_comp.getSelectedPlan().getScore()+"\t");

				out.write(this.getTravelTime(person)+"\t");
				out.write(this.getTravelTime(person_comp)+"\t");

				out.write(this.getTravelDist(person)+"\t");
				out.write(this.getTravelDist(person_comp)+"\t");

				out.write(this.getNumberOfTrips(person)+"\t");
				// TODO: using newline(). But still a tab is necessary (!?) Correct that later.
				out.write(this.getNumberOfTrips(person_comp)+"\t");

				out.newLine();
				out.flush();
			}
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}

	/*  TODO: Put the next three methods into a "stats" class and use
	 *  traveltime, numberOfTrips and traveldist as attributes.
	 *  At the moment it is "nicer" to have everything in one single class */

	private double getTravelTime(final Person person) {

		double travelTime=0.0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			travelTime+=leg.getTravTime();
		}
		return travelTime;
	}

	private double getTravelDist(final Person person) {

		double travelDist=0.0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();

		while (leg_it.hasNext()) {
			final Leg leg = (Leg)leg_it.next();
			travelDist+=leg.getRoute().getDist();
		}
		return travelDist;
	}

	private int getNumberOfTrips(final Person person) {

		int numberOfLegs=0;
		final LegIterator leg_it = person.getSelectedPlan().getIteratorLeg();
		while (leg_it.hasNext()) {
			leg_it.next();
			numberOfLegs++;
		}
		return numberOfLegs;
	}

	// --------------------------------------------------------------------------

	private void run(final String plansfilePath0, final String plansfilePath1,
			final String outfile, final String networkPath) {
		this.init(networkPath);
		readFiles(plansfilePath0, plansfilePath1);
		writeSummaryFile(outfile);
		System.out.println("finished");
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("CompareSelectedPlansTable:");
		System.out.println();
		System.out.println("Creates an agent-based table including all agent \n" +
				"attributes, the selected plan score and the total travel time");
		System.out.println();
		System.out.println("usage: CompareSelectedPlansTable args");
		System.out.println(" arg 0: path to plans file 0 (required)");
		System.out.println(" arg 1: path to plans file 1 (required)");
		System.out.println(" arg 2: name of output file (required)");
		System.out.println(" arg 3: path to network file (required)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

}
