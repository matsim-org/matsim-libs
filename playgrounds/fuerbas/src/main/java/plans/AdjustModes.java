/*
 * Copyright (C) 2005 The ExTeX Group and individual authors listed below
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */
package plans;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ArgumentParser;

public class AdjustModes {
	
	private static final String CAR_MODE = "car";
	
	private static final Logger logger = Logger.getLogger(AdjustModes.class);
	
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
		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(this.configfile);
		sl.loadNetwork();
		sl.loadPopulation();
		NetworkImpl network = sl.getScenario().getNetwork();
		this.config = sl.getScenario().getConfig();

		final PopulationImpl plans = (PopulationImpl) sl.getScenario().getPopulation();	
		
		logger.info("Processing persons...");
		Set<Person> toBeRemoved = new HashSet<Person>(); 
		int cntr = 0;
		for (Person person : plans.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()){ 
					if(element instanceof Leg) {
						if (!((Leg) element).getMode().equalsIgnoreCase(CAR_MODE))	{
							toBeRemoved.add(person);
						}
					}
				}
			}
			
			cntr++;
			if(cntr % 1000 == 0)
				logger.info(String.format("Parsed %1$s person - %2$s to be removed.", cntr, toBeRemoved.size()));
		}
		
		logger.info("Removing persons...");
		for (Person person : toBeRemoved) {
			plans.getPersons().remove(person.getId());
		}
		logger.info(String.format("Removed %1$s of %2$s persons - new size: %3$s", toBeRemoved.size(), cntr, plans.getPersons().size()));

		logger.info("Writing persons...");
		PopulationWriter plansWriter = new PopulationWriter(plans, network);
		String outfile = this.config.findParam("plans", "inputPlansFile") + "out";
		plansWriter.write(outfile);
		logger.info("Done.");
		
	}
	
	
	public static void main(String[] args) {
		
		new AdjustModes().run(args);

	}

}
