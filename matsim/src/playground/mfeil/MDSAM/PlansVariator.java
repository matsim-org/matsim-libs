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



import java.util.List;

import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.Plan;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.router.PlansCalcRoute;




/**
 * Class that creates a number of (slightly) varied plans of a person's original plan.
 * 
 * @author Matthias Feil
 */
public class PlansVariator implements PlanAlgorithm {
	
	private final int agentIDthreshold;
	private final int noOfVariedPlans;
	private int noOfMaxActs;
	private final double shareAC, shareACMC, shareLCMC, shareNumber, shareOrder; 
	private final Controler controler;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRoute router;
	private final List<String> actTypes;
	
	public PlansVariator (Controler controler, LocationMutatorwChoiceSet locator, PlansCalcRoute router, List<String> actTypes){
		this.controler = controler;
		this.locator = locator;
		this.router = router;
		this.actTypes = actTypes;
		this.agentIDthreshold = 324;
		this.noOfVariedPlans = 20;
		this.shareAC = 0.75;
		this.shareACMC = 0.66;
		this.shareLCMC = 0.66;
		this.shareNumber = 0.66;
		this.shareOrder = 0.17;
		this.noOfMaxActs = 10;
	}
	
	
	public void run (Plan plan){
		
		/* Ensure that noOfMaxActs is greater or equal than the noOfActs of current plan */
		this.noOfMaxActs = java.lang.Math.max(this.noOfMaxActs, plan.getPlanElements().size()/2);
		
		/* Remove all non-selected plans */
		Population pop = controler.getPopulation();
		for (int i=0;i<(pop.getPersons().get(plan.getPerson().getId())).getPlans().size();i++){
			if (!(pop.getPersons().get(plan.getPerson().getId())).getPlans().get(i).isSelected()){
				pop.getPersons().get(plan.getPerson().getId()).getPlans().remove(i);
			}
		}
		
		/* Do nothing for non-relevant agents */
		if (Integer.parseInt(plan.getPerson().getId().toString())<this.agentIDthreshold) {
			System.out.println("return!");
			return;
		}
		
		Plan[] output = new PlanImpl [this.noOfVariedPlans];
		
		/* Copy the plan into all fields of the output array and vary plans */
		for (int i = 0; i < output.length; i++){
			output[i] = new PlanImpl (plan.getPerson());
			output[i].copyPlan(plan);	
			System.out.println("Hallo");
		}
		this.varyPlans(output);
		
		/* Add the new plans to the person's set of plans */
		for (int i = 0; i < output.length; i++){
			pop.getPersons().get(plan.getPerson().getId()).addPlan(output[i]);
		}	
		
		
		
		//PopulationWriter popwriter = new PopulationWriter(pop, "./plans/output_pop.xml");
		//popwriter.write();
	}
	
	private void varyPlans (Plan[] output){
		int counter = 0;
		int j=1;
		double slots = this.noOfVariedPlans*this.shareAC*this.shareNumber;
		for (int i=0;i<slots;i++){	
				
			if (j==output[i].getPlanElements().size()/2+1) j++;
			while (j<=output[i].getPlanElements().size()/2){
				if (j==1) output[i].removeActivity(0); //remove first act until only one final home act remains
				else {
					int pos = (1+((int)(MatsimRandom.getRandom().nextDouble()*((output[i].getPlanElements().size()-2)/2))))*2;
					output[i].removeActivity(pos);
					router.handleLeg((Leg)output[i].getPlanElements().get(pos-1), 
							(Activity)output[i].getPlanElements().get(pos-2), 
							(Activity)output[i].getPlanElements().get(pos), 
							((Activity)output[i].getPlanElements().get(pos-2)).getEndTime());
				}
			}
			while (j>output[i].getPlanElements().size()/2+1){
				if (output[i].getPlanElements().size()==1) output[i].insertLegAct(1, new LegImpl(TransportMode.walk), (Activity)output[i].getPlanElements().get(0));
				else {
					int actPosition = ((int)(java.lang.Math.floor(MatsimRandom.getRandom().nextDouble() * (output[i].getPlanElements().size()/2))))*2+1;
					String actType = this.actTypes.get((int)(MatsimRandom.getRandom().nextDouble() * this.actTypes.size()));
					Activity actHelp = new ActivityImpl ((Activity)output[i].getPlanElements().get(0));
					actHelp.setType(actType);
					Leg legHelp = new LegImpl ((Leg)output[i].getPlanElements().get(actPosition));
					output[i].insertLegAct(actPosition, legHelp, actHelp);
				}
			}
			j++;
			counter++;
		}
		for (int i=counter;i<this.noOfVariedPlans*this.shareAC*(this.shareNumber+this.shareOrder);i++){
			counter++;
		}
		
		for (int i=counter;i<this.noOfVariedPlans*this.shareAC;i++){
			counter++;
		}
		
		/* Location and route choice for all slots */
		for (int i=0;i<output.length;i++){
			locator.handlePlan(output[i]);
			router.run(output[i]);
		}
		
	}
}
