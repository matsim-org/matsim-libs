/* *********************************************************************** *
 * project: org.matsim.*
 * Perceptor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withinday_evac.beliefs;

import playground.gregor.withinday_evac.information.FollowGuideMessage;
import playground.gregor.withinday_evac.information.NextLinkMessage;

public abstract class Perceptor {
	
	public void addPerception(NextLinkMessage msg) {
		
	}
	
	public void addPerception(FollowGuideMessage msg) {
		
	}
	
	
	
	public abstract void reset();

}
