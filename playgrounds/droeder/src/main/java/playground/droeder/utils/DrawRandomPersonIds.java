/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.PersonAlgorithm;

/**
 * @author droeder
 *
 */
final class DrawRandomPersonIds {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(DrawRandomPersonIds.class);
	protected static double sample = 0.2;
	private static String pFile = "E:\\VSP\\svn\\shared-svn\\studies\\countries\\de\\berlin\\plans\\baseplan_900s.xml.gz";
	private static String out = "C:\\Users\\Daniel\\Desktop\\pIds.txt";
	protected static int working = 0;

	private DrawRandomPersonIds() {
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Scenario sc =  ScenarioUtils.createScenario(ConfigUtils.createConfig());
		final Set<Id> ids =  new HashSet<Id>();
		
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		((PopulationImpl) sc.getPopulation()).addAlgorithm(new PersonAlgorithm() {
			
			private Random r = null;

			@Override
			public void run(Person person) {
				
				boolean works = false;
				for(int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i+=2){
					Activity a = (Activity) person.getSelectedPlan().getPlanElements().get(i);
					if(a.getType().equals("work")){
						works = true;
					}
				}
				if(works ){
					working ++;
					if(choose()){
						ids.add(person.getId());
					}
				}
			}

			private boolean choose() {
				if(r == null){
//					init random
					r = MatsimRandom.getLocalInstance();
					for(int i = 0; i < 10000; i++){
						r.nextDouble();
					}
				}
				return (r.nextDouble() < sample);
			}
		});
		new MatsimPopulationReader(sc).readFile(pFile);
		BufferedWriter w = IOUtils.getBufferedWriter(out);
		
		for(Id id: ids){
			try {
				w.write(id.toString() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			w.flush();
			w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("choose " + ids.size() + " of " + working + " persons working.");
	}
}

