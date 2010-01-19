package playground.jhackney.socialnetworks.algorithms;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;

public class CompareActs {

	/**
	 * If parts of the acts take place at the same time
	 * 
	 * @param act1
	 * @param act2
	 * @return
	 */
	public static boolean overlapTime(ActivityImpl act1, ActivityImpl act2){
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
	public static boolean overlapTimePlace(ActivityImpl act1, ActivityImpl act2){
		boolean overlap=false;
		if(act2.getFacilityId().equals(act1.getFacilityId())){
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
	public static boolean overlapTimePlaceType(ActivityImpl act1, ActivityImpl act2, ActivityFacilities facilities){
		ActivityFacilityImpl af1 = (ActivityFacilityImpl) facilities.getFacilities().get(act1.getFacilityId());
		ActivityFacilityImpl af2 = (ActivityFacilityImpl) facilities.getFacilities().get(act2.getFacilityId());
//		System.out.println("Checking overlap "+act1.getType()+" "+act1.getFacility().getId()+": "+act2.getType()+" "+act2.getFacility().getId());
		if(af2.getActivityOptions().get(act2.getType())==null){
			System.out.println("It's act2 "+act1.getType()+" "+act1.getFacilityId()+": "+act2.getType()+" "+act2.getFacilityId());
		}
		if(af1.getActivityOptions().get(act1.getType())==null){
			System.out.println("It's act1 "+act1.getType()+" "+act1.getFacilityId()+": "+act2.getType()+" "+act2.getFacilityId());
		}
		boolean overlap=false;
		if(af2.getActivityOptions().get(act2.getType()).equals(af1.getActivityOptions().get(act1.getType()))){
			if(act2.getEndTime() >=act1.getStartTime() && act2.getStartTime()<=act1.getEndTime()){
				overlap=true;
			}
		}
		return overlap;
	}
}
