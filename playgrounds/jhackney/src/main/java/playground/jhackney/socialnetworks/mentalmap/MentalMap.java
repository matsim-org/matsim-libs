/* *********************************************************************** *
 * project: org.matsim.*
 * MentalMap2.java
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

package playground.jhackney.socialnetworks.mentalmap;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.world.MappedLocation;

import playground.jhackney.socialnetworks.algorithms.SortHashMapByValue;
import playground.jhackney.socialnetworks.io.ActivityActReader;

/**
 *
 * @author fmarchal, jhackney
 *
 */
public class MentalMap {
	
	public static final String NAME = "MentalMap";

	// Class to manage the knowledge.
	// Activities and Acts need to point to each other consistently
	//  across iterations of replanning. An agent should not forget in
	//  which facility he performed his Act. Indeed at the moment, which
	//  facility is chosen does not affect the mobility calculation or
	//  the standard MATSim utility, but it will be important for social
	//  networks.
	//
	// In a Plan, each Act takes place in one Facility.
	// One Facility can have multiple Acts, however, since Facilities can have several Activities.
	// The Act will change each iteration of the mobility simulation (start/end times).
	// Its mapping to the Activity is stored so it can be found again

//	Map of activities and acts: this is like a memory of having been someplace
//	private LinkedHashMap<Activity,Act> mapActivityAct = new LinkedHashMap<Activity,Act>();
//	private LinkedHashMap<Integer, Act> actIdAct = new LinkedHashMap<Integer, Act>();

//	Map of act and activity ID numbers. Reverse of above. Acts change so we use Id's
//	private LinkedHashMap<Act,Id> mapActActivityId = new LinkedHashMap<Act,Id>();


//	The activity score
	private final LinkedHashMap<ActivityOptionImpl, Double> activityScore = new LinkedHashMap<ActivityOptionImpl, Double>();

//	Total maximum number of activities (locations + action) an agent can remember

	private KnowledgeImpl knowledge = null;
	private final Logger log= Logger.getLogger(MentalMap.class);
	private final Network network;

	public MentalMap(KnowledgeImpl knowledge, Network network){
		this.knowledge=knowledge;
		this.network = network;
	}

