/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalBasicWithindayAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.population.PopulationUtils;

/**
 * <p>
 * This class is an attempt to provide access to the internals of PersonDriverAgentImpl
 * in a way that it can be used for within-day replanning.
 * </p>
 * <p>
 * Moreover, it is an attempt to replace ExperimentalBasicWithindayAgent which extends
 * PersonDriverAgentImpl. This has become a problem since other MATSim modules (such as
 * PT which uses TransitAgents) also extend that class. Instead, this class re-implements
 * the functionality of the PlanBasedWithinDayAgent Interface by accessing the package
 * protected methods from PlanBasedWithinDayAgent (this is possible since it is located
 * in the same package).
 * </p>
 * <p>
 * Up to now, all the methods get a MobsimAgent. Depending on which class implements
 * the interface, the methods will perform their task or throw an exception if the
 * given object does not support that operation. At the moment, only PersonDriverAgentImpl
 * are supported but UmlaufDrivers should also be supported in the future (since UmlaufDriver
 * does not extend PersonDriverAgentImpl the method signatures have been changed).
 * </p>
 * <i>The class is experimental. Use at your own risk, and expect even 
 * less support than with other pieces of matsim.</i>
 * 
 * @author cdobler
 */
public final class WithinDayAgentUtils {

		private WithinDayAgentUtils(){
			// do not instantiate: static methods only
		}

	private static final Logger log = LogManager.getLogger( WithinDayAgentUtils.class );

	public static Integer getCurrentPlanElementIndex(MobsimAgent agent) {
		//		if (agent instanceof PersonDriverAgentImpl) {
		//			return ((PersonDriverAgentImpl) agent).getCurrentPlanElementIndex() ;
		//		} else

		// commenting out the above so the new code runs through the existing tests.  kai, nov'17

		if ( agent instanceof PlanAgent ) {
			return ((PlanAgent)agent).getCurrentPlan().getPlanElements().indexOf( ((PlanAgent)agent).getCurrentPlanElement() ) ;
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getCurrentPlanElementIndex(...). Aborting!");
		}
	}
	
