/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTimeAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package plans;

import java.util.Iterator;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ArgumentParser;

public class ActivitySimplifier {

	/**
	 * @param args
	 */
	
	
	private Config config;
	private String configfile = null;	
	
	private void parseArguments(final String[] args) {
		if (args.length == 0) {
			System.out.println("Too few arguments.");
			System.exit(1);
		}
		Iterator<String> argIter = new ArgumentParser(args).iterator();
		String arg = argIter.next();
		if (arg.equals("-h") || arg.equals("--help")) {
			System.exit(0);
		} else {
			this.configfile = arg;
			if (argIter.hasNext()) {
				System.out.println("Too many arguments.");
				System.exit(1);
			}
		}
	}
	
	public void run(final String[] args) {
		parseArguments(args);
		ScenarioLoaderImpl sl = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(this.configfile);
		sl.loadNetwork();
		sl.loadPopulation();
		Network network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();	
		
		
		for (Person person : plans.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()){
					if(element instanceof Activity) {
						String act = ((Activity) element).getType();
						if (act.startsWith("w")) ((Activity) element).setType("work");
						else if (act.startsWith("h")) ((Activity) element).setType("home");
						else if (act.startsWith("e")) ((Activity) element).setType("edu");
						else if (act.startsWith("l")) ((Activity) element).setType("leisure");
						else if (act.startsWith("s")) ((Activity) element).setType("shop");
					}
				}
			}
		}
		
		
		plans.setIsStreaming(false);
		final PopulationWriter plansWriter = new PopulationWriter(plans, network);
		plans.addAlgorithm(plansWriter);
		plans.printPlansCount();
		plansWriter.write(this.config.findParam("plans", "outputPlansFile"));

		System.out.println("done.");
		
	}
	
	
	public static void main(String[] args) {
		new ActivitySimplifier().run(args);
	}

}