	public void initializeActActivityMapRandom (Plan myPlan){

//		Associate each act in the plan with a random facility on the link

		for (PlanElement pe : myPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl myAct = (ActivityImpl) pe;
				if(myAct.getFacilityId()==null){ // new Acts are assigned a facility in the Plans file
					ActivityOptionImpl myActivity = null;
					// If there is already knowledge in the initial plans file, use it
					if(this.knowledge.getActivities(myAct.getType()).size()>0){
						myActivity=this.knowledge.getActivities(myAct.getType()).get(MatsimRandom.getRandom().nextInt(this.knowledge.getActivities(myAct.getType()).size()));
						myAct.setFacilityId(myActivity.getFacility().getId());
						//TODO JH add logic to label this activity primary or secondary
						this.knowledge.addActivity(myActivity, false);
//					learnActsActivities(myAct,myActivity);
					}
					
					// Else the activity is null and we choose an activity to assign to the act
					Link myLink = this.network.getLinks().get(myAct.getLinkId());
					// These Locations are facilities by the new convention
					Collection<MappedLocation> locations = ((LinkImpl) myLink).getUpMapping().values();
					// These Objects are facilities by convention
					Object[] facs =  locations.toArray();
					// Assign a random activity (a facility) on the link to the act
					// thus giving it in effect a street address
					while(myActivity==null){
						int k = MatsimRandom.getRandom().nextInt(facs.length);
						ActivityFacilityImpl f = (ActivityFacilityImpl) facs[k];
						myActivity = f.getActivityOptions().get(myAct.getType());
						if(myActivity!=null){
							myAct.setFacilityId(myActivity.getFacility().getId());
							//TODO JH add logic to label this activity primary or secondary
							this.knowledge.addActivity(myActivity,false);
						}
					}
				}
			}
		}
	}

	public void initializeActActivityMapFromFile(Plan myPlan, ActivityFacilities facilities, ActivityActReader aar){


		if(aar==null) return;
//		this.log.info("READING ACT-ACTIVITY MAP FROM FILE PERSON "+myPlan.getPerson().getId());
		for (PlanElement pe : myPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl myAct = (ActivityImpl) pe;
				
				TreeMap<Id,String> nextFac = aar.getNextPoint();
				Id myFacilityId = nextFac.firstKey();
//			String myActivityType=nextFac.get(myFacilityId);
				String myActivityType=myAct.getType();
				
				ActivityFacility fac = facilities.getFacilities().get(myFacilityId);
//			myAct.setFacility(fac);
//			this.knowledge.addActivity(fac.getActivity(myActivityType));
				//TODO JH apply some logic to label this a primary or secondary location
				ActivityOptionImpl myActivity=fac.getActivityOptions().get(myActivityType);
				this.knowledge.addActivity(myActivity,false);
			}
		}
	}

	public void prepareActs(Plan myPlan){

//		Tidy the acts up so they correspond to the expectations of the social net module.
//		First, change the types to be the same as the facility types.
//		Should not have to be done: adapt SocialNetworks code to use only the first letter
//		or else a standard MATSim "type" when this becomes established

		for (PlanElement pe : myPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl myAct = (ActivityImpl) pe;
				
				String type="none";
				char typechar=myAct.getType().charAt(0);
				if(typechar=='h'){
					type="home";
				}else if(typechar=='w'){
					type="work";
				}else if(typechar=='l'){
					type="leisure";
				}else if(typechar=='s'){
					type="shop";
				}else if(typechar=='e'){
					type="education";
				}else
					Gbl.errorMsg("Activity type "+ typechar +" not known");
				myAct.setType(type);
			}
		}
	}

	private void forgetActivity (ActivityOptionImpl myactivity){

		if(this.activityScore.containsKey(myactivity)){
			this.activityScore.remove(myactivity);
		}
		//TODO JH 6.2008 if you remove an activity from Knowledge you must reset the facility of ALL
		// acts pointing to it, not just the one passed in, here. This is not checked, here. Make sure
		// you have checked your mapping before removing the Activity or you will get null pointers
		// from Act --> Facility.
		this.knowledge.removeActivity(myactivity);
	}
	/**
	 * ManageMemory is an algorithm that could be written to serve many purposes. Here, it tags
	 * activities in excess of what is needed by an agent and deletes them from the agent's knowledge.
	 *  
	 * @param max
	 * @param myPlans
	 */
	public void manageMemory(int max, List<? extends Plan> myPlans){

		if(myPlans.get(0).getPlanElements().size()*myPlans.size()/2 >max){
			this.log.info("No activities removed from memory");
			return;
		}

		if(this.knowledge.getActivities().size()>max){
			// Mark the activities associated with all plans in memory
			// so that they won't be deleted

			ArrayList<Id> currentActivities = new ArrayList<Id>();
			for (Plan myPlan : myPlans) {
				for (PlanElement pe : myPlan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						currentActivities.add(act.getFacilityId());
					}
				}
			}

//			Sort the activities by score so that they can be managed

			SortHashMapByValue shmbv = new SortHashMapByValue();
//			LinkedHashMap<Activity, ?> sortedScores = SortHashMapByValue.makeSortedMap(this.activityScore);
			LinkedHashMap<ActivityOptionImpl, ?> sortedScores = shmbv.makeSortedMap(this.activityScore);

			// Remove activities if there are too many, but keep one activity
			// for each act in the current plan. Iterator goes by score.

			int numToForget = this.knowledge.getActivities().size()-max-currentActivities.size();
			// Avoid concurrent modification errors
			// Add to the "forgetlist" the correct number of activities to forget
			// Since these activities are sorted by score, just take the first ones
			ArrayList<ActivityOptionImpl> forgetList=new ArrayList<ActivityOptionImpl>();
			int counter=0;

			// If there are too many activities 
//			for (Enumeration<Activity> e = sortedScores.keys() ; e.hasMoreElements() ;) {
//				Activity myactivity=e.nextElement();
				Set<ActivityOptionImpl> myActivities=sortedScores.keySet();
				Iterator<ActivityOptionImpl> ait= myActivities.iterator();
				while(ait.hasNext()) {
					ActivityOptionImpl myactivity=ait.next();
//				double score=(Double) sortedScores.get(myactivity);
				// note which activity to forget

				if(counter<=numToForget && !currentActivities.contains(myactivity)){
					forgetList.add(myactivity);
					counter++;
				}
			}

			Iterator<ActivityOptionImpl> forget=forgetList.iterator();
			while(forget.hasNext()){
				ActivityOptionImpl myactivity=forget.next();
				forgetActivity(myactivity);
			}
		}
//		System.out.println(this.getClass()+" NumActivities2 "+this.knowledge.getActivities().size()+" "+this.activityScore.values().size());
	}

	public void addActivity(ActivityOptionImpl myActivity){
		// Adds unmapped activity to mental map without associating it with an act
		//TODO JH add logic to label this activity primary or secondary
		this.knowledge.addActivity(myActivity,false);
		setActivityScore(myActivity);
	}

	public void setActivityScore(ActivityOptionImpl myActivity){
//		Initializes the score of a newly learned or discovered activity to 0
//		Adds one to the score of the activity each time it is accessed.
//		Supposed to simulate how frequently it is thought of by agent.

		double x=0;
		if(!(this.activityScore.containsKey(myActivity))){
			this.activityScore.put(myActivity,x);
		}else
			if(this.activityScore.containsKey(myActivity)){
				x=this.activityScore.get(myActivity)+1;
				this.activityScore.remove(myActivity);
				this.activityScore.put(myActivity,x);
			}
	}

	public ActivityImpl getActFromActivity (PersonImpl person, ActivityOptionImpl myActivity){
		ActivityImpl myAct=null;
		for (Plan myPlan : person.getPlans()) {
			for (PlanElement pe : myPlan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if(act.getFacilityId()==myActivity.getFacility().getId()){
						myAct=act;
					}
				}
			}
		}
		return myAct;
	}

}


