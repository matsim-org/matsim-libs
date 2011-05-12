/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAgent
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
package playground.dgrether.designdrafts.qsim;

import org.matsim.api.core.v01.Id;


/**
 * TODO is this really an interface only for public transit or
 * could this interface also be used for other passenger e.g.
 * agents driving in cars? should be considered before method
 * names are fixed.
 * @author dgrether
 *
 */
public interface PassengerAgent extends Agent  {
	
	/**
	 * TODO what is the return type of this method, 
	 * should also be called Id??
	 */
	public void getEnterTransitRoute();
	
	public Id getExitAtStopId();
}
