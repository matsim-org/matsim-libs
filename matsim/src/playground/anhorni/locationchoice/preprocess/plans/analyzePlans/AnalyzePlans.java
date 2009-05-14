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

package playground.anhorni.locationchoice.preprocess.plans.analyzePlans;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.world.World;

public class AnalyzePlans {

	private Population plans = new PopulationImpl();
	private ActivityFacilities facilities;
	private NetworkLayer network;
	
	private String plansfilePath;
	private String facilitiesfilePath;
	private String networkfilePath;
	
	private DecimalFormat formatter = new DecimalFormat("0.0");
	
	private final static Logger log = Logger.getLogger(AnalyzePlans.class);
	
	
	public static void main(final String[] args) {

		Gbl.startMeasurement();
		final AnalyzePlans analyzer = new AnalyzePlans();
		
		String pathsFile = "./input/trb/valid/paths.txt";
		analyzer.readInputFile(pathsFile);
		
		String outpath = "output/valid/plans/";
		
		analyzer.init();
		analyzer.analyze1(outpath);
		
		analyzer.analyze2("shop", outpath);
		analyzer.analyze2("leisure", outpath);
		
		analyzer.analyze3(outpath);
		analyzer.analyze4(outpath);
		
		Gbl.printElapsedTime();
	}
	
