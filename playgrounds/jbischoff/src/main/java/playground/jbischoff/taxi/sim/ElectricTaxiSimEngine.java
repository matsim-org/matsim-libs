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

package playground.jbischoff.taxi.sim;

import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.core.mobsim.qsim.QSim;

import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.taxi.optimizer.rank.NOSRankTaxiOptimizer;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class ElectricTaxiSimEngine extends VrpSimEngine {
	private DepotArrivalDepartureCharger dac;
	private NOSRankTaxiOptimizer optimizer;

	public ElectricTaxiSimEngine(QSim qsim, MatsimVrpData data,
	        NOSRankTaxiOptimizer optimizer, DepotArrivalDepartureCharger dac) {
		super(qsim, data, optimizer);
		this.optimizer=optimizer;
		this.dac = dac;
	}

	@Override
	public void doSimStep(double time) {
		
		this.optimizer.doSimStep(time);
		this.dac.doSimStep(time);
		notifyAgentLogics();
	}
}
