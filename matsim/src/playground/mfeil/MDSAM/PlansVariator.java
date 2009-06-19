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



import java.util.ArrayList;
import java.util.List;

import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.api.facilities.ActivityOption;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.PlanElement;
import org.matsim.core.api.population.Population;
import org.matsim.core.api.population.Plan;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.router.PlansCalcRoute;

import playground.mfeil.PlanomatXPlan;





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
			return;
		}
		
		Plan[] output = new PlanImpl [this.noOfVariedPlans];
		
		/* Copy the plan into all fields of the output array and vary plans */
		for (int i = 0; i < output.length; i++){
			output[i] = new PlanImpl (plan.getPerson());
			output[i].copyPlan(plan);	
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
		
		/* Change number */
		int slots = (int) (this.noOfVariedPlans*this.shareAC*this.shareNumber);
		
		int j;
		if (slots/(this.noOfMaxActs-1)>=1)j=1;
		else j=java.lang.Math.max((output[0].getPlanElements().size()/2+1)-(slots/2),1);
		
		for (int i=0;i<slots;i++){	
			if (j>this.noOfMaxActs) j=3; //sets the index back to an act chain of 3 acts (1 and 2 exist only once).	
			if (j==output[i].getPlanElements().size()/2+1) j++; // jumps over the base act chain.
			while (j<=output[i].getPlanElements().size()/2){
				if (j==1) output[i].removeActivity(0); //remove first act until only one final home act remains
				else {
					int pos = (1+((int)(MatsimRandom.getRandom().nextDouble()*((output[i].getPlanElements().size()-2)/2))))*2;
					output[i].removeActivity(pos);
					/* Recovers the route of the trip in front of the removed act */
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
		
		/* Change order */
		//TODO: Positions should be indexed in acts order, not planElements order
		int[] orderPos = {2,4};
		for (int i=counter;i<this.noOfVariedPlans*this.shareAC*(this.shareNumber+this.shareOrder);i++){
			if (!this.changeOrder(output[i], orderPos)) break;
			counter++;
		}
		
		/* Change type */
		int [] typePos = new int [(output[counter].getPlanElements().size()/2)];
		for (int x=1;x<output[counter].getPlanElements().size()/2;x++){
			typePos[x] = (int)(MatsimRandom.getRandom().nextDouble()*this.actTypes.size());
		}
		int rotationPos = 1;
		for (int i=counter;i<this.noOfVariedPlans*this.shareAC;i++){
			rotationPos = this.changeType(output[i], typePos, rotationPos);
			counter++;
		}
		
		
		
		/* Location and route choice for all slots */
		for (int i=0;i<output.length;i++){
			locator.handlePlan(output[i]);
			router.run(output[i]);
		}
		
	}
	
	private boolean changeOrder (Plan plan, int [] positions){
		
		List<PlanElement> actslegs = plan.getPlanElements();

		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order. Do nothing.		
			return false;
		}
		else {
			for (int planBasePos = positions[0]; planBasePos < actslegs.size()-4; planBasePos=planBasePos+2){			
				for (int planRunningPos = positions[1]; planRunningPos < actslegs.size()-2; planRunningPos=planRunningPos+2){ //Go through the "inner" acts only
					positions[1] = positions[1]+2;
					
					/*Activity swapping	*/		
					Activity act0 = (Activity)(actslegs.get(planBasePos));
					Activity act1 = (Activity)(actslegs.get(planRunningPos));
					if (act0.getType()!=act1.getType() &&
							act0.getFacilityId() != act1.getFacilityId()){
							
						Activity actHelp = new ActivityImpl ((Activity)(actslegs.get(planBasePos)));
						
						actslegs.set(planBasePos, actslegs.get(planRunningPos));
						actslegs.set(planRunningPos, actHelp);
						
						positions[0] = planBasePos;
						return true;
					}
				}
				positions[1] = planBasePos+4;
			}
			return false;
		}
	}
	
	private int changeType (Plan plan, int [] position, int rotationPos){
			
		Activity act = (Activity) plan.getPlanElements().get(rotationPos*2);
		String type = act.getType();
				
		while (type.equals(this.actTypes.get(position[rotationPos]))) {
			position[rotationPos]++;
			if (position[rotationPos]>=this.actTypes.size()) position[rotationPos] = 0;
		} 
		
		act.setType(this.actTypes.get(position[rotationPos]));
		if (act.getType().equalsIgnoreCase("home")){
			act.setFacility(((Activity)(plan.getPlanElements().get(0))).getFacility());
			act.setCoord(((Activity)(plan.getPlanElements().get(0))).getCoord());
			act.setLink(((Activity)(plan.getPlanElements().get(0))).getLink());
		}
		position[rotationPos]++;
		if (position[rotationPos]>=this.actTypes.size()) position[rotationPos] = 0;
		rotationPos++;
		if (rotationPos>=plan.getPlanElements().size()/2) rotationPos=1;
		return rotationPos;		
	}
	
}
