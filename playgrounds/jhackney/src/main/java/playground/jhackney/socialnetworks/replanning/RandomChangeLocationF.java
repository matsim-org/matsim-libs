/* *********************************************************************** *
 * project: org.matsim.*
 * SecLocRandom.java
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

package playground.jhackney.socialnetworks.replanning;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.SocNetConfigGroup;
import playground.jhackney.socialnetworks.mentalmap.MentalMap;

/**
 * Sample replanning strategy to change activity location:
 * uses all facilities
 *
 * @author jhackney
 *
 */

public class RandomChangeLocationF  implements PlanAlgorithm{
	private final String weights;

	private final double[] cum_p_factype;
	private final Network network;
	private final PersonalizableTravelCost tcost;
	private final TravelTime ttime;
	private final String[] factypes;
	private final ActivityFacilities facs;

	public RandomChangeLocationF(String[] factypes, Network network, PersonalizableTravelCost tcost, TravelTime ttime, ActivityFacilities facs, SocNetConfigGroup snConfig) {
		weights = snConfig.getSWeights();
		cum_p_factype = getCumFacWeights(weights);
		this.network=network;
		this.tcost=tcost;
		this.ttime=ttime;
		this.factypes=factypes;
		this.facs=facs;
	}

	public void run(Plan plan) {
		replaceRandomFacility(plan);
	}

	private void replaceRandomFacility(Plan plan) {

		// Draw a random number to figure out which of the facility types will be changed for this plan
		// If the plan contains this facility type, replace it with a facility from FACILITIES,
		//  IFF the FACILITIES contains an alternative facility for this activity (do not make new
		//  activities in the plan)
		//	Pick one of the facilities in FACILITIES to replace the one in the plan
		//
		//
		//
//		System.out.println("########## SNSecLocRandom ");
		String factype=null;// facility type to switch out
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN
		Plan newPlan = ((PersonImpl) person).copySelectedPlan();

		// Note that it is not changed, yet
		boolean changed = false;

//		Pick a type of facility to replace in this plan according to config settings
		double rand = MatsimRandom.getRandom().nextDouble();

		if (rand < cum_p_factype[0] && cum_p_factype[0]!=0 ) {
			factype = factypes[0];
		}else if (cum_p_factype[0] <= rand && rand < cum_p_factype[1] && (cum_p_factype[1]!=cum_p_factype[0])) {
			factype = factypes[1];
		}else if (cum_p_factype[1] <= rand && rand < cum_p_factype[2] && (cum_p_factype[2]!=cum_p_factype[1])) {
			factype = factypes[2];
		}else if (cum_p_factype[2] <= rand && rand < cum_p_factype[3] && (cum_p_factype[3]!=cum_p_factype[2])) {
			factype = factypes[3];
		}else if (cum_p_factype[3] <= rand && rand < cum_p_factype[4] && (cum_p_factype[4]!=cum_p_factype[3])) {
			factype = factypes[4];
		}

//		Get all instances of this facility type in the plan

		ArrayList<ActivityImpl> actsOfFacType= new ArrayList<ActivityImpl>();
		for (PlanElement pe : newPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl nextAct=(ActivityImpl) pe;
				if(nextAct.getType().equals(factype)){
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

			// Replace with plan.getRandomActivity(type)

//			Pick a random ACTIVITY of this type from knowledge

//			List<Activity> actList = k.getActivities(factype);

			if(((ActivityFacilitiesImpl) facs).getFacilitiesForActivityType(newAct.getType()).size()>0){
				int index=MatsimRandom.getRandom().nextInt(((ActivityFacilitiesImpl) facs).getFacilitiesForActivityType(newAct.getType()).size());
				ActivityFacilityImpl fFromFacilities=(ActivityFacilityImpl) ((ActivityFacilitiesImpl) facs).getFacilitiesForActivityType(newAct.getType()).values().toArray()[index];

//				And replace the activity in the chain with it (only changes the facility)

				if(!newAct.getLinkId().equals(fFromFacilities.getLinkId())){
					// If the first activity was chosen, make sure the last activity is also changed
					if(newAct.getType() == ((PlanImpl) plan).getFirstActivity().getType() && newAct.getLinkId().equals(((PlanImpl) plan).getFirstActivity().getLinkId())){
						ActivityImpl lastAct = (ActivityImpl) newPlan.getPlanElements().get(newPlan.getPlanElements().size()-1);
						lastAct.setLinkId(fFromFacilities.getLinkId());
						lastAct.setCoord(fFromFacilities.getCoord());
						lastAct.setFacilityId(fFromFacilities.getId());
					}
					// If the last activity was chosen, make sure the first activity is also changed
					if(newAct.getType() == ((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getType() && newAct.getLinkId().equals(((ActivityImpl)plan.getPlanElements().get(plan.getPlanElements().size()-1)).getLinkId())){
						ActivityImpl firstAct = (ActivityImpl) ((PlanImpl) newPlan).getFirstActivity();
						firstAct.setLinkId(fFromFacilities.getLinkId());
						firstAct.setCoord(fFromFacilities.getCoord());
						firstAct.setFacilityId(fFromFacilities.getId());
					}
					// Change the activity
//					System.out.println("  ##### Act at "+newAct.getFacility().getId()+" of type "+newAct.getType()+" ID "+newAct.getLink().getId()+" was changed for person "+plan.getPerson().getId()+" to "+fFromKnowledge.getLink().getId());
					newAct.setLinkId(fFromFacilities.getLinkId());
					newAct.setCoord(fFromFacilities.getCoord());
					newAct.setFacilityId(fFromFacilities.getId());
					((MentalMap)person.getCustomAttributes().get(MentalMap.NAME)).addActivity(fFromFacilities.getActivityOptions().get(factype));
					changed = true;
				}
			}

			if(changed){
				//		 loop over all <leg>s, remove route-information
				List<? extends PlanElement> bestactslegs = newPlan.getPlanElements();
				for (int j = 1; j < bestactslegs.size(); j=j+2) {
					LegImpl leg = (LegImpl)bestactslegs.get(j);
					leg.setRoute(null);
				}
//				Reset the score.
				newPlan.setScore(null);

				new PersonPrepareForSim(new PlansCalcRoute(null, network, tcost, ttime, new DijkstraFactory()), (NetworkLayer) network).run(newPlan.getPerson());
//				new PlansCalcRoute(network, tcost, ttime).run(newPlan);

				((PersonImpl) person).setSelectedPlan(newPlan);
//				System.out.println("   ### new location for "+person.getId()+" "+newAct.getType());

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

