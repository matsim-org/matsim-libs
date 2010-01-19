package playground.jhackney.replanning;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.jhackney.socialnetworks.algorithms.CompareTimeWindows;
import playground.jhackney.socialnetworks.mentalmap.TimeWindow;
import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class SNAdjustTimes implements PlanAlgorithm {

//	private Controler controler;
	private final playground.jhackney.controler.SNController3 controler;
	private final Logger log = Logger.getLogger(SNAdjustTimes.class);

	public SNAdjustTimes(playground.jhackney.controler.SNController3 controler){
		this.controler=controler;
	}
	public void run(Plan plan) {
		adjustDepartureTimes(plan);
	}

	private void adjustDepartureTimes(Plan plan) {
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN		    
		Plan newPlan = ((PersonImpl) person).copySelectedPlan();

		for (PlanElement pe : newPlan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl thisAct=(ActivityImpl) pe;
				// Ideally,
				// last Act new departure time =
				// last Act current departure time +
				// average arrival time of friends at thisAct -
				// this Act current arrival time
				
				// Might be easier to set start time of thisAct to the
				// average arrival time of friends at thisAct
				
//			this.log.info("old "+thisAct.getStartTime());
				thisAct.setStartTime(getAvgFriendArrTime(thisAct));
//			this.log.info("new "+thisAct.getStartTime());
			}
		}

		newPlan.setScore(null);
		((PersonImpl) person).setSelectedPlan(newPlan);
	}
	private double getAvgFriendArrTime(ActivityImpl act) {
		LinkedHashMap<Id,ArrayList<TimeWindow>> twm = controler.getTwm();
		int count=0;
		double avgStartTime=0;
		TimeWindow tw1 = null;
		TimeWindow tw2 = null;
		PersonImpl p1 = null;
		PersonImpl p2 = null;

		if(!twm.keySet().contains(act.getFacilityId())){
			log.error(" activityMap does not contain myActivity");
		}
		ArrayList<TimeWindow> visits=twm.get(act.getFacilityId());
		if(!(visits.size()>0)){
			log.error(" number of visitors not >0");
		}
		// Go through all agents who passed through this facility
		for(int i=0; i<visits.size();i++){
			tw1 = visits.get(i);
			p1 = visits.get(i).person;
			avgStartTime+=tw1.startTime;
			count++;

			// Match the activity type and time window overlap
			for(int j=i+1;j<visits.size();j++){
				p2 = visits.get(j).person;
				tw2 = visits.get(j);

				//Check if the overlapping agents are friends and sum the arrival times
				if(CompareTimeWindows.overlapTimePlaceType(tw1,tw2,this.controler.getFacilities()) && !p1.equals(p2)){
					EgoNet net = (EgoNet)p1.getCustomAttributes().get(EgoNet.NAME);
					if(net.getAlters().contains(p2)){
						avgStartTime+=tw2.startTime;
						count++;
					}
				}
			}
		}
		avgStartTime=avgStartTime/count;
		return avgStartTime;
	}

}
