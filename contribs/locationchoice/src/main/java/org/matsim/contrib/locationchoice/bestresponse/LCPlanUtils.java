/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.LCActivity;
import org.matsim.contrib.locationchoice.bestresponse.LCLeg;
import org.matsim.contrib.locationchoice.bestresponse.LCPlan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacility;

 class LCPlanUtils{
 	// yy It looks like PopulationUtils now does similar things or even the same--???
	
	public static void copyPlanFieldsFromTo( Plan planTemplate, Plan planTarget ) {
		if (planTarget instanceof Plan) {
			if (planTemplate instanceof Plan) {
				copyPlanFieldsToFrom1((Plan) planTarget, (Plan) planTemplate);
				return;
			} else if (planTemplate instanceof LCPlan) {
				copyPlanFieldsToFrom((Plan) planTarget, (LCPlan) planTemplate);
				return;
			}
		} else if (planTarget instanceof LCPlan) {
			if (planTemplate instanceof LCPlan) {
				copyPlanFieldsToFrom((LCPlan) planTarget, (LCPlan) planTemplate);
				return;
			}
		}
		
		throw new RuntimeException("Unexpected combination of plan types was found: " + planTarget.getClass().toString() +
				" and " + planTemplate.getClass().toString() + ". Aborting!");
	}
	
	private static void copyPlanFieldsToFrom1(Plan planTarget, Plan planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof Activity) {
				Activity actTemplate = ((Activity) planTemplate.getPlanElements().get(actLegIndex));
				((Activity) pe).setEndTime(actTemplate.getEndTime());
				((Activity) pe).setCoord(actTemplate.getCoord());
				((Activity) pe).setFacilityId(actTemplate.getFacilityId());
				((Activity) pe).setLinkId(actTemplate.getLinkId());
				((Activity) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((Activity) pe).setStartTime(actTemplate.getStartTime());
				((Activity) pe).setType(actTemplate.getType());
			} else if (pe instanceof Leg) {
				Leg legTemplate = ((Leg)planTemplate.getPlanElements().get(actLegIndex));
				Leg r = ((Leg) pe);
				r.setTravelTime( legTemplate.getDepartureTime() + legTemplate.getTravelTime() - r.getDepartureTime() );
				((Leg) pe).setDepartureTime(legTemplate.getDepartureTime());
				((Leg) pe).setMode(legTemplate.getMode());
				((Leg) pe).setRoute(legTemplate.getRoute());
				((Leg) pe).setTravelTime(legTemplate.getTravelTime());
			} else throw new RuntimeException("Unexpected PlanElement type was found: " + pe.getClass().toString() + ". Aborting!");
			actLegIndex++;
		}
	}

	private static void copyPlanFieldsToFrom(Plan planTarget, LCPlan planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof Activity) {
				LCActivity actTemplate = ((LCActivity) planTemplate.getPlanElements().get(actLegIndex));
				((Activity) pe).setEndTime(actTemplate.getEndTime());
				((Activity) pe).setCoord(actTemplate.getCoord());
				((Activity) pe).setFacilityId(actTemplate.getFacilityId());
				((Activity) pe).setLinkId(actTemplate.getLinkId());
				((Activity) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((Activity) pe).setStartTime(actTemplate.getStartTime());
				((Activity) pe).setType(actTemplate.getType());
			} else if (pe instanceof Leg) {
				LCLeg legTemplate = ((LCLeg) planTemplate.getPlanElements().get(actLegIndex));
				Leg r = ((Leg) pe);
				r.setTravelTime( legTemplate.getArrivalTime() - r.getDepartureTime() );
				((Leg) pe).setDepartureTime(legTemplate.getDepartureTime());
				((Leg) pe).setMode(legTemplate.getMode());
				((Leg) pe).setRoute(legTemplate.getRoute());
				((Leg) pe).setTravelTime(legTemplate.getTravelTime());
			} else throw new RuntimeException("Unexpected PlanElement type was found: " + pe.getClass().toString() + ". Aborting!");
			actLegIndex++;
		}
	}
	
	private static void copyPlanFieldsToFrom(LCPlan planTarget, LCPlan planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof LCActivity) {
				LCActivity actTemplate = ((LCActivity) planTemplate.getPlanElements().get(actLegIndex));
				((LCActivity) pe).setEndTime(actTemplate.getEndTime());
				((LCActivity) pe).setCoord(actTemplate.getCoord());
				((LCActivity) pe).setFacilityId(actTemplate.getFacilityId());
				((LCActivity) pe).setLinkId(actTemplate.getLinkId());
				((LCActivity) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((LCActivity) pe).setStartTime(actTemplate.getStartTime());
				((LCActivity) pe).setType(actTemplate.getType());
			} else if (pe instanceof LCLeg) {
				LCLeg legTemplate = ((LCLeg) planTemplate.getPlanElements().get(actLegIndex));
				((LCLeg) pe).setArrivalTime(legTemplate.getArrivalTime());
				((LCLeg) pe).setDepartureTime(legTemplate.getDepartureTime());
				((LCLeg) pe).setMode(legTemplate.getMode());
				((LCLeg) pe).setRoute(legTemplate.getRoute());
				((LCLeg) pe).setTravelTime(legTemplate.getTravelTime());
			} else throw new RuntimeException("Unexpected PlanElement type was found: " + pe.getClass().toString() + ". Aborting!");
			actLegIndex++;
		}
	}
		
	public static void copyFromTo( Plan srcPlan, Plan destPlan ) {
		if (destPlan instanceof Plan) {
			PopulationUtils.copyFromTo(srcPlan, destPlan);
		} else if (destPlan instanceof LCPlan) {
			if (srcPlan instanceof LCPlan) {
				LCPlan.copyFrom((LCPlan) srcPlan, (LCPlan) destPlan);				
			} else LCPlan.copyFrom(srcPlan, (LCPlan) destPlan);
		} else throw new RuntimeException("Unexpected type of plan was found: " + destPlan.getClass().toString() + ". Aborting!");
	}
	
	public static Plan createCopy(Plan plan) {
		if (plan instanceof Plan) {
			Plan planTmp = PopulationUtils.createPlan();
			PopulationUtils.copyFromTo(plan, planTmp);
			return planTmp;			
		} else if (plan instanceof LCPlan) {
			LCPlan planTmp = new LCPlan((LCPlan) plan);
			return planTmp;
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Leg getPreviousLeg(final Plan plan, final Activity activity) {
		if (plan instanceof Plan) {
			return PopulationUtils.getPreviousLeg(((Plan) plan), activity);
		} else if (plan instanceof LCPlan) {
			return LCPlan.getPreviousLeg((LCPlan) plan, (LCActivity) activity);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Activity getPreviousActivity(final Plan plan, final Leg leg) {
		if (plan instanceof Plan) {
			return PopulationUtils.getPreviousActivity(((Plan) plan), leg) ;
		} else if (plan instanceof LCPlan) {
			return LCPlan.getPreviousActivity((LCPlan) plan,  (LCLeg) leg);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Leg getNextLeg(final Plan plan, final Activity activity) {
		if (plan instanceof Plan) {
			return PopulationUtils.getNextLeg(((Plan) plan), activity) ;
		} else if (plan instanceof LCPlan) {
			return LCPlan.getNextLeg((LCPlan) plan, (LCActivity) activity);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Activity getNextActivity(final Plan plan, final Leg leg) {
		if (plan instanceof Plan) {
			return PopulationUtils.getNextActivity(((Plan) plan), leg) ;
		} else if (plan instanceof LCPlan) {
			return LCPlan.getNextActivity((LCPlan) plan,  (LCLeg) leg);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static void setArrivalTime(Leg leg, double arrivalTime) {
		if (leg instanceof Leg) {
			final double arrTime = arrivalTime;
			Leg r = ((Leg) leg);
			r.setTravelTime( arrTime - r.getDepartureTime() );
		} else if (leg instanceof LCLeg) {
			((LCLeg) leg).setArrivalTime(arrivalTime);
		} else throw new RuntimeException("Unexpected type of leg was found: " + leg.getClass().toString() + ". Aborting!");
	}
	
	public static void setFacilityId(Activity activity, Id<ActivityFacility> facilityId) {
		if (activity instanceof Activity) {
			((Activity) activity).setFacilityId(facilityId);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setFacilityId(facilityId);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}
	
	public static void setCoord(Activity activity, Coord coord) {
		if (activity instanceof Activity) {
			((Activity) activity).setCoord(coord);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setCoord(coord);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}
	
	public static void setLinkId(Activity activity, Id<Link> linkId) {
		if (activity instanceof Activity) {
			((Activity) activity).setLinkId(linkId);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setLinkId(linkId);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}

}
