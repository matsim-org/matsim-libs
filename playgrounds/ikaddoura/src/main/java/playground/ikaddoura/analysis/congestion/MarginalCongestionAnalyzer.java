/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
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

/**
 * 
 */
package playground.ikaddoura.analysis.congestion;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * @author Ihab
 *
 */
public class MarginalCongestionAnalyzer implements  CongestionEventHandler {

	private double delaySum = 0;
	
	@Override
	public void reset(int iteration) {
		this.delaySum = 0.;
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		delaySum = delaySum + event.getDelay();
	}

	public double getDelaySum() {
		return delaySum;
	}

}
