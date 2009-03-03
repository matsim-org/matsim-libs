/* *********************************************************************** *
 * project: org.matsim.*
 * PlansAnalyzer.java
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

package playground.anhorni.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Counter;


/**
 * @author anhorni
 */

public class PlansAnalyzer {

	private Population plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities =null;

	private final static Logger log = Logger.getLogger(PlansAnalyzer.class);


	/**
	 * @param
	 *  - path of the plans file
	 */
	public static void main(final String[] args) {

		if (args.length < 1 || args.length > 1 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath = args[0];
		String type[] = {"s", "l"};

		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";

		log.info(plansfilePath);

		PlansAnalyzer analyzer = new PlansAnalyzer();
		log.info("Initialize analysis:");
		analyzer.init(plansfilePath, networkfilePath, facilitiesfilePath);

		log.info("Doing analysis");
		for (int i=0; i<2; i++) {
			analyzer.analyzeActDuration(type[i]);
		}
		analyzer.analyzeTotalNumberOfActivities();
		analyzer.analyzeSLBetweenPrimary();
		log.info("finished analysis");
	}

	private void init(final String plansfilePath, final String networkfilePath, final String facilitiesfilePath) {

		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");

		this.plans=new PopulationImpl(false);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");

	}


	private void analyzeActDuration(final String type) {

		try {
			final String header="Person_id\tActDuration\tTypicalDuration";
			final BufferedWriter out = IOUtils.getBufferedWriter("./output/plananalysis"+type+".txt");
			out.write(header);
			out.newLine();

			int numberOfPersonsDoingType = 0;
			int numberOfTrips = 0;

			Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
			Counter counter = new Counter(" person # ");
			while (person_iter.hasNext()) {
				Person person = person_iter.next();
				counter.incCounter();
				boolean personSet = false;

				Plan selectedPlan = person.getSelectedPlan();

				final ArrayList<?> actslegs = selectedPlan.getActsLegs();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Act act = (Act)actslegs.get(j);
					if (act.getType().startsWith(type)) {
						out.write(person.getId().toString()+"\t"+
								String.valueOf(act.getDuration())+"\t"+
								act.getType().substring(1));
						out.newLine();

						if (!personSet) {
							numberOfPersonsDoingType++;
							personSet = true;
						}
					}
				}
				numberOfTrips += (actslegs.size()-1)/2;
				out.flush();
			}
			log.info("Number of persons doing "+type+" :"+ numberOfPersonsDoingType +"\n");
			double avgNumberOfTrips = (double)numberOfTrips/(double)this.plans.getPersons().size();
			log.info("Avg number of trips per person: "+avgNumberOfTrips);
			out.close();
			}
			catch (final IOException e) {
				Gbl.errorMsg(e);
			}
		}

	private void analyzeTotalNumberOfActivities() {
		int countS = 0;
		int countL = 0;
		int countPrim = 0;

		int numberOfPersonsDoingSL = 0;

		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			counter.incCounter();
			boolean personSet = false;

			Plan selectedPlan = person.getSelectedPlan();
			final ArrayList<?> actslegs = selectedPlan.getActsLegs();


			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Act act = (Act)actslegs.get(j);
				if (act.getType().startsWith("s")) {
					countS++;
					if (!personSet) {
						numberOfPersonsDoingSL++;
						personSet = true;
					}
				}
				else if (act.getType().startsWith("l")) {
					countL++;
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
		}
		log.info("Total number of Shop Activities "+ countS);
		log.info("Total number of Leisure Activities "+ countL);
		log.info("Total number of Primary Activities "+ countPrim);
		log.info("Number of Persons Doing Shop or Leisure "+ numberOfPersonsDoingSL);
	}

	private void analyzeSLBetweenPrimary() {

		try {
			final String header="Person_id\tnumberOfSL";
			final BufferedWriter out = IOUtils.getBufferedWriter("./output/actchainsplananalysis.txt");
			out.write(header);
			out.newLine();


			Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
			Counter counter = new Counter(" person # ");
			while (person_iter.hasNext()) {
				Person person = person_iter.next();
				counter.incCounter();
				Plan selectedPlan = person.getSelectedPlan();
				final ArrayList<?> actslegs = selectedPlan.getActsLegs();

				int countSL = 0;
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final Act act = (Act)actslegs.get(j);
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
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
}

