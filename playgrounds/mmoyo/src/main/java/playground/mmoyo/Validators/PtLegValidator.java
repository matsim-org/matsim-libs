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

package playground.mmoyo.Validators;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/**validates that all agents have at least a pt leg*/
public class PtLegValidator implements PersonAlgorithm{
	private final Set<Id> agents = new HashSet<Id>();
	
	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg) {
					Leg leg = (Leg)pe;
					if(leg.getMode().equals(TransportMode.pt)){
						return;
					}
				}
			}
		}
		agents.add(person.getId());
	}
	
	public Set<Id> getAgents(){
		return this.agents;
	}
	
	public static void main(String[] args) {
		String populationFile = "../../input/newDemand/bvg.run189.10pct.100.plans_Cleaned_planswithXYlinks.xml.gzplansWtPtLegs.xml.gz";
		String networkFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		
		DataLoader dataLoader = new DataLoader();
		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(networkFile);
		
		PtLegValidator legValidator = new PtLegValidator(); 
		PopSecReader popSecReader = new PopSecReader (scn, legValidator);
		popSecReader.readFile(populationFile);
		
		for (Id agentId : legValidator.getAgents()){
			System.out.println(agentId);
		}
		System.out.println("number of agents without pt-leg: " + legValidator.getAgents().size());
	}

}
