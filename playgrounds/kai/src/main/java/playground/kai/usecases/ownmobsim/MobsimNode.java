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

package playground.kai.usecases.ownmobsim;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

class MobsimNode {
	Node originalNode ;
	
	MobsimNode(Node node) {
		this.originalNode = node ;
	}
	
	private List<MobsimLink> incomingLinks ;

	public void doSimStep() {
		for ( MobsimLink inLink : incomingLinks ) {
			DriverVehicleUnit vehicle = inLink.peek() ;
			if ( vehicle != null ) {
				Id nextLinkId = vehicle.getNextLinkId() ;
				MobsimLink outLink = null ; // (find outLink)
				if ( outLink.hasSpace() ) {
					inLink.remove() ;
					outLink.addFromIntersection( vehicle ) ;
				}
			}
			
		
		}
		
	}

}
