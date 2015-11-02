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

package org.matsim.contrib.locationchoice.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.population.LCActivity;
import org.matsim.contrib.locationchoice.population.LCLeg;
import org.matsim.contrib.locationchoice.population.LCPlan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.facilities.ActivityFacility;

public class PlanUtils {
	
	public static void copyPlanFieldsToFrom(Plan planTarget, Plan planTemplate) {
		if (planTarget instanceof PlanImpl) {
			if (planTemplate instanceof PlanImpl) {
				copyPlanFieldsToFrom((PlanImpl) planTarget, (PlanImpl) planTemplate);
				return;
			} else if (planTemplate instanceof LCPlan) {
				copyPlanFieldsToFrom((PlanImpl) planTarget, (LCPlan) planTemplate);
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
	
	private static void copyPlanFieldsToFrom(PlanImpl planTarget, PlanImpl planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl actTemplate = ((ActivityImpl) planTemplate.getPlanElements().get(actLegIndex));
				((ActivityImpl) pe).setEndTime(actTemplate.getEndTime());
				((ActivityImpl) pe).setCoord(actTemplate.getCoord());
				((ActivityImpl) pe).setFacilityId(actTemplate.getFacilityId());
				((ActivityImpl) pe).setLinkId(actTemplate.getLinkId());
				((ActivityImpl) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((ActivityImpl) pe).setStartTime(actTemplate.getStartTime());
				((ActivityImpl) pe).setType(actTemplate.getType());
			} else if (pe instanceof LegImpl) {
				LegImpl legTemplate = ((LegImpl)planTemplate.getPlanElements().get(actLegIndex));
				((LegImpl) pe).setArrivalTime(legTemplate.getArrivalTime());
				((LegImpl) pe).setDepartureTime(legTemplate.getDepartureTime());
				((LegImpl) pe).setMode(legTemplate.getMode());
				((LegImpl) pe).setRoute(legTemplate.getRoute());
				((LegImpl) pe).setTravelTime(legTemplate.getTravelTime());
			} else throw new RuntimeException("Unexpected PlanElement type was found: " + pe.getClass().toString() + ". Aborting!");
			actLegIndex++;
		}
	}

	private static void copyPlanFieldsToFrom(PlanImpl planTarget, LCPlan planTemplate) {
		planTarget.setScore(planTemplate.getScore());
		
		int actLegIndex = 0;
		for (PlanElement pe : planTarget.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				LCActivity actTemplate = ((LCActivity) planTemplate.getPlanElements().get(actLegIndex));
				((ActivityImpl) pe).setEndTime(actTemplate.getEndTime());
				((ActivityImpl) pe).setCoord(actTemplate.getCoord());
				((ActivityImpl) pe).setFacilityId(actTemplate.getFacilityId());
				((ActivityImpl) pe).setLinkId(actTemplate.getLinkId());
				((ActivityImpl) pe).setMaximumDuration(actTemplate.getMaximumDuration());
				((ActivityImpl) pe).setStartTime(actTemplate.getStartTime());
				((ActivityImpl) pe).setType(actTemplate.getType());
			} else if (pe instanceof LegImpl) {
				LCLeg legTemplate = ((LCLeg) planTemplate.getPlanElements().get(actLegIndex));
				((LegImpl) pe).setArrivalTime(legTemplate.getArrivalTime());
				((LegImpl) pe).setDepartureTime(legTemplate.getDepartureTime());
				((LegImpl) pe).setMode(legTemplate.getMode());
				((LegImpl) pe).setRoute(legTemplate.getRoute());
				((LegImpl) pe).setTravelTime(legTemplate.getTravelTime());
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
		
	public static void copyFrom(Plan srcPlan, Plan destPlan) {
		if (destPlan instanceof PlanImpl) {
			((PlanImpl) destPlan).copyFrom(srcPlan);
		} else if (destPlan instanceof LCPlan) {
			if (srcPlan instanceof LCPlan) {
				LCPlan.copyFrom((LCPlan) srcPlan, (LCPlan) destPlan);				
			} else LCPlan.copyFrom(srcPlan, (LCPlan) destPlan);
		} else throw new RuntimeException("Unexpected type of plan was found: " + destPlan.getClass().toString() + ". Aborting!");
	}
	
	public static Plan createCopy(Plan plan) {
		if (plan instanceof PlanImpl) {
			PlanImpl planTmp = new PlanImpl();
			planTmp.copyFrom(plan);
			return planTmp;			
		} else if (plan instanceof LCPlan) {
			LCPlan planTmp = new LCPlan((LCPlan) plan);
			return planTmp;
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Leg getPreviousLeg(final Plan plan, final Activity activity) {
		if (plan instanceof PlanImpl) {
			return ((PlanImpl) plan).getPreviousLeg(activity);
		} else if (plan instanceof LCPlan) {
			return LCPlan.getPreviousLeg((LCPlan) plan, (LCActivity) activity);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Activity getPreviousActivity(final Plan plan, final Leg leg) {
		if (plan instanceof PlanImpl) {
			return ((PlanImpl) plan).getPreviousActivity(leg);
		} else if (plan instanceof LCPlan) {
			return LCPlan.getPreviousActivity((LCPlan) plan,  (LCLeg) leg);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Leg getNextLeg(final Plan plan, final Activity activity) {
		if (plan instanceof PlanImpl) {
			return ((PlanImpl) plan).getNextLeg(activity);
		} else if (plan instanceof LCPlan) {
			return LCPlan.getNextLeg((LCPlan) plan, (LCActivity) activity);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static Activity getNextActivity(final Plan plan, final Leg leg) {
		if (plan instanceof PlanImpl) {
			return ((PlanImpl) plan).getNextActivity(leg);
		} else if (plan instanceof LCPlan) {
			return LCPlan.getNextActivity((LCPlan) plan,  (LCLeg) leg);
		} else throw new RuntimeException("Unexpected type of plan was found: " + plan.getClass().toString() + ". Aborting!");
	}
	
	public static void setArrivalTime(Leg leg, double arrivalTime) {
		if (leg instanceof LegImpl) {
			((LegImpl) leg).setArrivalTime(arrivalTime);
		} else if (leg instanceof LCLeg) {
			((LCLeg) leg).setArrivalTime(arrivalTime);
		} else throw new RuntimeException("Unexpected type of leg was found: " + leg.getClass().toString() + ". Aborting!");
	}
	
	public static void setFacilityId(Activity activity, Id<ActivityFacility> facilityId) {
		if (activity instanceof ActivityImpl) {
			((ActivityImpl) activity).setFacilityId(facilityId);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setFacilityId(facilityId);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}
	
	public static void setCoord(Activity activity, Coord coord) {
		if (activity instanceof ActivityImpl) {
			((ActivityImpl) activity).setCoord(coord);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setCoord(coord);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}
	
	public static void setLinkId(Activity activity, Id<Link> linkId) {
		if (activity instanceof ActivityImpl) {
			((ActivityImpl) activity).setLinkId(linkId);
		} else if (activity instanceof LCActivity) {
			((LCActivity) activity).setLinkId(linkId);
		} else throw new RuntimeException("Unexpected type of activity was found: " + activity.getClass().toString() + ". Aborting!");
	}
	
	public static void resetRoutes(final Plan plan) {
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				((Leg) pe).setRoute(null);
			}
		}
	}
}