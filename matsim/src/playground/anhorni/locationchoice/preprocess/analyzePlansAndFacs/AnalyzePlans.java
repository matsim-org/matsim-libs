/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSelectedPlansTables.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.analyzePlansAndFacs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.utils.io.IOUtils;

public class AnalyzePlans {

	private Population plans = new PopulationImpl(false);
	
	private final static Logger log = Logger.getLogger(AnalyzePlans.class);
	
	public void run(final String plansfilePath, Facilities facilities, NetworkLayer network) {;
		this.readPlansFile(plansfilePath, network);

		write("./output/plan_activities_summary.txt");
		System.out.println("finished");
	}

	private void readPlansFile(final String plansfilePath, NetworkLayer network) {
		log.info("  reading file " + plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans, network);
		plansReader.readFile(plansfilePath);
	}

	private void write(String outfile) {

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outfile);
			
			int numberOfShoppingActs = 0;
			double totalDesiredShoppingDuration = 0.0;
			int numberOfLeisureActs = 0;
			double totalDesiredLeisureDuration = 0.0;
			
			Iterator<Person> person_it = this.plans.getPersons().values().iterator();
			while (person_it.hasNext()) {
				Person person = person_it.next();
				Plan selectedPlan = person.getSelectedPlan();
				
				final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Activity act = (Activity)actslegs.get(j);
					if (act.getType().startsWith("shop")) {
						numberOfShoppingActs++;
						totalDesiredShoppingDuration += person.getDesires().getActivityDuration("shop");
					}
					else if (act.getType().startsWith("leisure")) {
						numberOfLeisureActs++;
						totalDesiredLeisureDuration += person.getDesires().getActivityDuration("leisure");
					}
				}
			}
			out.write("Number of shopping activities: " + numberOfShoppingActs + "\n");
			out.write("Total desired duration of shopping activities: " + 1/3600.0 * totalDesiredShoppingDuration + " [h] \n");
			out.write("Avg. desired shopping duration: " + 1/3600.0 * (totalDesiredShoppingDuration / numberOfShoppingActs) + " [h] \n");
			out.newLine();
			out.write("Number of leisure activities: " + numberOfLeisureActs + "\n");
			out.write("Total desired duration of leisure activities: " + 1/3600.0 * totalDesiredLeisureDuration + " [h] \n");
			out.write("Avg. desired leisure duration: " + 1/3600.0 * (totalDesiredLeisureDuration / numberOfLeisureActs) + " [h] \n");
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}
