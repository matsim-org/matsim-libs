/* *********************************************************************** *
 * project: org.matsim.*
 * PlansVariator.java
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

package playground.mfeil.MDSAM;


import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.Plan;



/**
 * Class that creates a number of (slightly) varied plans of a person's original plan.
 * 
 * @author Matthias Feil
 */
public class PlansVariator implements PlanAlgorithm {
	
	private final int agentIDthreshold = 20;
	private final int noOfVariedPlans = 20;
	private Plan[] output = new PlanImpl [this.noOfVariedPlans];
	
	
	public void run (Plan plan){
		
		if (Integer.parseInt(plan.getPerson().getId().toString())<this.agentIDthreshold) return;
		
		Population pop = new PopulationImpl ();
		pop.getPersons().put(plan.getPerson().getId(), plan.getPerson());
		
		/* Copy the plan into all fields of the output array */
		for (int i = 0; i < this.output.length; i++){
			this.output[i] = new PlanImpl (plan.getPerson());
			this.output[i].copyPlan(plan);	
			//pop.getPersons().get(plan.getPerson().getId()).addPlan(this.output[i]);
		}	
		
		PopulationWriter popwriter = new PopulationWriter(pop, "./plans/output_pop.xml");
		popwriter.write();
	}
}
