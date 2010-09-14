/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRandomReplaceSecLoc.java
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
/**
 * Algorithm to change the location of a random Act in a Plan. The location of one
 * Act is changed randomly to another location from the facilities layer. Thus knowledge
 * of the world is not filtered by or for the Agent in any way. The Act
 * to change is selected randomly according to
 * config settings. Algorithm will return the plan unchanged if an Act of the
 * chosen type is not in the Plan, or if there are no other locations of this type
 * in the facilities. If the Act is changed, both the old and new plans
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.SocNetConfigGroup;


public class PlanRandomReplaceSecLoc  implements PlanAlgorithm{
	private final String weights;

	private final double[] cum_p_factype;
	private final NetworkImpl network;
	private final PersonalizableTravelCost tcost;
	private final PersonalizableTravelTime ttime;
	private final String[] factypes;

	private final ActivityFacilitiesImpl facilities;

	private final Knowledges knowledges;

	public PlanRandomReplaceSecLoc(String[] factypes, NetworkImpl network, ActivityFacilitiesImpl facilities, PersonalizableTravelCost tcost, PersonalizableTravelTime ttime, Knowledges knowledges, SocNetConfigGroup snConfig) {
		weights = snConfig.getSWeights();
		cum_p_factype = getCumFacWeights(weights);
		this.network=network;
		this.facilities=facilities;
		this.tcost=tcost;
		this.ttime=ttime;
		this.factypes=factypes;
		this.knowledges = knowledges;
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
		Plan newPlan = ((PersonImpl) person).copySelectedPlan();

		// Note that it is not changed, yet
		boolean changed = false;

//		Pick a type of facility to replace in this plan according to config settings
		double rand = MatsimRandom.getRandom().nextDouble();

		if (rand < cum_p_factype[0]) {
			factype = factypes[0];
		}else if ((cum_p_factype[0] <= rand) && (rand < cum_p_factype[1])) {
			factype = factypes[1];
		}else if ((cum_p_factype[1] <= rand) && (rand < cum_p_factype[2])) {
			factype = factypes[2];
		}else if ((cum_p_factype[2] <= rand) && (rand < cum_p_factype[3])) {
			factype = factypes[3];
		}else {
			factype = factypes[4];
		}

//		Get all instances of this facility type in the plan

		ArrayList<ActivityImpl> actsOfFacType= new ArrayList<ActivityImpl>();
		for (PlanElement pe : newPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl nextAct=(ActivityImpl) pe;
				if(nextAct.getType()== factype){
					actsOfFacType.add(nextAct);
				}
			}
		}

		// Choose a random act from this list. Return the plan unchanged if there are none.
		if(actsOfFacType.size()<1){
			((PersonImpl) person).setSelectedPlan(plan);
			person.getPlans().remove(newPlan);
			return;
		}else{
			ActivityImpl newAct = (actsOfFacType.get(MatsimRandom.getRandom().nextInt(actsOfFacType.size())));

//			Get agent's knowledge
			KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(person.getId());

			// Replace with plan.getRandomActivity(type)

//			Pick a random ACTIVITY of this type from Facilities

//			Id facId=new Id(Gbl.random.nextInt(facs.getFacilities().size()));
			int facId=MatsimRandom.getRandom().nextInt(facilities.getFacilitiesForActivityType(factype).size());
			ActivityFacilityImpl f= (ActivityFacilityImpl) facilities.getFacilitiesForActivityType(factype).values().toArray()[facId];

//			Facility f = facs.getFacilities().get(new Id(Gbl.random.nextInt(facs.getFacilities().size())));
//			And replace the activity in the chain with it (only changes the facility)

			if(!newAct.getLinkId().equals(f.getLinkId())) {
				// If the first activity was chosen, make sure the last activity is also changed
				if((newAct.getType() == ((PlanImpl) plan).getFirstActivity().getType()) && (newAct.getLinkId().equals(((PlanImpl) plan).getFirstActivity().getLinkId()))){
					ActivityImpl lastAct = (ActivityImpl) newPlan.getPlanElements().get(newPlan.getPlanElements().size()-1);
//					Act lastAct = (Act) plan.getActsLegs().get(plan.getActsLegs().size()-1);
					lastAct.setLinkId(f.getLinkId());
					lastAct.setCoord(f.getCoord());
					lastAct.setFacilityId(f.getId());
				}
				// If the last activity was chosen, make sure the first activity is also changed
				if((newAct.getType() == ((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType()) && (newAct.getLinkId().equals(((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getLinkId()))){
					ActivityImpl firstAct = (ActivityImpl) ((PlanImpl) newPlan).getFirstActivity();
					firstAct.setLinkId(f.getLinkId());
					firstAct.setCoord(f.getCoord());
					firstAct.setFacilityId(f.getId());
				}
				// Change the activity
//				System.out.println("  ##### Act "+newAct.getRefId()+" of type "+newAct.getType()+" ID "+newAct.getLink().getId()+" was changed for person "+plan.getPerson().getId()+" to "+fFromKnowledge.getLink().getId());
				newAct.setLinkId(f.getLinkId());
				newAct.setCoord(f.getCoord());
				newAct.setFacilityId(f.getId());
				changed = true;
			}

			if(changed == true){
				//		 loop over all <leg>s, remove route-information
				List<? extends PlanElement> bestactslegs = newPlan.getPlanElements();
//				ArrayList<?> bestactslegs = plan.getActsLegs();
				for (int j = 1; j < bestactslegs.size(); j=j+2) {
					LegImpl leg = (LegImpl)bestactslegs.get(j);
					leg.setRoute(null);
				}
//				Reset the score to Undefined. Helps to see if the plan was really changed
				newPlan.setScore(Double.NaN);

				new PersonPrepareForSim(new PlansCalcRoute(null, network, tcost, ttime, new DijkstraFactory()), network).run(newPlan.getPerson());

//				Not needed with new change to Act --> Facility JH 7.2008
//				k.getMentalMap().learnActsActivities(newAct,f.getActivity(factype));
				((PersonImpl) person).setSelectedPlan(newPlan);
//				person.setSelectedPlan(plan);
				// Remove previous plan
//				person.getPlans().remove(plan);
			}else{
//				System.out.println("   ### newPlan same as old plan");
				person.getPlans().remove(newPlan);
				((PersonImpl) person).setSelectedPlan(plan);
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

