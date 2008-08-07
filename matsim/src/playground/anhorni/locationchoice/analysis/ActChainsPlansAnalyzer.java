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
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Counter;


/**
 * @author anhorni
 */

public class ActChainsPlansAnalyzer {

	private Population plans=null;
	private NetworkLayer network=null;
	private final static Logger log = Logger.getLogger(ActChainsPlansAnalyzer.class);


	/**
	 * @param
	 *  - path of the plans file
	 */
	public static void main(String[] args) {

		if (args.length < 1 || args.length > 1 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath = args[0];
		log.info(plansfilePath);
		
		String networkfilePath="./input/network.xml";

		ActChainsPlansAnalyzer analyzer = new ActChainsPlansAnalyzer();
		analyzer.init(plansfilePath, networkfilePath);
		analyzer.analyze();
	}

	private void init(final String plansfilePath, final String networkfilePath) {

		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		this.plans=new Population(false);
		final PopulationReader plansReader = new MatsimPopulationReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
	}

	
	private void analyze() {
		
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

