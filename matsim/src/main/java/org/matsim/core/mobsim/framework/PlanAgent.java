/* *********************************************************************** *
 * project: matsim
 * PlanAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

/**Design decisions:<ul>
 * <li>The concept that I found was that, at the end of a PlanElement, the agent got control from the relevant engine 
 * (activity, net, teleportation, ...).
 * After advancing the Plan, the agent would not return its control to the calling method, but insert itself directly into the 
 * Mobsim.
 * <li>When trying around with object composition, we found that this does not work, since the PlanAgent delegate would only
 * schedule the delegate back into the Mobsim.  Discussing a bit, we found that this is a problem in other places as well
 * (e.g. context switches in window-driven systems, where the calling method needs to know about the context switch).  The decision
 * was thus to modify the design such that control about the agent is always returned to the calling method.  This is, however,
 * not yet implemented (nov'10).
 * </ul>
 * 
 * Towards a concept for status pointers:<ul>
 * <li> Let us start with the PlanElements Iterator.
 * </li><li> Re-using standard iterators does not make sense since those are always <i>between</i> elements,
 * but we need a "current" here.
 * </li><li> So we could say getPrev and getNext.
 * </li><li> How do we insert and remove?  In ArrayList, the Iterator fails if there is insert/remove
 * outside of the iterator.  But here, we cannot move the iterator away from its current position.
 * </li>
 * </ul>
 * In terms of design, the assumption is that the plan remains unchanged in the mobsim, at least from the
 * perspective of the iterations: plan = genotype, execution = phenotype.  Therefore, <i>already Christoph's implementation
 * violates the specification</i>.
 * <br/>
 * That is, we need to copy the plan, or at least copy it before modification ("getModifiablePlan").  This, however,
 * would immensely simplify the design, since we could, at this point, also shorten the plan to where the agent currently is,
 * meaning that all replanning algos should also work much better.
 * <br/>
 * Memory considerations could be addressed by lazily delaying this plans copying to the point where the modifiable
 * plan is needed.
 * <p/>
 *
 * @author dgrether
 * @author nagel
 *
 */
public interface PlanAgent extends MobsimAgent {
	public Id getDestinationLinkId();

	/**
	 * The time the agent wants to depart from an Activity. If the agent is currently driving,
	 * the return value cannot be interpreted (e.g. it is not defined if it is the departure time
	 * from the previous activity, or from the next one).
	 *
	 * @return the time when the agent wants to depart from an activity.
	 */
	public double getActivityEndTime();
	/* there is no corresponding setter, as the implementation should set the the corresponding time
	 * internally, e.g. in legEnds().
	 */
	// yyyy getDepartureTimeFromActivity()  [[since there is also a linkDepartureTime of the
	// queue sim, and possibly a departure time of a leg]].  kai, jan'10
	// But the transit driver does not have an activity (?!). kai, apr'10
	// Re-named this into this weird method name since I got confused once more.  Would have been a lot easier if
	// bus drivers either would have activities, or would not be PersonAgents.  kai, oct'10

	/**
	 * Informs the agent that the activity has ended.  The agent is responsible for what comes next.
	 *
	 * @param now
	 */
	@Deprecated
	public void endActivityAndAssumeControl(final double now);

	/**
	 * Informs the agent that the leg has ended.  The agent is responsible for what comes next.
	 *
	 * @param now the current time in the simulation
	 */
	@Deprecated
	public void endLegAndAssumeControl(final double now);

	/**
	 * Design decisions:<ul>
	 * <li> Since there is getCurrentPlanElement(), returning that plan element is not necessary.  Thus, I decided to use void
	 * return for the time being.
	 * <li> Turns out that some plans end here, and making sure that the next call to getCurrentPlanElement seems to be to heavy
	 * of a requirement.  Thus, this is returning a Boolean, so it can return null if the advancePlan failed.
	 * <li> Might just have advancePlan as a method.  For the time being, they include the type of the previous PlanElement,
	 * since that info may be needed (although it should, in theory, be in the type of getCurrentPlanElement).
	 * </ul>
	 */
	public Boolean endLegAndAdvancePlan() ;
	public Boolean endActivityAndAdvancePlan() ;
	
	public PlanElement getCurrentPlanElement() ;
	// if this does not make sense for a class, then the class is maybe not a "Plan"Agent.  kai, may'10

	/**
	 * @return "(Leg) getCurrentPlanElement()" if the current plan element is a leg, otherwise null.
	 */
	public Leg getCurrentLeg();

	/**
	 * @return "(Activity) getCurrentPlanElement()" if the current plan element is an activity, otherwise null.
	 */
	public Activity getCurrentActivity();

	// yyyy "Teleportation" certainly does NOT belong into a vehicle.  Also not into the driver.
	// Might go directly into the person, as some kind of minimal mobsim convenience method
	// (although I am not convinced).  kai, jan/apr'10
	// zzzz Teleportation should from my point of view not be included in a data class like Person dg apr'10
	// This is here since a in a normal leg, the driver moves from node to node and eventually is at the destination.
	// With teleportation, this does not work, and so another setting method needs to be found.
	// Can't say how this is done with transit.  kai, aug'10
	@Deprecated // unclear, see somments above. kai, aug'10
	public void teleportToLink(final Id linkId);

	/**
	 * Design thoughts:<ul>
	 * <li> yyyy I don't like this "initialize" method that one can easily forget to call.
	 * And I am confident that one can do without it.  kai, may'10
	 * <li> The "checkIfAlive" is there since an agent can have no log (staying at first activity all day), in which case
	 * the agent is essentially ignored by the mobsim.  (This is how I found it; not so great since it causes problems
	 * with the scoring.)
	 * </ul>
	 */
	public boolean initializeAndCheckIfAlive();


}
