package org.matsim.socialnetworks.algorithms;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.plans.Act;

public class CompareActsEvents implements EventHandlerAgentArrivalI {

	double starttime=0;
	double endtime=0;
	/**
	 * If parts of the acts take place at the same time
	 *
	 * @param act1
	 * @param act2
	 * @return
	 */
	public static boolean overlapTime(Act act1, Act act2){
		//TODO get the events corresponding to act1 and act2 and use these arrival and end times instead
		// of the times from the Plan
		boolean overlap=false;
			if(act2.getEndTime() >=act1.getStartTime() && act2.getStartTime()<=act1.getEndTime()){
				overlap=true;
		}
		return overlap;
	}
	/**
	 * If the acts take place at the same facility and overlap in time
	 *
	 * @param act1
	 * @param act2
	 * @return
	 */
	public static boolean overlapTimePlace(Act act1, Act act2){
		//TODO get the events corresponding to act1 and act2 and use these arrival and end times instead
		// of the times from the Plan
		boolean overlap=false;
		if(act2.getFacility().equals(act1.getFacility())){
			if(act2.getEndTime() >=act1.getStartTime() && act2.getStartTime()<=act1.getEndTime()){
				overlap=true;
			}
		}
		return overlap;
	}
	/**
	 * If the acts take place at the same facility and overlap in time
	 * and are the same type
	 *
	 * @param act1
	 * @param act2
	 * @return
	 */
	public static boolean overlapTimePlaceType(Act act1, Act act2){
		//TODO get the events corresponding to act1 and act2 and use these arrival and end times instead
		// of the times from the Plan
//		System.out.println("Checking overlap "+act1.getType()+" "+act1.getFacility().getId()+": "+act2.getType()+" "+act2.getFacility().getId());
		if(act2.getFacility().getActivity(act2.getType())==null){
			System.out.println("It's act2 "+act1.getType()+" "+act1.getFacility().getId()+": "+act2.getType()+" "+act2.getFacility().getId());
		}
		if(act1.getFacility().getActivity(act1.getType())==null){
			System.out.println("It's act1 "+act1.getType()+" "+act1.getFacility().getId()+": "+act2.getType()+" "+act2.getFacility().getId());
		}
		boolean overlap=false;
		if(act2.getFacility().getActivity(act2.getType()).equals(act1.getFacility().getActivity(act1.getType()))){
			if(act2.getEndTime() >=act1.getStartTime() && act2.getStartTime()<=act1.getEndTime()){
				overlap=true;
			}
		}
		return overlap;
	}

	public void handleEvent(EventAgentArrival event) {
		// TODO Auto-generated method stub
		this.starttime=event.time;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
}

