/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzePlans.java
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

package playground.anhorni.locationchoice.analysis.plans;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.io.IOUtils;

public class AnalyzePlansActivities {

	private final ScenarioImpl scenario = new ScenarioImpl();
	private final Population plans = scenario.getPopulation();
	private final static Logger log = Logger.getLogger(AnalyzePlansActivities.class);


	/*
	 * 0: path to plans file
	 * 1: path to network file
	 * 2: path to facilities file
	 * 3: outpath
	 */
	public static void main(final String[] args) {
		log.info("Analyzig plans ...");
				
		String plansFile = args[0];
		String networkFile = args[1];
		String facilitiesFile = args[2];
		String outpath = args[3];
		
		final AnalyzePlansActivities analyzer = new AnalyzePlansActivities();
		analyzer.init(plansFile, networkFile, facilitiesFile);
		analyzer.run(plansFile, outpath);
		
		log.info("Finished analyzig plans ...");
		
	}

	public void run(String plansFile, String outpath) {
		this.analyze(outpath, plansFile);	
	}

	private void init(String plansFile, String networkFile, String facilitiesFile) {

		log.info("reading the facilities ...");
		new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesFile);

		log.info("reading the network ...");
		new MatsimNetworkReader(this.scenario).readFile(networkFile);

		log.info("reading the plans file " + plansFile);
		final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
		plansReader.readFile(plansFile);
	}


	private void analyze(String outpath, String plansFile) {

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "plan_activities.txt");

			int numberOfPersons = 0;
			int numberOfActs = 0;
			
			int numberOfShoppingActs = 0;
			int numberOfLeisureActs = 0;
			int numberOfWorkActs = 0;
			int numberOfEducationActs = 0;
			int numberOfHomeActs = 0;
			
			int numberOfPersonsInclWorking = 0;
			int numberOfPersonsInclEducation = 0;
			
			int numberOfPersonsWorkAndLeisure = 0;
			int numberOfPersonsEducationAndLeisure = 0;
			
			int numberOfPersonsWorkOnly = 0;
			int numberOfPersonsEducationOnly = 0;
			int numberOfPersonsLeisureOnly = 0;
			int numberOfPersonsShopOnly = 0;
			int numberOfPersonsHomeOnly = 0;
			
			int numberOfPersonsShopAndLeisureOnly = 0;

			Iterator<? extends Person> person_it = this.plans.getPersons().values().iterator();
			while (person_it.hasNext()) {
				
				numberOfPersons++;
				
				Person person = person_it.next();
				Plan selectedPlan = person.getSelectedPlan();
				
				boolean workOnly = true;
				boolean educationOnly = true;
				boolean leisureOnly = true;
				boolean shopOnly = true;
				boolean homeOnly = true;
				
				boolean work = false;
				boolean education = false;
				boolean leisure = false;
				boolean shop = false;				

				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);
					
					numberOfActs++;
					
					if (act.getType().startsWith("shop")) {
						numberOfShoppingActs++;
						
						shop = true;
						
						workOnly = false;
						educationOnly = false;
						leisureOnly = false;
						homeOnly = false;
					}
					else if (act.getType().startsWith("leisure")) {
						numberOfLeisureActs++;
						
						leisure = true;
						
						workOnly = false;
						educationOnly = false;
						shopOnly = false;
						homeOnly = false;
					}
					else if (act.getType().startsWith("work")) {
						numberOfWorkActs++;
						
						work = true;
						
						leisureOnly = false;
						educationOnly = false;
						shopOnly = false;
						homeOnly = false;
					}
					else if (act.getType().startsWith("education")) {
						numberOfEducationActs++;
						
						education = true;
						
						workOnly = false;
						leisureOnly = false;
						shopOnly = false;
						homeOnly = false;
					}
					else if (act.getType().startsWith("home")) {
						numberOfHomeActs++;
					}
				}
				if (workOnly) {	numberOfPersonsWorkOnly++;}
				if (educationOnly) {numberOfPersonsEducationOnly++;}
				if (leisureOnly) {numberOfPersonsLeisureOnly++;}
				if (shopOnly) {numberOfPersonsShopOnly++;}
				if (homeOnly) {numberOfPersonsHomeOnly++;}
				
				if (shop && leisure &&!work && !education) {numberOfPersonsShopAndLeisureOnly++;}
				
				if (work) {numberOfPersonsInclWorking++;}
				if (education) {numberOfPersonsInclEducation++;}
				
				if (work && leisure) {numberOfPersonsWorkAndLeisure++;}
				if (education && leisure) {numberOfPersonsEducationAndLeisure++;}
			}	
			out.write("Plans file: " + plansFile +"\n");
			
			out.write("numberOfPersons: \t" + numberOfPersons + "\n");
			out.write("numberOfActs: \t" + numberOfActs + "\n");
			
			out.write("numberOfPersonsInclWorking: \t" + numberOfPersonsInclWorking + "\n");
			out.write("numberOfPersonsInclEducation: \t" + numberOfPersonsInclEducation + "\n");

			out.write("numberOfPersonsWorkOnly: \t" + numberOfPersonsWorkOnly + "\n");
			out.write("numberOfPersonsEducationOnly: \t" + numberOfPersonsEducationOnly + "\n");
			out.write("numberOfPersonsLeisureOnly: \t" + numberOfPersonsLeisureOnly + "\n");
			out.write("numberOfPersonsShopOnly: \t" + numberOfPersonsShopOnly + "\n");
			out.write("numberOfPersonsHomeOnly: \t" + numberOfPersonsHomeOnly + "\n");
			
			out.write("numberOfPersonsWorkAndLeisure: \t" + numberOfPersonsWorkAndLeisure + "\n");
			out.write("numberOfPersonsEducationAndLeisure: \t" + numberOfPersonsEducationAndLeisure + "\n");
			
			out.write("numberOfPersonsShopAndLeisureOnly: \t" + numberOfPersonsShopAndLeisureOnly + "\n");
			
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}