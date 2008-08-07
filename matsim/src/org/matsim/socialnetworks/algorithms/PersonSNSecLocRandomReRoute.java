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

package org.matsim.socialnetworks.algorithms;
/**
 * Algorithm to change the location of a random Act in a Plan. The location of one
 * Act is changed randomly to another location from the agent's Knowledge. The
 * route is also re-set. The Act to change is selected randomly according to
 * config settings. Algorithm will return the plan unchanged if an Act of the
 * chosen type is not in the Plan, or if there are no other locations of this type
 * in the agent's Knowledge. If the Act is change, both the old and new plans
 * are retained and the new plan is selected.
 * 
 * TODO: facility types are hard-coded in the conroler! No checks are made whether
 * the facility types in "activities", "acts", and config are identical! They must
 * be or else this code won't work. No warnings will be given. If the facility types
 * do not match perfectly, errors may occur, or the code may run but the result
 * may be buggy.
 * 
 * @author jhackney
 */
import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Knowledge;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.geometry.CoordImpl;


public class PersonSNSecLocRandomReRoute  implements PlanAlgorithm{
	private final String weights;

	private double[] cum_p_factype;
	private NetworkLayer network;
	private TravelCost tcost;
	private TravelTime ttime;
	private String[] factypes;

	public PersonSNSecLocRandomReRoute(String[] factypes, NetworkLayer network, TravelCost tcost, TravelTime ttime) {
		weights = Gbl.getConfig().socnetmodule().getSWeights();
		cum_p_factype = getCumFacWeights(weights);
		this.network=network;
		this.tcost=tcost;
		this.ttime=ttime;
		this.factypes=factypes;
	}

	public void run(Plan plan) {
		replaceRandomFacility(plan);
	}

