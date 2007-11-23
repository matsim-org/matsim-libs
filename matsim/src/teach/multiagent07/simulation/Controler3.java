/* *********************************************************************** *
 * project: org.matsim.*
 * Controler3.java
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

package teach.multiagent07.simulation;

import org.matsim.utils.vis.netvis.NetVis;

import teach.multiagent07.net.CANetStateWriter;
import teach.multiagent07.net.CANetwork;
import teach.multiagent07.net.CANetworkReader;
import teach.multiagent07.population.Person;
import teach.multiagent07.population.Population;
import teach.multiagent07.population.PopulationReader;
import teach.multiagent07.util.PersonsWriterTXT;


public class Controler3 {

	public static void main(String[] args) {

		//String netFileName = "e:\\Development\\tmp\\uckermark\\uckermark.xml";
		String netFileName = "..\\..\\tmp\\studies\\equil\\equil_net.xml";
		//String netFileName = "E:\\Uebungen MobSim S06\\uckermark + plans\\uckermark_gk4b.xml";

		
		// Read plans
		String popFileName = "..\\..\\tmp\\studies\\equil\\DSequil_plans.xml";
		//String popFileName = "E:\\Uebungen MobSim S06\\uckermark + plans\\kutter010car2_uckermark.xy2links_uckermarkgk4b.plans.v0.xml";
		
		// Create network
		CANetwork net = new CANetwork();

		// Read network
		CANetworkReader reader = new CANetworkReader(net, netFileName);
		reader.readNetwork();
		// connect network
		net.connect();
		// build network
		net.build();
		// fill with vehicles
		//net.randomfill(0.4);

		String visFileName = "../../tmp/testViz";
		
		// Read plans
		Population population = new Population();
		PopulationReader popreader = new PopulationReader(population, net, popFileName);
		popreader.readPopulation();
		// print ALL selected plans
		population.runHandler(new PersonsWriterTXT());
		
		String currentVisFile = visFileName;
		
		for (int i = 0; i < 10 ; i++) {
			
			// open network writer
			currentVisFile = visFileName + "iteration_" + i;
			CANetStateWriter netVis = CANetStateWriter.createWriter(net, netFileName, currentVisFile);

			EventManager events = new EventManager();

			// run simulation
			CAMobSim sim = new CAMobSim(net, netVis, events);
			sim.run(population);
			
			// print a list of events
			//events.runHandler(new EventWriterTXT());

			// calc Scores
			Scorer scorer = new Scorer(population);
			events.runHandler(scorer);
			
			// replan the first leg of the selected Plan
			// update 20% of the plans with departure Times
			double replanningRate = 0.2;
			
			double avgScore = 0;
			int personcount  = 0;
			
			for(Person person: population.getPersons()) {
				avgScore += person.getSelectedPlan().getScore();
				personcount++;
				
				// take a random sample
				if (Math.random() < replanningRate) {
					
					person.testAndExchange();
				}
			}
			System.out.println("Iteration " + i + " Avg Score: " + avgScore);
		}
		
		String[] visargs = {currentVisFile};
		NetVis.main(visargs);
	}

}
