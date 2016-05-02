/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityRelocator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight.moveFacilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;


/**
 * Class to check if an activity chain (i.e. {@link Plan}) has an affected
 * facility that must be relocated. If so, the facility(ies) are taken out
 * of the plan, and the relocations are added to the plan using a best
 * insertion heuristic.
 * 
 * @author jwjoubert
 */
public class FacilityRelocator {
	private final LeastCostPathCalculator router;
	private final Network network;
	Map<Id<Node>, Map<Id<Node>, Double>> routeMap;
	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:3857", "WGS84_SA_Albers");
	private final CONTAINER_RELOCATIONS relocation;

	public FacilityRelocator(Network network, String relocation) {
		this.network = network;
		this.routeMap = new HashMap<>();
		this.relocation = CONTAINER_RELOCATIONS.valueOf(relocation);
		
		/* Set up the network router. */
		TravelDisutility travelCosts = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return getLinkTravelDisutility(link, Time.UNDEFINED_TIME, null, null);
			}
		};
		TravelTime travelTimes = new FreeSpeedTravelTime();

		AStarLandmarksFactory asl = new AStarLandmarksFactory(this.network, travelCosts);
		this.router = asl.createPathCalculator(network, travelCosts, travelTimes );
	}
	
	/**
	 * For now I assume that the legs remain the same mode, all are 
	 * 'commercial'. All the affected facilities are replaced with a 
	 * <i><b>single</b></i> relocated facility.
	 * 
	 * @param plan
	 * @return
	 */
	public Plan processPlan(Plan plan){
		Plan newPlan = new PlanImpl();
		List<String> affectedFacilities = CTUtilities.getAffectedFacilities();
		List<Activity> partialPlan = new ArrayList<>();
		List<Activity> relocatedActivities = new ArrayList<>();
		
		boolean allowAsMajor = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Activity){
				Activity act = (Activity)pe;
				Id<ActivityFacility> fId = act.getFacilityId();
				if(fId != null){
					if(!affectedFacilities.contains(fId.toString())){
						partialPlan.add(act);
					} else{
						relocatedActivities.add(act);
						if(act.getType().equalsIgnoreCase("major")){
							allowAsMajor = true;
						}
					}
				} else{
					partialPlan.add(act);
				}
			}
		}
		
		List<Activity> bestInsertionList = findBestInsertion(partialPlan, allowAsMajor);
		/* Clean up the plan. That is, ensure only the first activity has an
		 * end time set, all others except the last only has max duration. The
		 * last activity has no times set. */
		bestInsertionList.get(0).setMaximumDuration(Time.UNDEFINED_TIME);
		double currentEnd = bestInsertionList.get(0).getEndTime();
		if(currentEnd == Time.UNDEFINED_TIME){
			bestInsertionList.get(0).setEndTime(Time.parseTime("00:06:00"));
		}
		
		bestInsertionList.get(bestInsertionList.size()-1).setEndTime(Time.UNDEFINED_TIME);
		bestInsertionList.get(bestInsertionList.size()-1).setMaximumDuration(Time.UNDEFINED_TIME);
		for(int i = 1; i < bestInsertionList.size()-1; i++){
			bestInsertionList.get(i).setEndTime(Time.UNDEFINED_TIME);
			double d = bestInsertionList.get(i).getMaximumDuration();
			if(d == Time.UNDEFINED_TIME){
				bestInsertionList.get(i).setMaximumDuration(Time.parseTime("00:20:00"));
			}
		}
		
		/* Build a complete plan from the list of activities. */
		Leg commercialLeg = new LegImpl("commercial");
		for(int i = 0; i < bestInsertionList.size()-1; i++){
			newPlan.addActivity(bestInsertionList.get(i));
			newPlan.addLeg(commercialLeg);
		}
		newPlan.addActivity(bestInsertionList.get(bestInsertionList.size()-1));
		
		return newPlan;
	}
	
	
	/**
	 * Find the best insertion position for a relocated activity in a given list 
	 * of current activities.
	 * 
	 * @param partialRoute
	 * @param allowAsMajor a flag indicating whether the relocated facility is
	 * 	      allowed to be inserted at the start or end of the activity chain.
	 * 		  This is only allowed if one (or more) of the affected facilities
	 * 		  occurred at the start or end of the list of activities.
	 * @return
	 */
	private List<Activity> findBestInsertion(List<Activity> partialRoute, boolean allowAsMajor){
		int startIndex = 1;
		int endIndex = partialRoute.size()-1;
		if(allowAsMajor){
			startIndex = 0;
			endIndex = partialRoute.size();
		}
		List<Activity> newList = partialRoute;
		double best = Double.POSITIVE_INFINITY;
		List<Activity> bestList = null;
		for(int i = startIndex; i <= endIndex; i++){
			List<Activity> tmpList = new ArrayList<>(newList);
			Activity tmpActivity = getRelocationActivity();
			tmpList.add(i, tmpActivity);
			
			double tmp = evaluateList(tmpList);
			if(tmp < best){
				if(i == 0){
					tmpActivity.setType("major");
					tmpActivity.setEndTime(Time.parseTime("08:00:00"));
				} else if(i == partialRoute.size()){
					tmpActivity.setType("major");
				} else{
					tmpActivity.setType("minor");
					tmpActivity.setMaximumDuration(Time.parseTime("00:20:00"));
				}
				best = tmp;
				bestList = tmpList;
			}
		}
		
		return bestList;
	}
	

	/**
	 * Calculate the total route distance. The inter-node distances are cached 
	 * locally to try and save computational time. If a node-pair does not have
	 * an existing route, it is calculated on the actual network using the A*
	 * land mark router, and the newly calculated route is then added to the
	 * cache.
	 * 
	 * @param list
	 * @return
	 */
	private double evaluateList(List<Activity> list){
		double value = 0.0;
		
		for(int i = 0; i < list.size()-1; i++){
			for(int j = i+1; j < list.size(); j++){
				Node ni = NetworkUtils.getNearestLink(network, list.get(i).getCoord()).getToNode();
				Node nj = NetworkUtils.getNearestLink(network, list.get(j).getCoord()).getFromNode();
				
				/* Check if the from-node already exists in the route map. */
				if(!routeMap.containsKey(ni.getId())){
					routeMap.put(ni.getId(), new HashMap<Id<Node>, Double>());
				}
				Map<Id<Node>, Double> pairMap = routeMap.get(ni.getId());
				if(pairMap.containsKey(nj.getId())){
					value += pairMap.get(nj.getId());
				} else{
					/* Find the route. */
					Path path = router.calcLeastCostPath(ni, nj, Time.UNDEFINED_TIME, null, null);
					double pathDistance = path.travelCost;
					value += pathDistance;
					
					/* Update the route map. */
					pairMap.put(nj.getId(), pathDistance);
				}
			}
		}
		
		return value;
	}
	
	/**
	 * Returns the relocation activity. The coordinate is transformed into the
	 * <code>WGS_SA_Albers</code> coordinate reference system, which is 
	 * typically what we use in MATSim models in South Africa.
	 * 
	 * @return
	 */
	private Activity getRelocationActivity(){
		Activity act = new ActivityImpl("minor", ct.transform(this.relocation.getCoord()));
		act.setFacilityId(Id.create(this.relocation.toString().toLowerCase(), ActivityFacility.class));
		return act;
	}
	
	public enum CONTAINER_RELOCATIONS{
		BELCON(),
		KRAAICON();
		
		private CONTAINER_RELOCATIONS() {
		}
		
		
		public Coord getCoord(){
			switch (this) {
			case BELCON:
				return CoordUtils.createCoord(2073560.0, -4016772.0);
			case KRAAICON:
				return CoordUtils.createCoord(2085660.0, -4007210.0);
			default:
				throw new RuntimeException("Dont know what to do with " + this.toString());
			}
		}
	}
}