	private void replaceRandomFacility(Plan plan) {

		// Draw a random number to figure out which of the facility types will be changed for this plan
		// If the plan contains this facility type, replace it with a facility from knowledge,
		//  IFF the knowledge contains an alternative facility for this activity (do not make new
		//  activities in the plan)
		//	Pick one of the facilities in knowledge to replace the one in the plan
		//
		//

		String factype=null;// facility type to switch out
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN		    
		Plan newPlan = person.copySelectedPlan();

		// Note that it is not changed, yet
		boolean changed = false;

//		Pick a type of facility to replace in this plan according to config settings
		double rand = MatsimRandom.random.nextDouble();

		// TODO JH generalize to any length of array of "facility types" for the future
		// in which there can be any number of facility/activity types
		if (rand < cum_p_factype[0]) {
			factype = factypes[0];
		}else if (cum_p_factype[0] <= rand && rand < cum_p_factype[1]) {
			factype = factypes[1];
		}else if (cum_p_factype[1] <= rand && rand < cum_p_factype[2]) {
			factype = factypes[2];
		}else if (cum_p_factype[2] <= rand && rand < cum_p_factype[3]) {
			factype = factypes[3];
		}else {
			factype = factypes[4];
		}

//		Get all instances of this facility type in the plan

// BUGFIX by mrieser:
// OLD CODE:
//		ActIterator planIter= plan.getIteratorAct();
// NEW CODE:
		ActIterator planIter= newPlan.getIteratorAct();
		ArrayList<Act> actsOfFacType= new ArrayList<Act>();
		while(planIter.hasNext()){
			Act nextAct=(Act) planIter.next();
			if(nextAct.getType()== factype){
				actsOfFacType.add(nextAct);
			}
		}

		// Choose a random act from this list. Return the plan unchanged if there are none.
		if(actsOfFacType.size()<1){
			person.setSelectedPlan(plan);
			person.getPlans().remove(newPlan);
			return;
		}else{
			Act newAct = (Act)(actsOfFacType.get(MatsimRandom.random.nextInt(actsOfFacType.size())));

//			Get agent's knowledge
			Knowledge k = person.getKnowledge();

			// Replace with plan.getRandomActivity(type)

//			Pick a random ACTIVITY of this type from knowledge

			List<Activity> actList = k.getActivities(factype);
			Facility fFromKnowledge = actList.get(MatsimRandom.random.nextInt( actList.size())).getFacility();

//			And replace the activity in the chain with it (only changes the facility)

			if(newAct.getLinkId()!=fFromKnowledge.getLink().getId()){
				// If the first activity was chosen, make sure the last activity is also changed
				if(newAct.getType() == plan.getFirstActivity().getType() && newAct.getLink() == plan.getFirstActivity().getLink()){
					Act lastAct = (Act) newPlan.getActsLegs().get(newPlan.getActsLegs().size()-1);
					lastAct.setLink(fFromKnowledge.getLink());
					lastAct.setCoord(fFromKnowledge.getCenter());
					lastAct.setFacility(fFromKnowledge);
				}
				// If the last activity was chosen, make sure the first activity is also changed
				if(newAct.getType() == ((Act)plan.getActsLegs().get(plan.getActsLegs().size()-1)).getType() && newAct.getLink() == ((Act)plan.getActsLegs().get(plan.getActsLegs().size()-1)).getLink()){
					Act firstAct = (Act) newPlan.getFirstActivity();
					firstAct.setLink(fFromKnowledge.getLink());
					firstAct.setCoord(fFromKnowledge.getCenter());
					firstAct.setFacility(fFromKnowledge);
				}
				// Change the activity
//				System.out.println("  ##### Act "+newAct.getRefId()+" of type "+newAct.getType()+" ID "+newAct.getLink().getId()+" was changed for person "+plan.getPerson().getId()+" to "+fFromKnowledge.getLink().getId());
				newAct.setLink(fFromKnowledge.getLink());
				newAct.setCoord(fFromKnowledge.getCenter());
				newAct.setFacility(fFromKnowledge);
				changed = true;
			}

			if(changed){
				//		 loop over all <leg>s, remove route-information
				ArrayList<?> bestactslegs = newPlan.getActsLegs();
				for (int j = 1; j < bestactslegs.size(); j=j+2) {
					Leg leg = (Leg)bestactslegs.get(j);
					leg.setRoute(null);
				}
//				Reset the score to Undefined. Helps to see if the plan was really changed
				newPlan.setScore(Plan.UNDEF_SCORE);

				new PersonPrepareForSim(new PlansCalcRoute(network, tcost, ttime), network).run(newPlan.getPerson());
//				new PlansCalcRoute(network, tcost, ttime).run(newPlan);

//				DEBUG 1 check for null routes
				LegIterator testlegit = newPlan.getIteratorLeg();
				while(testlegit.hasNext()){
					if(((Leg)testlegit.next()).getRoute() == null){
						System.out.println("####### DEBUG PersonSNSecLocRandomReRoute null route replanned");
					}
				}
//				DEBUG 2 check route of this person
				if(Integer.parseInt(person.getId().toString())==3356172){
					System.out.println("#### OUTPLAN: "+newPlan);
				}
//				k.getMentalMap().learnActsActivities(newAct,fFromKnowledge.getActivity(factype));
				person.setSelectedPlan(newPlan);

			}else{
//				System.out.println("   ### newPlan same as old plan");
				person.getPlans().remove(newPlan);
				person.setSelectedPlan(plan);
			}
		}
	}
	private double[] getCumFacWeights(String longString) {
		String patternStr = ",";
		String[] s;
		s = longString.split(patternStr);
		double[] w = new double[s.length];
		w[0]=Double.parseDouble(s[0]);
		double sum = w[0];	
		for (int i = 1; i < s.length; i++) {
			w[i] = Double.parseDouble(s[i])+w[i-1];
			sum=sum+Double.parseDouble(s[i]);
		}
		if (sum > 0) {
			for (int i = 0; i < s.length; i++) {

				w[i] = w[i] / sum;
			}
		} else if (sum < 0) {
			Gbl.errorMsg("At least one weight for the type of information exchange or meeting place must be > 0, check config file.");
		}
		return w;
	}


}
