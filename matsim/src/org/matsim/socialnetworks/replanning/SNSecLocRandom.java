/* *********************************************************************** *
 * project: org.matsim.*
 * SNSecLocRandom.java
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

package org.matsim.socialnetworks.replanning;

import java.util.List;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Knowledge;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.socialnetworks.controler.SNControllerListener;
import org.matsim.utils.geometry.shared.Coord;


public class SNSecLocRandom  implements PlanAlgorithmI{
    private final String weights;

    private double[] cum_p_factype;

    public SNSecLocRandom() {
	weights = Gbl.getConfig().socnetmodule().getSWeights();
	cum_p_factype = getCumFacWeights(weights);
    }

    public void run(Plan plan) {
	// TODO Auto-generated method stub
	replaceRandomFacility(plan);
    }

    private void replaceRandomFacility(Plan plan) {

	// Draw a random number to figure out which of the facility types will be changed for this plan
	// If the plan contains this facility type, replace it with a facility from knowledge,
	//  IFF the knowledge contains an alternative facility for this activity (do not make new
	//  activities in the plan)
	//	Pick one of the facilities in knowledge to replace the one in the plan
	//
	//The changed plan will be kept only if the score is better (depends on scoring strategy)
	//
	String factype=null;// facility type to switch out
	Person person = plan.getPerson();
	//COPY THE SELECTED PLAN and select it		    
	Plan bestPlan = person.copySelectedPlan();

//	Figure out which type of facility to replace in this plan
	double rand = Gbl.random.nextDouble();

	if (rand < cum_p_factype[0]) {
	    factype = SNControllerListener.activityTypesForEncounters[0];
	}else if (cum_p_factype[0] <= rand && rand < cum_p_factype[1]) {
	    factype = SNControllerListener.activityTypesForEncounters[1];
	}else if (cum_p_factype[1] <= rand && rand < cum_p_factype[2]) {
	    factype = SNControllerListener.activityTypesForEncounters[2];
	}else if (cum_p_factype[2] <= rand && rand < cum_p_factype[3]) {
	    factype = SNControllerListener.activityTypesForEncounters[3];
	}else {
	    factype = SNControllerListener.activityTypesForEncounters[4];
	}

//	Figure out if the agent has knowledge about other facilities of this kind
	Knowledge k = person.getKnowledge();

//	Find all instances of this facility type in the plan
	int max = plan.getActsLegs().size();
	for (int i = 0; i < max; i += 2) {

	    Act bestAct = (Act)(bestPlan.getActsLegs().get(i));	    
	    String type=bestAct.getType();

	    // Replace with plan.getRandomActivity(type)
	    if(factype.equals(type)){

//		Pick a random ACTIVITY of this type from knowledge

		List<Activity> actList = k.getActivities(type);
		Facility fFromKnowledge = actList.get(Gbl.random.nextInt( actList.size())).getFacility();

//		And replace the activity in the chain with it (only changes the facility)

		bestAct.setLink(fFromKnowledge.getLink());
		Coord newCoord = (Coord) fFromKnowledge.getCenter();
		bestAct.setCoord(newCoord);

		k.map.learnActsActivities(bestAct.getRefId(),fFromKnowledge.getActivity(type));
	    }
	}
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
	} else if (sum < 0) {
	    Gbl
	    .errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
	}
	return w;
    }
}
