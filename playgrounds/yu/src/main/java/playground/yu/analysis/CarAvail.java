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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

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
		this.never_but_car_plan = 0;
		this.never_but_pt_plan = 0;
		this.never_hasCar = 0;
		this.always_but_car_plan = 0;
		this.always_but_pt_plan = 0;
		this.always_hasCar = 0;
		this.sometimes_but_car_plan = 0;
		this.sometimes_but_pt_plan = 0;
		this.sometimes_hasCar = 0;
	}

	public void write(final String outputFilename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFilename);
			writer.write("\tpersons\tcar-plans\tpt-plans\nnever\t"
					+ this.never_hasCar + "\t" + this.never_but_car_plan + "\t"
					+ this.never_but_pt_plan + "\nalways\t"
					+ this.always_hasCar + "\t" + this.always_but_car_plan
					+ "\t" + this.always_but_pt_plan + "\nsometimes\t"
					+ this.sometimes_hasCar + "\t"
					+ this.sometimes_but_car_plan + "\t"
					+ this.sometimes_but_pt_plan + "\n");
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(final Person p) {
		PersonImpl person = (PersonImpl) p;
		String carAvail = person.getCarAvail();
		if (carAvail != null) {
			Plan selectedPlan = person.getSelectedPlan();
			if (carAvail.equals("never")) {
				this.never_hasCar++;
				if (
				// planTyp == Plan.Type.CAR
				PlanModeJudger.useCar(selectedPlan))
					this.never_but_car_plan++;
				else if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					this.never_but_pt_plan++;
			} else if (carAvail.equals("always")) {
				this.always_hasCar++;
				if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					this.always_but_pt_plan++;
				else if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan))
					this.always_but_car_plan++;
			} else if (carAvail.equals("sometimes")) {
				this.sometimes_hasCar++;
				if (
				// planType.equals(Plan.Type.PT)
				PlanModeJudger.usePt(selectedPlan))
					this.sometimes_but_pt_plan++;
				else if (
				// planType.equals(Plan.Type.CAR)
				PlanModeJudger.useCar(selectedPlan))
					this.sometimes_but_car_plan++;
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Gbl.startMeasurement();

		final String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		final String plansFilename = "../runs_SVN/run669/it.1000/1000.plans.xml.gz";
		final String outputFilename = "../runs_SVN/run669/it.1000/CarAvail.txt";

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(netFilename);

		CarAvail ca = new CarAvail();

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPopulationReader(scenario).readFile(plansFilename);

		ca.run(scenario.getPopulation());

		ca.write(outputFilename);

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

}
