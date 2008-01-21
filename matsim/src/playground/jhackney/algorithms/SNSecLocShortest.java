/* *********************************************************************** *
 * project: org.matsim.*
 * SNSecLocShortest.java
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

package playground.jhackney.algorithms;

import java.util.List;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.geometry.shared.Coord;

import playground.jhackney.controler.SNController;

public class SNSecLocShortest implements PlanAlgorithmI {

	private final String weights;

	private double[] cum_p_factype;

	public SNSecLocShortest() {
		weights = Gbl.getConfig().socnetmodule().getSWeights();
		cum_p_factype = getCumFacWeights(weights);
	}

	public void run(Plan plan) {
		// TODO Auto-generated method stub
		shortenPlanLength(plan);

	}



	private void shortenPlanLength(Plan plan) {

		// Draw a random number to figure out which of the facility types will be changed for this plan
		// If the plan contains this facility type, replace it with a facility from knowledge,
		//  IFF the knowledge contains an alternative facility for this activity (do not make new
		//  activities in the plan)
		//	Pick one of the facilities in knowledge to replace the one in the plan
		//	Repeat until the shortest path plan has been made
		//
		
		if(!(total(cum_p_factype)>0)){
			return;
		}
		
		String factype=null;// facility type to switch out
		Person person = plan.getPerson();
//		double oldPlanLength=getPlanLength(plan);
//		plan.setScore(oldPlanLength);
		//COPY THE SELECTED PLAN and select it		    
		Plan bestPlan = person.copySelectedPlan();
		person.setSelectedPlan(bestPlan);

//		Figure out which type of facility to replace in this plan
		double rand = Gbl.random.nextDouble();

		if (rand < cum_p_factype[0]) {
			factype = SNController.activityTypesForEncounters[0];
		}else if (cum_p_factype[0] <= rand && rand < cum_p_factype[1]) {
			factype = SNController.activityTypesForEncounters[1];
		}else if (cum_p_factype[1] <= rand && rand < cum_p_factype[2]) {
			factype = SNController.activityTypesForEncounters[2];
		}else if (cum_p_factype[2] <= rand && rand < cum_p_factype[3]) {
			factype = SNController.activityTypesForEncounters[3];
		}else {
			factype = SNController.activityTypesForEncounters[4];
		}

//		Figure out if the agent has knowledge about other facilities of this kind
		Knowledge k = person.getKnowledge();
//		System.out.println("SNSLS Person "+person.getId()+" ");
//		Find all instances of this facility type in the plan and replace facility
		int max = plan.getActsLegs().size();
		for (int i = 0; i < max; i += 2) {

			Act bestAct = (Act)(bestPlan.getActsLegs().get(i));	    
			String type=bestAct.getType();

			if(factype.equals(type)){

//				Pick a random ACTIVITY of this type from knowledge

				List<Activity> activityList = k.getActivities(type);
				Facility fFromKnowledge = activityList.get(Gbl.random.nextInt( activityList.size())).getFacility();

//				And replace the act in the chain with it (only changes the facility)

				bestAct.setLink(fFromKnowledge.getLink());
				Coord newCoord = (Coord) fFromKnowledge.getCenter();
				bestAct.setCoord(newCoord);
				//? set a new RefId here to avoid having this Act,Activity pairing written over
				// in knowledge. Else leave the RefId as it is and let the Activity for with the original
				// Act be forgotten. Note that both Act-Activity pairs will be retained in mapActActivity but
				// only the latest pairing will be in mapActIdActivityId.

				// IF CHAIN LENGTH ON THIS COPIED PLAN IS SHORTER THEN PUT THE CHANGE IN THE PLAN
				double bestPlanLength = getPlanLength(bestPlan);
				double planLength = getPlanLength(plan);

				if(bestPlanLength < planLength){
					//then copyPlan is the selected plan AND the better plan
					// associate this act with the facility in agent's mental map
					k.map.learnActsActivities(bestAct,fFromKnowledge.getActivity(type));
				}else{
					//copyPlan is the selected plan BUT it is worse:
					// unselect it and restore original plan as the selected one
//					person.setSelectedPlan(plan);
					bestPlan=person.copySelectedPlan();
					person.setSelectedPlan(bestPlan);
				}
			}
		}
	}

	private double total(double[] w) {
		// calculate sum of probabilities of changing each facility type
		double sum=0;
		for (int i=0; i<w.length;i++){
			sum=sum+w[i];
		}
		return sum;
	}

	private double[] getCumFacWeights(String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		w[0]=Double.valueOf(s[0]).doubleValue();
		double sum = w[0];	
		for (int i = 1; i < s.length; i++) {
			w[i] = Double.valueOf(s[i]).doubleValue()+w[i-1];
			sum=sum+Double.valueOf(s[i]).doubleValue();
		}
		if (sum > 0) {
			for (int i = 0; i < s.length; i++) {

				w[i] = w[i] / sum;
			}
		}else if (sum < 0) {
			Gbl
			.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}
	public double getPlanLength(Plan plan){

		double length=0.;
		for (int i = 0, max= plan.getActsLegs().size(); i < max-2; i += 2) {
			Act act1 = (Act)(plan.getActsLegs().get(i));
			Act act2 = (Act)(plan.getActsLegs().get(i+2));

			if (act2 != null || act1 != null) {
				double dist = act1.getCoord().calcDistance(act2.getCoord());
				length += dist;
			}
		}
		return length;
	}

}
