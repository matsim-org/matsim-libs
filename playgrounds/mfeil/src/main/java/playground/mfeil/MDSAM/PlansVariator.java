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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;

import playground.mfeil.TimeOptimizer;
import playground.mfeil.config.TimeModeChoicerConfigGroup;






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
	private final TransportMode[] possibleModes;
	private final double maxWalkingDistance;
	private final TimeOptimizer timer;
	
	public PlansVariator (Controler controler, DepartureDelayAverageCalculator tDepDelayCalc, LocationMutatorwChoiceSet locator, PlansCalcRoute router, List<String> actTypes){
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
		this.possibleModes = TimeModeChoicerConfigGroup.getPossibleModes();
		this.maxWalkingDistance	= Double.parseDouble(TimeModeChoicerConfigGroup.getMaximumWalkingDistance());
		this.timer = new TimeOptimizer (this.controler, tDepDelayCalc);
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
		
		PlanImpl[] output = new PlanImpl [this.noOfVariedPlans];
		
		/* Copy the plan into all fields of the output array and vary plans */
		for (int i = 0; i < output.length; i++){
			output[i] = new PlanImpl (plan.getPerson());
			output[i].copyPlan(plan);	
		}
		this.varyPlans(output, plan);
		
		/* Add the new plans to the person's set of plans */
		for (int i = 0; i < output.length; i++){
			pop.getPersons().get(plan.getPerson().getId()).addPlan(output[i]);
		}	
		
		
		
		//PopulationWriter popwriter = new PopulationWriter(pop, "./plans/output_pop.xml");
		//popwriter.write();
	}
	
	private void varyPlans (PlanImpl[] output, Plan plan){
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
					router.handleLeg(plan.getPerson(), 
							(LegImpl)output[i].getPlanElements().get(pos-1), 
							(ActivityImpl)output[i].getPlanElements().get(pos-2), 
							(ActivityImpl)output[i].getPlanElements().get(pos), ((ActivityImpl)output[i].getPlanElements().get(pos-2)).getEndTime());
				}
			}
			while (j>output[i].getPlanElements().size()/2+1){
				if (output[i].getPlanElements().size()==1) output[i].insertLegAct(1, new LegImpl(TransportMode.walk), (ActivityImpl)output[i].getPlanElements().get(0));
				else {
					int actPosition = ((int)(java.lang.Math.floor(MatsimRandom.getRandom().nextDouble() * (output[i].getPlanElements().size()/2))))*2+1;
					String actType = this.actTypes.get((int)(MatsimRandom.getRandom().nextDouble() * this.actTypes.size()));
					ActivityImpl actHelp = new ActivityImpl ((ActivityImpl)output[i].getPlanElements().get(0));
					actHelp.setType(actType);
					LegImpl legHelp = new LegImpl ((LegImpl)output[i].getPlanElements().get(actPosition));
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
		int [] typePos = new int [(plan.getPlanElements().size()/2)];
		for (int x=1;x<plan.getPlanElements().size()/2;x++){
			typePos[x] = (int)(MatsimRandom.getRandom().nextDouble()*this.actTypes.size());
		}
		int rotationPos = 1;
		for (int i=counter;i<this.noOfVariedPlans*this.shareAC;i++){
			rotationPos = this.changeType(output[i], typePos, rotationPos);
			counter++;
		}		
		
		/* Location choice for all slots */
		for (int i=0;i<output.length;i++){
			locator.handlePlan(output[i]);
		}
		
		/* Check whether LC activity chains are duplicated */
		boolean [] chosen = new boolean [output.length];
		boolean [] equal = new boolean [output.length];
		int equalChains = 0;
		for (int i=counter;i<output.length;i++){
			equal[i]=this.checkEqualityOfLocations(output[i], plan);
			if (!equal[i]){
				for (j=counter;j<i;j++){
					equal[i]=this.checkEqualityOfLocations(output[i], output[j]);
					if (equal[i]) {
						chosen[j]=true; // causes that i-chain gets new modes but j-chain keeps stable.
						break;
					}
				}
			}
			if (equal[i]) equalChains++; 
		}
		
		/* Variation of modes */		
		// AC slots
		for (int i=0;i<this.noOfVariedPlans*this.shareAC*this.shareACMC;i++){
			
			/* Selection of plan */
			int MCpos = (int)(MatsimRandom.getRandom().nextDouble()*this.noOfVariedPlans*this.shareAC);
			while (chosen[MCpos] || output[MCpos].getPlanElements().size()<4){
				MCpos++;
				if (MCpos>=(int) (this.noOfVariedPlans*this.shareAC)) MCpos=0;
			}			
			this.changeMode(output[MCpos]);
			chosen[MCpos]=true;
		}
		
		// LC slots
		for (int i=0;i<equal.length;i++){
			if (equal[i]) {
				this.changeMode(output[i]);
				chosen[i]=true;
			}
		}
		Loop:
		for (int i=0;i<(int)(this.noOfVariedPlans*(1-this.shareAC)*this.shareLCMC)-equalChains;i++){
			int MCpos = (int)(this.noOfVariedPlans*this.shareAC) + (int)(MatsimRandom.getRandom().nextDouble()*this.noOfVariedPlans*(1-this.shareAC));
			int count=0;
			while (chosen[MCpos]){
				MCpos++;
				if (MCpos>=this.noOfVariedPlans) MCpos= (int)(this.noOfVariedPlans*this.shareAC);
				if (count>this.noOfVariedPlans*(1-this.shareAC)) break Loop;
				count++;
			}		
			this.changeMode(output[MCpos]);
			chosen[MCpos]=true;
		}
		
		/* Route choice for all slots */
		for (int i=0;i<output.length;i++){
			router.run(output[i]);
			timer.run(output[i]);
		}
		
	}
	
	private boolean changeOrder (PlanImpl plan, int [] positions){
		
		List<PlanElement> actslegs = plan.getPlanElements();

		if (actslegs.size()<=5){	//If true the plan has not enough activities to change their order. Do nothing.		
			return false;
		}
		else {
			for (int planBasePos = positions[0]; planBasePos < actslegs.size()-4; planBasePos=planBasePos+2){			
				for (int planRunningPos = positions[1]; planRunningPos < actslegs.size()-2; planRunningPos=planRunningPos+2){ //Go through the "inner" acts only
					positions[1] = positions[1]+2;
					
					/*Activity swapping	*/		
					ActivityImpl act0 = (ActivityImpl)(actslegs.get(planBasePos));
					ActivityImpl act1 = (ActivityImpl)(actslegs.get(planRunningPos));
					if (act0.getType()!=act1.getType() &&
							!act0.getFacilityId().equals(act1.getFacilityId())){
							
						ActivityImpl actHelp = new ActivityImpl ((ActivityImpl)(actslegs.get(planBasePos)));
						
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
	
	private int changeType (PlanImpl plan, int [] position, int rotationPos){
			
		ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(rotationPos*2);
		String type = act.getType();
				
		while (type.equals(this.actTypes.get(position[rotationPos]))) {
			position[rotationPos]++;
			if (position[rotationPos]>=this.actTypes.size()) position[rotationPos] = 0;
		} 
		
		act.setType(this.actTypes.get(position[rotationPos]));
		if (act.getType().equalsIgnoreCase("home")){
			act.setFacilityId(((Activity)(plan.getPlanElements().get(0))).getFacilityId());
			act.setCoord(((Activity)(plan.getPlanElements().get(0))).getCoord());
			act.setLinkId(((Activity)(plan.getPlanElements().get(0))).getLinkId());
		}
		position[rotationPos]++;
		if (position[rotationPos]>=this.actTypes.size()) position[rotationPos] = 0;
		rotationPos++;
		if (rotationPos>=plan.getPlanElements().size()/2) rotationPos=1;
		return rotationPos;		
	}
	
	
	/* Method that returns true if two plans feature the same activity chain and the same locations, or false otherwise.*/
	private boolean checkEqualityOfLocations (Plan plan1, Plan plan2){
			
		ArrayList<String> acts1 = new ArrayList<String> ();
		ArrayList<String> acts2 = new ArrayList<String> ();
		for (int i = 0;i<plan1.getPlanElements().size();i=i+2){
			//acts1.add(((Activity)(plan1.getPlanElements().get(i))).getType().toString());	
			acts1.add(((ActivityImpl)(plan1.getPlanElements().get(i))).getFacilityId().toString());	
		}
		for (int i = 0;i<plan2.getPlanElements().size();i=i+2){
			//acts2.add(((Activity)(plan2.getPlanElements().get(i))).getType().toString());
			acts2.add(((ActivityImpl)(plan2.getPlanElements().get(i))).getFacilityId().toString());	
		}
	
		return (acts1.equals(acts2));
		
	}	
	
	/* Method that calculates the distance of a subtour. 
	 * Returns 	0 if distance = 0m
	 * 			2 if distance is longer than this.maxWalkingDistance
	 * 			1 otherwise.
	 * */
	private int checksubtourDistance2 (List<PlanElement> actslegs, PlanAnalyzeSubtours planAnalyzeSubtours, int pos){
		double distance = 0;
		for (int k=0;k<((actslegs.size()/2));k++){
			if ((planAnalyzeSubtours.getSubtourIndexation()[k])==pos){
				distance=distance+CoordUtils.calcDistance(((ActivityImpl)(actslegs.get(k*2))).getCoord(), ((ActivityImpl)(actslegs.get(k*2+2))).getCoord());
				if (distance>this.maxWalkingDistance) {
					return 2;
				}
			}
		}
		if (distance==0) return 0;
		return 1;	
	}
	
	private void changeMode (PlanImpl plan){
		/* Selection of subtour to be changed */
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours(controler.getConfig().planomat());
		planAnalyzeSubtours.run(plan);
		int subtourIndex = (int)(MatsimRandom.getRandom().nextDouble()*planAnalyzeSubtours.getNumSubtours());
		
		/* Selection of mode to be inserted */
		// Current mode
		TransportMode currentMode;
		int j=-1;
		do {
			j+=2;
			currentMode = ((LegImpl)(plan.getPlanElements().get(j))).getMode();
		} while (planAnalyzeSubtours.getSubtourIndexation()[j/2]!=subtourIndex);			
		
		// New mode
		int modeIndex = ((int)(MatsimRandom.getRandom().nextDouble()*this.possibleModes.length));
		while (this.possibleModes[modeIndex].equals(currentMode) ||
				(this.possibleModes[modeIndex].equals(TransportMode.walk) && this.checksubtourDistance2 (plan.getPlanElements(), planAnalyzeSubtours, subtourIndex) == 2) ||
				((!this.possibleModes[modeIndex].equals(TransportMode.walk)) && this.checksubtourDistance2 (plan.getPlanElements(), planAnalyzeSubtours, subtourIndex) == 0)){
			modeIndex++;
			if (modeIndex>=this.possibleModes.length) modeIndex=0;
		}
		
		/* Replacement of mode */
		for (j=1;j<plan.getPlanElements().size();j+=2){
			if (planAnalyzeSubtours.getSubtourIndexation()[j/2]==subtourIndex){
				((LegImpl)(plan.getPlanElements().get(j))).setMode(this.possibleModes[modeIndex]);
			}
		}
	}
}
