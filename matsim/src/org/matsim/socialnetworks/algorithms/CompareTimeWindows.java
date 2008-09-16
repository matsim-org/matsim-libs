package org.matsim.socialnetworks.algorithms;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.scoring.SocScoringFunctionPlan;
import org.matsim.socialnetworks.socialnet.EgoNet;


public class CompareTimeWindows {

	final private static Logger log = Logger.getLogger(CompareTimeWindows.class);

	/**
	 * Calculates a set of statistics about the face to face interactions
	 * at a social event and maps them to the act in the plan
	 * 
	 * Thus you can use this structure to look up the act for an ego and get
	 * the number of other agents who were at the same place at that same time
	 * 
	 * This might be more appropriate somewhere else than in an EventHandler
	 * 
	 * @param plan
	 * @return
	 */
	public static Hashtable<Act,ArrayList<Double>> calculateTimeWindowEventActStats(Hashtable<Facility,ArrayList<TimeWindow>> timeWindowMap) {

		Hashtable<Act,ArrayList<Double>> actStats = new Hashtable<Act,ArrayList<Double>>();
		int count=0;
		// stats(0)=friendFoeRatio
		// stats(1)=nFriends
		// stats(2)=totalTimeWithFriends
		Iterator<Facility> fit=timeWindowMap.keySet().iterator();
		while(fit.hasNext()){
			double friend=0.;
			double foe=0.;
			double totalTimeWithFriends=0;
			TimeWindow tw1 = null;
			TimeWindow tw2 = null;
			Person p1 = null;
			Person p2 = null;

			Facility myFacility=(Facility) fit.next();
			ArrayList<TimeWindow> visits=timeWindowMap.get(myFacility);
			if(!timeWindowMap.keySet().contains(myFacility)){
				log.info(" activityMap does not contain myActivity");
			}
			if(!(visits.size()>0)){
				log.info(" number of visitors not >0");
			}
			// Go through the visits
			for(int i=0; i<visits.size();i++){
				tw1 = visits.get(i);
				p1 = visits.get(i).person;

				for(int j=i+1;j<visits.size();j++){
					p2 = visits.get(j).person;
					tw2 = visits.get(j);
					if(CompareTimeWindows.overlapTimePlaceType(tw1,tw2) && !p1.equals(p2)){
						EgoNet net = p1.getKnowledge().getEgoNet();
						if(net.getAlters().contains(p2)){
							friend++;
							totalTimeWithFriends+=CompareTimeWindows.getTimeWindowOverlapDuration(tw1,tw2);
						}else{
							foe++;
						}
					}

				}			

				if(actStats.isEmpty() || !(actStats.keySet().contains(tw1.act))){
					ArrayList<Double> stats=new ArrayList<Double>();
					actStats.put(tw1.act,stats);
//					log.info("making new act statistic entry for "+ tw1.act.getType());
				}

				if(actStats.containsKey(tw1.act)){
					ArrayList<Double> stats=actStats.get(tw1.act);
					if((friend+foe)==0){
						stats.add((double) 0);
					}else{
						stats.add(friend/(foe+.1*(friend+foe)));
					}
					stats.add(friend);
					stats.add(totalTimeWithFriends);
//					log.info(count+" filling act statistics for "+myFacility.getId()+" "+ tw1.act.getType()+" "+tw1.startTime+" "+tw1.endTime+" "+stats.get(0)+" "+stats.get(1)+" "+stats.get(2));
					count++;
				}
			}
		}

		return actStats;
	}	



	public static boolean overlapTimePlaceType(TimeWindow tw1, TimeWindow tw2){
//		System.out.println("Checking overlap "+act1.getType()+" "+act1.getFacility().getId()+": "+act2.getType()+" "+act2.getFacility().getId());
		if(tw1.act.getFacility().getActivity(tw2.act.getType())==null){
			System.out.println("It's act2 "+tw2.act.getType()+" "+tw1.act.getFacility().getId()+": "+tw2.act.getType()+" "+tw2.act.getFacility().getId());
		}
		if(tw1.act.getFacility().getActivity(tw1.act.getType())==null){
			System.out.println("It's act1 "+tw1.act.getType()+" "+tw1.act.getFacility().getId()+": "+tw2.act.getType()+" "+tw2.act.getFacility().getId());
		}
		Act act1=tw1.act;
		Act act2=tw2.act;
		boolean overlap=false;
		if(act2.getFacility().getActivity(act2.getType()).equals(act1.getFacility().getActivity(act1.getType()))){
			if(act2.getEndTime() >=act1.getStartTime() && act2.getStartTime()<=act1.getEndTime()){
				overlap=true;
			}
		}
		return overlap;
	}

	public static double getTimeWindowOverlapDuration(TimeWindow tw1, TimeWindow tw2) {
		double duration = Math.min(tw1.endTime,tw2.endTime)-Math.max(tw1.startTime,tw2.startTime);
		if(duration<0) duration=0.;
		
		return duration;
	}
}
