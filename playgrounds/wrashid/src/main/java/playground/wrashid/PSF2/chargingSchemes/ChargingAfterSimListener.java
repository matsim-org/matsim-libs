/* *********************************************************************** *
 * project: org.matsim.*
 * DumbChargingAfterSim.java
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

package playground.wrashid.PSF2.chargingSchemes;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF2.ParametersPSF2;

public class ChargingAfterSimListener implements AfterMobsimListener {

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		ParametersPSF2.activityIntervalTracker.handleLastActivity();		
	}

}
