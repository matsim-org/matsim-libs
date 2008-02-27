/* *********************************************************************** *
 * project: org.matsim.*
 * CarAvail.java
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

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class CarAvail extends PersonAlgorithm {
	private int never_hasCar, always_hasCar, sometimes_hasCar,
			never_but_car_plan, never_but_pt_plan, always_but_car_plan,
			always_but_pt_plan, sometimes_but_car_plan, sometimes_but_pt_plan;

	/**
	 * 
	 */
	public CarAvail() {
		never_but_car_plan = 0;
		never_but_pt_plan = 0;
		never_hasCar = 0;
		always_but_car_plan = 0;
		always_but_pt_plan = 0;
		always_hasCar = 0;
		sometimes_but_car_plan = 0;
		sometimes_but_pt_plan = 0;
		sometimes_hasCar = 0;
	}

	public void write(String outputFilename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFilename);
			writer.write("\tpersons\tcar-plans\tpt-plans\nnever\t"
					+ never_hasCar + "\t" + never_but_car_plan + "\t"
					+ never_but_pt_plan + "\nalways\t" + always_hasCar + "\t"
					+ always_but_car_plan + "\t" + always_but_pt_plan
					+ "\nsometimes\t" + sometimes_hasCar + "\t"
					+ sometimes_but_car_plan + "\t" + sometimes_but_pt_plan
					+ "\n");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(Person person) {
		String carAvail = person.getCarAvail();
		if (carAvail != null) {
			String planType = person.getSelectedPlan().getType();
			if (planType != null) {
				if (carAvail.equals("never")) {
					never_hasCar++;
					if (planType.equals("car")) {
						never_but_car_plan++;
					} else if (planType.equals("pt")) {
						never_but_pt_plan++;
					}
				} else if (carAvail.equals("always")) {
					always_hasCar++;
					if (planType.equals("pt")) {
						always_but_pt_plan++;
					} else if (planType.equals("car")) {
						always_but_car_plan++;
					}
				} else if (carAvail.equals("sometimes")) {
					sometimes_hasCar++;
					if (planType.equals("pt")) {
						sometimes_but_pt_plan++;
					} else if (planType.equals("car")) {
						sometimes_but_car_plan++;
					}
				}
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/run271/ITERS/it.100/100.plans.xml.gz";
		final String outputFilename = "../data/ivtch/run271/CarAvail.txt";

		@SuppressWarnings("unused")
		Config config = Gbl.createConfig(null);

		World world = Gbl.getWorld();

		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		CarAvail ca = new CarAvail();
		population.addAlgorithm(ca);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);
		world.setPopulation(population);

		population.runAlgorithms();

		ca.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
