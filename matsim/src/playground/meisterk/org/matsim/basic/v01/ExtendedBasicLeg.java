/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.basic.v01;

import org.matsim.interfaces.basic.v01.BasicRoute;

/**
* @author meisterk
*/
public interface ExtendedBasicLeg {

	public enum Mode {
		miv(true), 
		car(true), 
		ride(false), 
		motorbike(true), 
		pt(false), 
		train(false), 
		bus(false), 
		tram(false), 
		bike(true), 
		walk(false), 
		undefined(false);
	
		/**
		 * This variable is used to differentiate chain-based and trip-based modes. 
		 * It can be used to perform chain-based/tour-based mode choice analysis and simulation. 
		 * If a car, bike or other individual means of transport is to be used on a tour, 
		 * it must be used for the entire chain, since the car/bike must be 
		 * returned home or to the respective anchor point at the end of the tour ("chain-based modes"). 
		 * Furthermore, a chain-based mode cannot be used if it is not available at the anchor point
		 * at the beginning of the tour.
		 * No such constraints, however, exists with respect to other modes such as walk and public transport modes,
		 * ("trip-based modes").
		 * 
		 * [TODO] One could discuss if "ride" is a trip-based or a chain-based mode.
		 * 
		 * See also: Miller, E. J., M. J. Roorda and J. A. Carrasco (2005) A tour-based model of travel mode choice,
		 * Transportation, 32 (4) 399–422.
		 */
		private boolean isChainBased;

		private Mode(boolean isChainBased) {
			this.isChainBased = isChainBased;
		}

		public boolean isChainBased() {
			return isChainBased;
		} 
	
	}

	public Mode getMode();

	public void setMode(Mode mode);

	public BasicRoute getRoute();

	public void setRoute(BasicRoute route);

	public double getDepartureTime();

	public void setDepartureTime(final double seconds);

	public double getTravelTime();

	public void setTravelTime(final double seconds);

	public double getArrivalTime();

	public void setArrivalTime(final double seconds);


}