package playground.jhackney.replanning;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.BasicPlanImpl.ActIterator;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.socialnetworks.algorithms.CompareTimeWindows;
import org.matsim.socialnetworks.mentalmap.TimeWindow;
import org.matsim.socialnetworks.socialnet.EgoNet;

public class SNAdjustTimes implements PlanAlgorithm {

	private Controler controler;
	private final Logger log = Logger.getLogger(SNAdjustTimes.class);

	public SNAdjustTimes(Controler controler){
		this.controler=controler;
	}
	public void run(Plan plan) {
		adjustDepartureTimes(plan);
	}

	private void adjustDepartureTimes(Plan plan) {
		Person person = plan.getPerson();

		//COPY THE SELECTED PLAN		    
		Plan newPlan = person.copySelectedPlan();

		ActIterator planIter= newPlan.getIteratorAct();

		while(planIter.hasNext()){
			Act thisAct=(Act) planIter.next();
			// Ideally,
			// last Act new departure time =
			// last Act current departure time +
			// average arrival time of friends at thisAct -
			// this Act current arrival time

			// Might be easier to set start time of thisAct to the
			// average arrival time of friends at thisAct

			this.log.info("old "+thisAct.getStartTime());
			thisAct.setStartTime(getAvgFriendArrTime(thisAct));
			this.log.info("new "+thisAct.getStartTime());

		}

		newPlan.setScore(Plan.UNDEF_SCORE);
		person.setSelectedPlan(newPlan);
	}
	private double getAvgFriendArrTime(Act act) {
		Hashtable<Facility,ArrayList<TimeWindow>> twm = controler.getTwm();
		int count=0;
		double avgStartTime=0;
		TimeWindow tw1 = null;
		TimeWindow tw2 = null;
		Person p1 = null;
		Person p2 = null;

		Facility actFacility=act.getFacility();

		if(!twm.keySet().contains(actFacility)){
			log.error(" activityMap does not contain myActivity");
		}
		ArrayList<TimeWindow> visits=twm.get(actFacility);
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
				if(CompareTimeWindows.overlapTimePlaceType(tw1,tw2) && !p1.equals(p2)){
					EgoNet net = p1.getKnowledge().getEgoNet();
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
