/* *********************************************************************** *
 * project: org.matsim.*
 * EvacPlansFixer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.gregor.patna;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class EvacPlansFixer {
	
	public static void main(String [] args) {
		String inFile = "/Users/laemmel/svn/runs-svn/patnaIndia/run105/input/evac_plans.xml.gz";
		String netFile = "/Users/laemmel/svn/runs-svn/patnaIndia/run105/input/evac_network.xml.gz";
		String outFile = "/Users/laemmel/svn/runs-svn/patnaIndia/run105/input/evac_plans_gregor_s_version.xml.gz";
		
		
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		
		MatsimPopulationReader r = new MatsimPopulationReader(sc);
		r.readFile(inFile);
		
		MatsimNetworkReader nr = new MatsimNetworkReader(sc);
		nr.readFile(netFile);
		
		
		List<Person> walkers = new ArrayList<>();
		for (Person p : sc.getPopulation().getPersons().values()) {
			for (Plan pl : p.getPlans()) {
				Leg l = (Leg)pl.getPlanElements().get(1);//identify walkers 
				if (l.getMode().equals("walk")){
					walkers.add(p);
					break;
				}
				//and those who intend to start outside the network
				ActivityImpl home = (ActivityImpl) pl.getPlanElements().get(0);
				if (sc.getNetwork().getLinks().get(home.getLinkId()) == null) { //link does not exist
					walkers.add(p);
					break;
				}
				
				
				ActivityImpl evac = (ActivityImpl) pl.getPlanElements().get(2); //3rd element evac activity
				evac.setCoord(null);
				evac.setLinkId(Id.createLinkId("safeLink_Patna"));
			}
		}
		
		//remove walkers 
		for (Person w : walkers) {
			sc.getPopulation().getPersons().remove(w.getId());
		}
		
		PopulationWriter w = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		w.write(outFile);
	}

}
