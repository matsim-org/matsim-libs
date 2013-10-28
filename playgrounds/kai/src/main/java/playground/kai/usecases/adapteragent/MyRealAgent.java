/* *********************************************************************** *
 * project: kai
 * MyRealAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.kai.usecases.adapteragent;

import org.matsim.api.core.v01.Id;

import playground.mzilske.withinday.ActivityBehavior;
import playground.mzilske.withinday.ActivityWorld;
import playground.mzilske.withinday.DrivingBehavior;
import playground.mzilske.withinday.DrivingWorld;
import playground.mzilske.withinday.RealAgent;
import playground.mzilske.withinday.TeleportationBehavior;
import playground.mzilske.withinday.TeleportationWorld;
import playground.mzilske.withinday.World;

/**
 * @author nagel
 *
 */
class MyRealAgent implements RealAgent {

	@Override
	public void doSimStep(World world) {
		// this is called if the agent is "in limbo".  If the agent is in one of the planes, the doSimStep of the
		// corresponding plane is called.

		boolean condition1=true, condition2=true, condition3=true ;
		
		// depending on some condition, start some action:
		if ( condition1 ) {
			world.getActivityPlane().startDoing(activityBehavior) ;
		} else if ( condition2 ) {
			world.getRoadNetworkPlane().startDriving(drivingBehavior) ;
		} else if ( condition3 ) {
			world.getTeleportationPlane().startTeleporting(teleportationBehavior) ;
		}
	}
		
	// the behavior for the actions is defined here:
	
	ActivityBehavior activityBehavior = new ActivityBehavior() {

		@Override
		public void doSimStep(ActivityWorld activityWorld) {
			// what can I do while being at an activity?
			
			activityWorld.stopActivity() ;
			
		}

		@Override
		public String getActivityType() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

	} ;

	DrivingBehavior drivingBehavior = new DrivingBehavior() {

		@Override
		public void doSimStep(DrivingWorld drivingWorld) {
			// what can I do while driving?
			
			Id linkId = null ;
			drivingWorld.nextTurn(linkId) ;
			
			drivingWorld.park() ;
			
			drivingWorld.requiresAction() ; // true if nextLinkId is not known
			
		}

	} ;

	TeleportationBehavior teleportationBehavior = new TeleportationBehavior() {

		@Override
		public void doSimStep(TeleportationWorld teleWorld) {
			// what can I do while teleporting?
			
			teleWorld.stop() ; // but this is technically difficult
			
		}

		@Override
		public Id getDestinationLinkId() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public String getMode() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

		@Override
		public double getTravelTime() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException() ;
		}

	} ;


}