	/** NOTES:
	 * () The current link index does not point to where the agent is, but one ahead.
	 * () It does that even if there is nothing there in the underlying list.  I keep forgetting the convention, but I think that the
	 *     arrival link is not in the list, and so for the arrival link special treatment is necessary.
	 * () Routes may have loops, in which case the "indexOf" approach does not work.
	 */
	public static Integer getCurrentRouteLinkIdIndex(MobsimAgent agent) {

		if (agent instanceof HasModifiablePlan) {

			return ((HasModifiablePlan) agent).getCurrentLinkIndex();

//		} else if ( agent instanceof PlanAgent ) {
//			
//			// the following does not work because of loop routes, see above
//				Leg currentLeg = (Leg) ((PlanAgent)agent).getCurrentPlanElement() ;
//				if ( ! (currentLeg.getRoute() instanceof NetworkRoute ) ) {
//					throw new RuntimeException("agent currently not on network route; asking for link id index does not make sense") ;
//				}
//				NetworkRoute route = (NetworkRoute) currentLeg.getRoute();
//				int index = route.getLinkIds().indexOf( agent.getCurrentLinkId() );
//
//				// if agent is on arrival link, we need special treatment:
//				if ( index==-1 && agent.getCurrentLinkId().equals( route.getEndLinkId() ) ) {
//					index = route.getLinkIds().size() ;
//				}
//
//				// and in the end it points even one further (always points to the _next_ entry, if there is any)
//				index++ ;
//
//				return index ;

			} else {
				throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
						" which does not support getCurrentRouteLinkIdIndex(...). Aborting!");
			}
	}

	//	public static final void calculateAndSetDepartureTime(MobsimAgent agent, Activity act) {
	//		if (agent instanceof PersonDriverAgentImpl) {
	//			((PersonDriverAgentImpl) agent).calculateAndSetDepartureTime(act);			
	//		} else {
	//			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
	//					" which does not support calculateAndSetDepartureTime(...). Aborting!");
	//		}
	//	}

	public static void resetCaches(MobsimAgent agent) {
		if (agent instanceof HasModifiablePlan) {
			((HasModifiablePlan) agent).resetCaches();			
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support resetCaches(...). Aborting!");
		}
	}
	public static void rescheduleActivityEnd(MobsimAgent agent, Mobsim mobsim ) {
		if ( mobsim instanceof ActivityEndRescheduler ) {
			((ActivityEndRescheduler) mobsim).rescheduleActivityEnd(agent);
		} else {
			throw new RuntimeException("mobsim does not support activity end rescheduling; aborting ...") ;
		}
	}

	public static Leg getModifiableCurrentLeg(MobsimAgent agent) {
		PlanElement currentPlanElement = getCurrentPlanElement(agent);
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return (Leg) currentPlanElement;
	}

	public static Plan getModifiablePlan(MobsimAgent agent) {
		if (agent instanceof HasModifiablePlan) {
			return ((HasModifiablePlan) agent).getModifiablePlan();
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getModifiablePlan(...). Aborting!");
		}
	}

	public static PlanElement getCurrentPlanElement(MobsimAgent agent) {
		return getModifiablePlan(agent).getPlanElements().get(getCurrentPlanElementIndex(agent));
	}

	public static boolean isOnReplannableCarLeg(MobsimAgent agent) {
		//		if (plan == null) {
		//			log.info( " we don't have a modifiable plan; returning ... ") ;
		//			return false;
		//		}
		if ( !(getCurrentPlanElement(agent) instanceof Leg) ) {
			log.info( "agent not on leg; returning ... ") ;
			return false ;
		}
		if (!((Leg) getCurrentPlanElement(agent)).getMode().equals(TransportMode.car)) {
			log.info( "not a car leg; can only replan car legs; returning ... ") ;
			return false;
		}
		return true ;
	}
	
	public static Plan printPlan(MobsimAgent agent1) {
		final Plan plan = getModifiablePlan(agent1);
		return printPlan(plan) ;
	}
	
	public static Plan printPlan(Plan plan) {
		System.err.println( "plan=" + plan );
		for ( int ii=0 ; ii<plan.getPlanElements().size() ; ii++ ) {
			System.err.println( "\t" + ii + ":\t" + plan.getPlanElements().get(ii) );
		}
		return plan;
	}
	
	/**
	 * Only the PlanElements are changed - further Steps
	 * like updating the Routes of the previous and next Leg
	 * have to be done elsewhere.
	 *
	 * @author cdobler
	 */
	public static boolean replaceLegBlindly(Plan plan, Leg oldLeg, Leg newLeg) {

		if (plan == null) return false;
		if (oldLeg == null) return false;
		if (newLeg == null) return false;

		int index = plan.getPlanElements().indexOf(oldLeg);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two legs are equal if
		// certain (or all) elements are equal.  kai, oct'10

		if (index == -1) return false;

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index,newLeg);

		return true;
	}
	
	/**
	 * Only the PlanElements are changed - further Steps
	 * like updating the Routes of the previous and next Leg
	 * have to be done elsewhere.
	 *
	 * @author cdobler
	 */
	public static boolean replaceActivityBlindly( Plan plan, Activity oldActivity, Activity newActivity) {

		if (plan == null) return false;
		if (oldActivity == null) return false;
		if (newActivity == null) return false;

		int index = plan.getPlanElements().indexOf(oldActivity);
		// yyyy I can't say how safe this is.  There is no guarantee that the same entry is not used twice in the plan.  This will in
		// particular be a problem if we override the "equals" contract, in the sense that two activities are equal if
		// certain (or all) elements are equal.  kai, oct'10

		if (index == -1) return false;

		//		/*
		//		 *  If the new Activity takes place on a different Link
		//		 *  we have to replan the Routes from an to that Activity.
		//		 */
		//		if (oldActivity.getLinkId() != newActivity.getLinkId())
		//		{
		//
		//		}

		plan.getPlanElements().remove(index);
		plan.getPlanElements().add(index, newActivity);

		return true;
	}
	
	// === search methods: ===
	public static int indexOfPlanElement(MobsimAgent agent, PlanElement pe) {
		Plan plan = getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		return planElements.indexOf(pe) ;
	}
	
	public static int indexOfNextActivityWithType( MobsimAgent agent, String type ) {
		Plan plan = getModifiablePlan(agent) ;
		List<PlanElement> planElements = plan.getPlanElements() ;

		for ( int index = getCurrentPlanElementIndex(agent) ; index < planElements.size() ; index++ ) {
			PlanElement pe = planElements.get(index) ;
			if ( pe instanceof Activity ) {
				if ( ((Activity)pe).getType().equals(type) ) {
					return index ;
				}
			}
		}
		return -1 ;
	}
	
	public static Activity findNextActivityWithType( MobsimAgent agent, String type ) {
		int index = indexOfNextActivityWithType( agent, type ) ;
		return (Activity) getModifiablePlan(agent).getPlanElements().get(index) ;
	}
	
	public static List<PlanElement> subList( MobsimAgent agent, int fromIndex, int toIndex) {
		return getModifiablePlan(agent).getPlanElements().subList( fromIndex, toIndex ) ;
	}
	
	public static List<PlanElement> convertInteractionActivities(List<? extends PlanElement> elements) {
		List<PlanElement> updatedElements = new ArrayList<>(elements.size());
		
		for (int i = 0; i < elements.size(); i++) {
			PlanElement element = elements.get(i);
			
			if (element instanceof Activity) {
				element = PopulationUtils.convertInteractionToStandardActivity((Activity) element);
			}
			
			updatedElements.add(element);
		}
		
		return updatedElements;
	}
}