	private void readInputFile(final String inputFile) {
		try {
			FileReader fileReader = new FileReader(inputFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			this.networkfilePath = bufferedReader.readLine();
			this.facilitiesfilePath = bufferedReader.readLine();
			this.plansfilePath = bufferedReader.readLine();

			bufferedReader.close();
			fileReader.close();

		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	
	private void init() {
		
		World world = Gbl.getWorld();
				
		log.info("reading the facilities ...");
		this.facilities =(ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
			
		log.info("reading the network ...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		
		log.info("  reading file " + plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans, network);
		plansReader.readFile(plansfilePath);				
	}
	

	private void analyze1(String outpath) {

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "plan_activities_summary.txt");
			
			int numberOfShoppingActs = 0;
			double totalDesiredShoppingDuration = 0.0;
			int numberOfLeisureActs = 0;
			double totalDesiredLeisureDuration = 0.0;
			
			Iterator<Person> person_it = this.plans.getPersons().values().iterator();
			while (person_it.hasNext()) {
				Person person = person_it.next();
				Plan selectedPlan = person.getSelectedPlan();
				
				int numberOfShoppingActsPerPerson = 0;
				int numberOfLeisureActsPerPerson = 0;
				double desiredShopPerPerson = 0.0;
				double desiredLeisurePerPerson = 0.0;
				
				final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Activity act = (Activity)actslegs.get(j);
					if (act.getType().startsWith("shop")) {
						numberOfShoppingActs++;
						desiredShopPerPerson += person.getDesires().getActivityDuration("shop");
						numberOfShoppingActsPerPerson++;
					}
					else if (act.getType().startsWith("leisure")) {
						numberOfLeisureActs++;
						desiredLeisurePerPerson += person.getDesires().getActivityDuration("leisure");
						numberOfLeisureActsPerPerson++;
					}
				}
				if (numberOfShoppingActsPerPerson > 0) {
					totalDesiredShoppingDuration += (desiredShopPerPerson / numberOfShoppingActsPerPerson);
				}
				if (numberOfLeisureActsPerPerson > 0) {
					totalDesiredLeisureDuration += (desiredLeisurePerPerson / numberOfLeisureActsPerPerson);
				}				
			}
			out.write("Plans file: " + this.plansfilePath +"\n");
			out.write("Number of shopping activities: \t" + numberOfShoppingActs + "\n");
			out.write("Total desired duration of shopping activities: \t" + 
					formatter.format(1/3600.0 * totalDesiredShoppingDuration) + " [h] \n");
			out.write("Avg. desired shopping duration: \t" + 
					formatter.format(1/3600.0 * (totalDesiredShoppingDuration / numberOfShoppingActs)) + " [h] \n");
			out.newLine();
			out.write("Number of leisure activities: \t" + numberOfLeisureActs + "\n");
			out.write("Total desired duration of leisure activities: \t" + 
					formatter.format(1/3600.0 * totalDesiredLeisureDuration) + " [h] \n");
			out.write("Avg. desired leisure duration: \t" + 
					formatter.format(1/3600.0 * (totalDesiredLeisureDuration / numberOfLeisureActs)) + " [h] \n");
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void analyze2(final String type, String outpath) {
		try {
			final String header="Person_id\tActDuration\tDesiredDuration";
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "personActDurations_" + type + ".txt");
			final BufferedWriter outSummary = IOUtils.getBufferedWriter(outpath + "/summary_"+type+".txt");
			out.write(header);
			out.newLine();
			int numberOfPersonsDoingType = 0;

			Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
			Counter counter = new Counter(" person # ");
			while (person_iter.hasNext()) {
				Person person = person_iter.next();
				counter.incCounter();
				boolean personSet = false;

				Plan selectedPlan = person.getSelectedPlan();

				final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Activity act = (Activity)actslegs.get(j);
					if (act.getType().startsWith(type)) {
						out.write(person.getId().toString()+"\t"+
								String.valueOf(act.getDuration())+"\t"+
								person.getDesires().getActivityDuration(type));
						out.newLine();

						if (!personSet) {
							numberOfPersonsDoingType++;
							personSet = true;
						}
					}
				}
				out.flush();
			}
			out.close();
			
			outSummary.write("Number of persons doing " + type + " :\t" + numberOfPersonsDoingType + "\n");
			outSummary.flush();
			outSummary.close();
			}
			catch (final IOException e) {
				Gbl.errorMsg(e);
			}
		}
	
	private void analyze3(String outpath) {

		try {
			final String header="Person_id\tnumberOfIntermediateShopandLeisureActs";
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "actchainsplananalysis.txt");
			out.write(header);
			out.newLine();


			Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
			Counter counter = new Counter(" person # ");
			while (person_iter.hasNext()) {
				Person person = person_iter.next();
				counter.incCounter();
				Plan selectedPlan = person.getSelectedPlan();
				final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();

				int countSL = 0;
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Activity act = (Activity)actslegs.get(j);
					if (act.getType().startsWith("s") || act.getType().startsWith("l")) {
						countSL++;
					}
					else if (act.getType().startsWith("h") || act.getType().startsWith("w")||
							act.getType().startsWith("e")) {
						if (countSL > 0) {
							out.write(person.getId().toString()+"\t"+String.valueOf(countSL)+"\n");
							countSL = 0;
						}
					}
				}
			}
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void analyze4(String outpath) {
		int countPrim = 0;
		int numberOfPersonsDoingSL = 0;
		int numberOfTrips = 0;

		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			counter.incCounter();
			boolean personSet = false;

			Plan selectedPlan = person.getSelectedPlan();
			List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Activity act = (Activity)actslegs.get(j);
				if (act.getType().startsWith("s")) {
					if (!personSet) {
						numberOfPersonsDoingSL++;
						personSet = true;
					}
				}
				else if (act.getType().startsWith("l")) {
					if (!personSet) {
						numberOfPersonsDoingSL++;
						personSet = true;
					}
				}
				else if (act.getType().startsWith("h") || act.getType().startsWith("w")||
						act.getType().startsWith("e")) {
					countPrim++;
				}
			}
			numberOfTrips += (actslegs.size()-1)/2;
		}
		
		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "summary2.txt");
			out.write("Total number of Primary Activities: \t"+ countPrim + "\n");
			out.write("Number of Persons Doing Shop or Leisure: \t"+ numberOfPersonsDoingSL + "\n");
			double avgNumberOfTrips = (double)numberOfTrips/(double)this.plans.getPersons().size();
			out.write("Number of persons: \t" + this.plans.getPersons().size());
			out.write("Avg number of trips per person: \t" + avgNumberOfTrips);
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}	
	}	
}
