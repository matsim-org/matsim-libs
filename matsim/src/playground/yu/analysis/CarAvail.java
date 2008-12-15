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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author ychen
 * 
 */
public class CarAvail extends AbstractPersonAlgorithm {
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

	public void write(final String outputFilename) {
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
	public void run(final Person person) {
		String carAvail = person.getCarAvail();
		PlanModeJudger pmj = new PlanModeJudger();
		if (carAvail != null) {
			Plan selectedPlan = person.getSelectedPlan();
			// Plan.Type planType = person.getSelectedPlan().getType();
			// if (planType != null && planType != Plan.Type.UNDEFINED)

			boolean useCar = new PlanModeJudger().useCar(selectedPlan);
			if (carAvail.equals("never")) {
				never_hasCar++;
				if (
				// planTyp == Plan.Type.CAR
				PlanModeJudger.useCar(selectedPlan))
					never_but_car_plan++;
				else if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					never_but_pt_plan++;
			} else if (carAvail.equals("always")) {
				always_hasCar++;
				if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					always_but_pt_plan++;
				else if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan))
					always_but_car_plan++;
			} else if (carAvail.equals("sometimes")) {
				sometimes_hasCar++;
				if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					sometimes_but_pt_plan++;
				else if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan))
					sometimes_but_car_plan++;
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../data/ivtch/input/network.xml";
		final String plansFilename = "../data/ivtch/run271/ITERS/it.100/100.plans.xml.gz";
		final String outputFilename = "../data/ivtch/run271/CarAvail.txt";

		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);
		world.complete();

		Population population = new Population();

		CarAvail ca = new CarAvail();
		population.addAlgorithm(ca);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(population).readFile(plansFilename);

		population.runAlgorithms();

		ca.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
