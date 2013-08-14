/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.fullParkingStrategies;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;

import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;

public class FakeStrategy implements FullParkingSearchStrategy {

	@Override
	public void applySearchStrategy(MobsimAgent agent, double time) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean acceptParking(MobsimAgent agent, Id facilityId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getStrategyName() {
		// TODO Auto-generated method stub
		return null;
	}

	

}

