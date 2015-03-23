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

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;

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
public class WithinDayAgentUtils {
//	private WithinDayAgentUtils(){} // do not instantiate: static methods only

	public static final Integer getCurrentPlanElementIndex(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			return ((PersonDriverAgentImpl) agent).getCurrentPlanElementIndex() ;			
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getCurrentPlanElementIndex(...). Aborting!");
		}
	}

	public static final Integer getCurrentRouteLinkIdIndex(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			return ((PersonDriverAgentImpl) agent).getCurrentLinkIndex();			
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

	public static final void resetCaches(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			((PersonDriverAgentImpl) agent).resetCaches();			
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support resetCaches(...). Aborting!");
		}
	}
	
	public static final Leg getModifiableCurrentLeg(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			PlanElement currentPlanElement = getCurrentPlanElement(agent);
			if (!(currentPlanElement instanceof Leg)) {
				return null;
			}
			return (Leg) currentPlanElement;
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getCurrentLeg(...). Aborting!");
		}
	}
	
	public static final Plan getModifiablePlan(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			return ((PersonDriverAgentImpl) agent).getModifiablePlan();
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getModifiablePlan(...). Aborting!");
		}
	}
	
	public static final PlanElement getCurrentPlanElement(MobsimAgent agent) {
		if (agent instanceof PersonDriverAgentImpl) {
			return getModifiablePlan(agent).getPlanElements().get(getCurrentPlanElementIndex(agent));
		} else {
			throw new RuntimeException("Sorry, agent is from type " + agent.getClass().toString() + 
					" which does not support getCurrentPlanElement(...). Aborting!");
		}
	}
}