package playground.southafrica.freight.digicore.containers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class DigicoreChain extends ArrayList<DigicoreChainElement>{
	
	private static final long serialVersionUID = 1L;

	public DigicoreChain() {
		
	}
	
	
	/**
	 * Returns the chain duration in seconds as the time difference between the 
	 * last major activity's start time and the first major activity's end time.
	 * @return duration (in seconds).
	 */
	public double getDuration(){
		return this.getLastMajorActivity().getStartTime() - this.getFirstMajorActivity().getEndTime();
	}
	
	
	/** Returns the number of minor activities. The two major activities at the 
	 * start and end of the chain are ignored.
	 * @return number of minor activities.
	 */
	public int getNumberOfMinorActivities(){
		return this.size() - 2;
	}
	
	
	/**
	 * Returns only the minor activities.
	 */
	public List<DigicoreActivity> getMinorActivities(){
		List<DigicoreActivity> minors = new ArrayList<DigicoreActivity>();
		for(DigicoreChainElement dce : this){
			if(dce instanceof DigicoreActivity){
				DigicoreActivity a = (DigicoreActivity)dce;
				if(a.getType().equalsIgnoreCase("minor")){
					minors.add(a);
				}
			}
		}
		return minors;
	}
	
	
	/**
	 * Still quite primitive, but the sum of the straight-line distances between
	 * all consecutive activity-pairs.
	 * @return distance in meters (assuming the {@link CoordinateReferenceSystem}
	 * is projected, and the unit of measure is `meters', such as in `WGS84_UTM35S'
	 * and `WGS84_SA_Albers'.
	 */
	public Double getDistance(){
		double distance = 0.0;
		List<DigicoreActivity> allActivities = getAllActivities();
		for(int i = 0; i < allActivities.size()-1; i++){
			distance += CoordUtils.calcEuclideanDistance(
					allActivities.get(i).getCoord(), 
					allActivities.get(i+1).getCoord());
		}
		return distance;
	}
	
	
//	/**
//	 * Returns the estimated vehicle-kilometers (vkm) traveled using the shortest
//	 * path between the activities. For each activity, the network node closest 
//	 * to the activity is used.
//	 * TODO Create a test!!!
//	 * 
//	 * <h5>Note:</h5> Use sparingly! I don't know what the computational burden is
//	 * of creating a router every time this method is called (JWJ 201207).<br><br>
//	 * @param  
//	 */
//	public double getEstimatedVkm(DigicoreNetworkRouterFactory routerFactory){
//		AStarLandmarks router = routerFactory.createRouter();
//		double vkm = 0;
//		for(int i = 0; i < this.size()-1; i++){
//			Node fromNode = routerFactory.getNetwork().getNearestNode(this.getAllActivities().get(i).getCoord());
//			Node toNode = routerFactory.getNetwork().getNearestNode(this.getAllActivities().get(i+1).getCoord());
//			double startTime = this.getAllActivities().get(i).getEndTime();
//			Path path = router.calcLeastCostPath(fromNode, toNode, startTime, null, null);
//			vkm += path.travelCost;
//		}
//		return vkm;
//	}
	
	
	/**
	 * Checks if a chain is complete.
	 * @return true if the chain contains at least two {@link DigicoreActivity}s,
	 * and both the first and last {@link DigicoreActivity} is of type "major",
	 * and the chain is alternating between a {@link DigicoreActivity} and 
	 * {@link DigicoreTrace}.
	 */
	public boolean isComplete(){
		List<DigicoreActivity> activities = getAllActivities();
		boolean isComplete = false;
		
		/* Check that the sequence of activities starts and ends with a 'major'
		 * activity. */
		boolean startsAndEndsWithMajor = false;
		if(activities.size() > 1 &&
				activities.get(0).getType().equalsIgnoreCase("major") &&
				activities.get(activities.size()-1).getType().equalsIgnoreCase("major")){
			startsAndEndsWithMajor =  true;
		}
		
		/* Check that the chain starts and ends with an activity, and is 
		 * alternating between activities and traces. */
		boolean alternatesActivitiesAndTraces = true;
		for(int i = 0; i < this.size(); i++){
			DigicoreChainElement dce = this.get(i);
			if((i % 2) == 0){ /* Even should be an activity. */ 
				if(!(dce instanceof DigicoreActivity)){
					alternatesActivitiesAndTraces = false;
				}
			} else{ /* Odd, should be a trace. */
				if(!(dce instanceof DigicoreTrace)){
					alternatesActivitiesAndTraces = false;
				}
			}
		}

		/* All conditions must be met for the chain to be considered 'complete' */
		if(startsAndEndsWithMajor && alternatesActivitiesAndTraces){
			isComplete = true;
		}
		return isComplete;
	}
	
	
	/**
	 * Checks for and returns the first {@link DigicoreActivity} in the chain.
	 * @return first {@link DigicoreActivity} in the chain, or null if there 
	 * are none.
	 */
	public DigicoreActivity getFirstMajorActivity(){
		List<DigicoreActivity> activities = getAllActivities();
		return activities.size() > 0 ? activities.get(0) : null;
	}

	
	/**
	 * Checks for and returns the last {@link DigicoreActivity} in the chain.
	 * @return last {@link DigicoreActivity} in the chain, or null if there 
	 * are none.
	 */
	public DigicoreActivity getLastMajorActivity(){
		List<DigicoreActivity> activities = getAllActivities();
		return activities.size() > 0 ? activities.get(this.size()-1) : null;
	}
	
	
	public List<DigicoreActivity> getAllActivities(){
		List<DigicoreActivity> activities = new ArrayList<>();
		for(DigicoreChainElement dce : this){
			if(dce instanceof DigicoreActivity){
				activities.add((DigicoreActivity)dce);
			}
		}
		return activities;
	}
	
	
	/**
	 * Get the day of the week that the chain starts. That is the day of the 
	 * week that the first major activity of the chain <i>ends</i>. Sunday is
	 * day 1, Monday is 2, Tuesday is 3, etc. 
	 * @return
	 */
	public int getChainStartDay(){
		return this.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.DAY_OF_WEEK);	
	}
	
	
	/**
	 * Get the day of the week that the chain starts. That is the day of the 
	 * week that the first major activity of the chain <i>ends</i>. Sunday is
	 * day 1, Monday is 2, Tuesday is 3, etc. If the day falls on a specific 
	 * <i>abnormal</i> day, the value 8 is returned.
	 * @param abnormalDays a list of integers, each representing a specific
	 * 		   {@link Calendar#DAY_OF_YEAR} that are considered <i>abnormal</i> 
	 * @return the day of the week (1 through 7), or 8 if it is one of the given
	 * 		   <i>abnormal</i> days.  
	 */
	public int getChainStartDay(List<Integer> abnormalDays){
		int dayOfYear = this.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.DAY_OF_YEAR);
		if(!abnormalDays.contains(dayOfYear)){
			return this.getFirstMajorActivity().getEndTimeGregorianCalendar().get(Calendar.DAY_OF_WEEK);
		} else{
			return 8;
		}
	}
	
	
	/**
	 * Checks if any of the chain's activities occur at a given facility.
	 * @param id the {@link Id} of the {@link DigicoreFacility} searched for.
	 * @return true if at least one of the activities in the chain occurs 
	 * 		at the given facility, false otherwise.
	 */
	public boolean containsFacility(Id<ActivityFacility> id){
		List<DigicoreActivity> activities = getAllActivities();
		boolean answer = false;
		int i = 0;
		while(!answer && i < activities.size()){
			DigicoreActivity thisActivity = activities.get(i);
			if(id == null && thisActivity.getFacilityId() == null){
				answer = true;
			} else if( id != null && thisActivity.getFacilityId() != null){
				answer = thisActivity.getFacilityId().compareTo(id) == 0 ? true : false;				
			}
			i++;
		}
		return answer;
	}
	
	
	/**
	 * This method takes an inter-provincial vehicle's chain and determines 
	 * whether the chain is an out-in chain's first part, i.e. the "OUT" 
	 * part before leaving the area. * NB: This check will only work if 
	 * chains are considered from XML3 vehicle files obtained from running 
	 * the {@link DigicoreChainCleaner2}.
	 * @return a boolean value
	 */
	
	public boolean isInterOutChain(){
		
		boolean isFirstMajor = false;
		boolean isLastExit = false;
		
		/* determine if first activity is major and last activity is an exit */			
		if(this.getFirstMajorActivity().getType().equalsIgnoreCase("major")){
			isFirstMajor = true;
		}
		
		if(this.getLastMajorActivity().getType().equalsIgnoreCase("exit")){
			isLastExit = true;
		}
	
		if(isFirstMajor && isLastExit){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * This method takes an inter-provincial vehicle's chain and determines 
	 * whether the chain is an out-in chain's last part, i.e. the "IN" part 
	 * after returning to the area. * NB: This check will only work if 
	 * chains are considered from XML3 vehicle files obtained from running 
	 * the {@link DigicoreChainCleaner2}.
	 * @return a boolean value
	 */
	
	public boolean isInterInChain(){
		
		boolean isFirstEntry = false;
		boolean isLastMajor = false;
		
		/* determine if first activity is an entry and last activity is a major */			
		if(this.getFirstMajorActivity().getType().equalsIgnoreCase("entry")){
			isFirstEntry = true;
		}
		
		if(this.getLastMajorActivity().getType().equalsIgnoreCase("major")){
			isLastMajor = true;
		}
	
		if(isFirstEntry && isLastMajor){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * This method takes an inter-provincial vehicle's chain and determines 
	 * whether the chain is an in-out chain, i.e. is the first activity an entry 
	 * and last activity an exit. * NB: This check will only work if chains are 
	 * considered from XML3 vehicle files obtained from running the 
	 * {@link DigicoreChainCleaner2}.
	 * @return a boolean value
	 */
	public boolean isInterInOutChain(){
		
		boolean isFirstEntry = false;
		boolean isLastExit = false;
		
		/* determine if first activity is an entry and last activity is an exit */			
		if(this.getFirstMajorActivity().getType().equalsIgnoreCase("entry")){
			isFirstEntry = true;
		}
		
		if(this.getLastMajorActivity().getType().equalsIgnoreCase("exit")){
			isLastExit = true;
		}
	
		if(isFirstEntry && isLastExit){
			return true;
		}else{
			return false;
		}
	}
	
	public String toString(){
		DigicoreActivity firstActivity = getFirstMajorActivity();
		GregorianCalendar startCalendar = firstActivity.getEndTimeGregorianCalendar();
		String startDateTime = String.format("%4d%02d%02d, Day %d, %02d:%02d", 
				startCalendar.get(Calendar.YEAR), 
				startCalendar.get(Calendar.MONTH)+1,
				startCalendar.get(Calendar.DAY_OF_MONTH),
				startCalendar.get(Calendar.DAY_OF_WEEK),
				startCalendar.get(Calendar.HOUR_OF_DAY),
				startCalendar.get(Calendar.MINUTE));
		return "Start time: " + startDateTime + "; Number of (minor) activities: " + this.getMinorActivities().size(); 
	}
}
