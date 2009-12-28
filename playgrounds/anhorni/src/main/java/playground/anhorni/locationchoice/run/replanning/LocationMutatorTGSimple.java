/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutatorTGSimple.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.run.replanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;
import org.matsim.locationchoice.utils.QuadTreeRing;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;

public class LocationMutatorTGSimple extends LocationMutator {
	
	protected int unsuccessfullLC = 0;
	private DefineFlexibleActivities defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges);
	private LeisureFacilityExtractor leisureFacilityExtractor;	
	private QuadTreeRing<ActivityFacilityImpl> leisureQuadtree;
	
	private final static Logger log = Logger.getLogger(LeisureFacilityExtractor.class);
	
	
	public LocationMutatorTGSimple(final NetworkLayer network, Controler controler,
			Knowledges knowledges,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type) {
		
		super(network, controler, knowledges, quad_trees, facilities_of_type);
		
		log.info("Using modified TGMutator");
		
		this.leisureQuadtree = new FacilityQuadTreeBuilder().buildFacilityQuadTree("leisure", 
				(ActivityFacilitiesImpl)controler.getFacilities());
		leisureFacilityExtractor = new LeisureFacilityExtractor(this.leisureQuadtree);	
	}
	
	public void handlePlan(final Plan plan){
		if (!this.handleShopActs((PlanImpl)plan) && !this.handleLeisureActs((PlanImpl)plan)){
			this.unsuccessfullLC++;
		}
		super.resetRoutes(plan);
	}
		
	private boolean handleShopActs(final PlanImpl plan) {
		List<ActivityImpl> flexibleActivities = this.getFlexibleActivities(plan);
		
		if (flexibleActivities.size() == 0) {
			return false;
		}	
		Collections.shuffle(flexibleActivities);
		ActivityImpl actToMove = flexibleActivities.get(0);
		List<?> actslegs = plan.getPlanElements();
		int indexOfActToMove = actslegs.indexOf(actToMove);
		
		// starting home and ending home are never flexible
		final Leg legPre = (Leg)actslegs.get(indexOfActToMove -1);
		final Leg legPost = (Leg)actslegs.get(indexOfActToMove + 1);
		final Activity actPre = (Activity)actslegs.get(indexOfActToMove - 2);
		final Activity actPost = (Activity)actslegs.get(indexOfActToMove + 2);
		
		double travelDistancePre = 0.0;
		double travelDistancePost = 0.0;
		
		if (legPre.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePre = legPre.getRoute().getDistance();
		}
		else {
			travelDistancePre = ((CoordImpl)actPre.getCoord()).calcDistance(actToMove.getCoord());
		}
		if (legPost.getMode().compareTo(TransportMode.car) == 0) {
			travelDistancePost = legPost.getRoute().getDistance();
		}
		else {
			travelDistancePost = ((CoordImpl)actToMove.getCoord()).calcDistance(actPost.getCoord());
		}
		double radius =  0.5 * (travelDistancePre + travelDistancePost);
				
		if (Double.isNaN(radius)) {
			return false;
		}
		
		if (!this.modifyLocationShop(actToMove, actPre.getCoord(), actPost.getCoord(), radius)) {
			return false;
		}
		return true;
	}
	
	private List<ActivityImpl> getFlexibleActivities(final PlanImpl plan) {
		List<ActivityImpl> flexibleActivities = new Vector<ActivityImpl>();
		if (!super.locationChoiceBasedOnKnowledge) {
			flexibleActivities = this.defineFlexibleActivities.getFlexibleActivities(plan);
		}
		else {
			flexibleActivities = defineMovablePrimaryActivities(plan);			
			List<?> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);		
				if (!this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).isPrimary(act.getType(), act.getFacilityId()) && 
						!(act.getType().startsWith("h") || act.getType().startsWith("tta"))) {
					flexibleActivities.add(act);
				}
			}
		}
		return flexibleActivities;
	}
			
	protected boolean modifyLocationShop(ActivityImpl act, Coord startCoord, Coord endCoord, double radius) {		
		double midPointX = (startCoord.getX() + endCoord.getX()) / 2.0;
		double midPointY = (startCoord.getY() + endCoord.getY()) / 2.0;	
		
		ArrayList<ActivityFacility> facilitySet = 
			(ArrayList<ActivityFacility>) this.quadTreesOfType.get(act.getType()).get(midPointX, midPointY, radius);
		
		ActivityFacilityImpl facility = null;
		if (facilitySet.size() > 1) {
			facility = (ActivityFacilityImpl)facilitySet.get(MatsimRandom.getRandom().nextInt(facilitySet.size()));
		}
		else {
			return false;
		}
		act.setFacility(facility);
   		act.setLink(((NetworkImpl) this.network).getNearestLink(facility.getCoord()));
   		act.setCoord(facility.getCoord());
   		
   		return true;
	}
	
	private boolean handleLeisureActs(final PlanImpl plan) {
		
		ActivityImpl actToMove = this.getRandomLeisureActivitiy(plan);
		
		if (actToMove == null) return false;
	
		LegImpl legPre = plan.getPreviousLeg(actToMove);
		ActivityImpl actPre = plan.getPreviousActivity(legPre);		
		return this.modifyLeisureLocation(actToMove, (CoordImpl)actPre.getCoord());
	}
	
	public ActivityImpl getRandomLeisureActivitiy(final Plan plan) {		
		List<ActivityImpl> flexibleActivities = new Vector<ActivityImpl>();
		
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				final ActivityImpl act = (ActivityImpl) pe;
				if (act.getType().startsWith("leisure")) {
					flexibleActivities.add(act);
				}
			}
		}
		if (flexibleActivities.size() > 0) {
			Collections.shuffle(flexibleActivities);
			return flexibleActivities.get(0);
		}
		else return null;
	}
	
	private boolean modifyLeisureLocation(ActivityImpl act, CoordImpl coordActPre) {
		
		String type = act.getType();		
		ActivityFacilityImpl facility = null;		
		if (type.equals("leisure*") || type.equals("leisure")) {	// FOR DEBUIGGING // mode ride			
			ArrayList<ActivityFacilityImpl> facilitySet = 
				(ArrayList<ActivityFacilityImpl>)this.leisureQuadtree.get(coordActPre.getX(), coordActPre.getY(), 
					MatsimRandom.getRandom().nextInt(5 * 1000));			
			if (facilitySet.size() > 1) {
				facility = facilitySet.get(MatsimRandom.getRandom().nextInt(facilitySet.size()));
			}
		}
		else {
			String[] entries = type.split("_", -1);							
			int dist = Integer.parseInt(entries[1].trim());
			facility = leisureFacilityExtractor.getFacility(coordActPre, dist);
		}
		if (facility != null) {
			act.setFacility(facility);
	   		act.setLink(((NetworkImpl) this.network).getNearestLink(facility.getCoord()));
	   		act.setCoord(facility.getCoord());	   		
	   		return true;
		}
		return false;
	}
				
	public int getNumberOfUnsuccessfull() {
		return this.unsuccessfullLC;		
	}
	
	public void resetUnsuccsessfull() {
		this.unsuccessfullLC = 0;
	}
}

