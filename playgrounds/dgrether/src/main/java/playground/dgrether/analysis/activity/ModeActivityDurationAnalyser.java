/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.analysis.activity;

import java.io.File;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class ModeActivityDurationAnalyser {

	private static final String EXAMPLEBASE = "examples/";

	private static final String EQUILBASE = EXAMPLEBASE + "equil/";

	private static final String NETWORK = EQUILBASE + "network.xml";

	private static final String PLANSFILEBASE = "/Volumes/data/work/cvsRep/vsp-cvs/documents/papers/2008/paralimes/data/outputPlansSelectRuns/";

	private static final String PLANSFILE = PLANSFILEBASE + "run591.output_plans.xml";

//	private static final String PLANSFILE = PLANSFILEBASE + "588.output_plans.xml";

	private static final String CONFIGFILE = EQUILBASE + "config.xml";

	private final double t0Home = 12.0*Math.exp(-10.0/12.0);
	private final double t0Work = 8.0*Math.exp(-10.0/8.0);

	private final Config config;

	public ModeActivityDurationAnalyser() {
		
		this.config = Gbl.createConfig(new String[] {CONFIGFILE});
//	config = Gbl.createConfig(null);
//	Gbl.getWorld().setNetworkLayer(net);
//	Gbl.getWorld().complete();

		ScenarioImpl scenario = new ScenarioImpl(config);
		
		File f = new File("test.txt");
		System.out.println(f.getAbsolutePath());
		
		NetworkLayer net = scenario.getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(net);
		reader.readFile(NETWORK);

		PopulationImpl plans = scenario.getPopulation();
		MatsimPopulationReader plansParser = new MatsimPopulationReader(scenario);
		plansParser.readFile(PLANSFILE);

		double homeActivityDurationsCar = 0.0;
		double homeActivityDurationsNonCar = 0.0;
		double workActivityDurationsCar = 0.0;
		double workActivityDurationsNonCar = 0.0;
		int homeActivityCarCount = 0;
		int homeActivityNonCarCount = 0;
		int workActivityCarCount = 0;
		int workActivityNonCarCount = 0;
		double durTemp;

		for (Person pers : plans.getPersons().values()){
			Plan p = pers.getSelectedPlan();
			for (PlanElement pe : p.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					try {
						durTemp = act.calculateDuration();
						if (act.getType().equalsIgnoreCase("h")) {
							if (((PlanImpl) p).getType().equals(PlanImpl.Type.CAR)) {
								homeActivityDurationsCar += durTemp;
								homeActivityCarCount++;
							}
							else if (((PlanImpl) p).getType().equals(PlanImpl.Type.PT)){
								homeActivityDurationsNonCar += durTemp;
								homeActivityNonCarCount++;
							}
						}
						else if (act.getType().equalsIgnoreCase("w")) {
							if (((PlanImpl) p).getType().equals(PlanImpl.Type.CAR)) {
								workActivityDurationsCar += durTemp;
								workActivityCarCount++;
							}
							else if (((PlanImpl) p).getType().equals(PlanImpl.Type.PT)){
								workActivityDurationsNonCar += durTemp;
								workActivityNonCarCount++;
							}
						}
					} catch (Exception e) {
						System.err.println(e.getMessage());
						System.out.println("No duration for plan: " + p + " of person " + pers.getId());
					}
				}
			}
		}
		System.out.println("Total home activity duration for mode car:     " + homeActivityDurationsCar);
		System.out.println("Total home activity duration for mode non-car: " + homeActivityDurationsNonCar);
		System.out.println("Total work activity duration for mode car:     " + workActivityDurationsCar);
		System.out.println("Total work activity duration for mode non-car: " + workActivityDurationsNonCar);
		System.out.println();
		System.out.println("Average home activity duration for mode non-car: " + Time.writeTime(homeActivityDurationsNonCar / homeActivityNonCarCount));
		System.out.println("Average home activity duration for mode car:     " + Time.writeTime(homeActivityDurationsCar / homeActivityCarCount));
		System.out.println("Average work activity duration for mode car:     " + Time.writeTime(workActivityDurationsCar / workActivityCarCount));
		System.out.println("Average work activity duration for mode non-car: " + Time.writeTime(workActivityDurationsNonCar / workActivityNonCarCount));
		System.out.println();
		System.out.println("Marginal utility of home activity total: " + (6.0 * 12.0 ) / (((homeActivityDurationsNonCar + homeActivityDurationsCar)  / 3600.0) / (homeActivityNonCarCount + homeActivityCarCount)));
		System.out.println("Marginal utility of work activity total:" + (6.0 * 8.0 ) / (((workActivityDurationsCar + workActivityDurationsNonCar)  / 3600.0)  / (workActivityCarCount + workActivityNonCarCount)));

		System.out.println("Marginal utility of home activity car: " + (6.0 * 12.0 ) / ((homeActivityDurationsCar   / 3600.0) / homeActivityCarCount));
		System.out.println("Marginal utility of home activity non-car: " + (6.0 * 12.0 ) / ((homeActivityDurationsNonCar  / 3600.0)  / homeActivityNonCarCount));
		System.out.println("Marginal utility of work activity car: " + (6.0 * 8.0) / ((workActivityDurationsCar   / 3600.0) / workActivityCarCount));
		System.out.println("Marginal utiltiy of work activity non-car: " + (6.0 * 8.0)  / ((workActivityDurationsNonCar   / 3600.0) / workActivityNonCarCount));
	}


	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		new ModeActivityDurationAnalyser();

	}

}
