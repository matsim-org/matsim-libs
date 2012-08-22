/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingHandler.java
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
package playground.thibautd.hitchiking.qsim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent.State;

/**
 * Interface for a class meant at generating fake plan elements
 * on the fly during the simulation of a hitch hiking trip
 * @author thibautd
 */
public interface HitchHikingHandler {
	/**
	 * The stage of the trip
	 */
	public enum Stage {
			EGRESS( null ),
			DROP_OFF( EGRESS ),
			CAR_POOL( DROP_OFF ),
			PICK_UP( CAR_POOL ),
			ACCESS( PICK_UP );

			private final Stage next;
			private Stage(final Stage next) {
				this.next = next;
			}

			/**
			 * @return  The stage following the current one,  or null if it is the last one
			 */
			public Stage next() {
				return next;
			}
	};

	public Id getCurrentLinkId();

	public Id chooseNextLinkId();

	public Id getDestinationLinkId();

	public State getState();

	public double getActivityEndTime();

	/**
	 * @param now
	 * @return true if there is a next hitch hiking state, false
	 * if the control be be passed again to the default agent
	 */
	public boolean endActivityAndComputeNextState(double now);

	/**
	 * @param now
	 * @return true if there is a next hitch hiking state, false
	 * if the control be be passed again to the default agent
	 * */
	public boolean endLegAndComputeNextState(double now);

	public String getMode();

	public void notifyMoveOverNode(Id newLinkId);
}

