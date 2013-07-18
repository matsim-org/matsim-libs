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

import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.taxi.optimizer.rank.RankTaxiOptimizer;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.taxi.TaxiSimEngine;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class ElectricTaxiSimEngine extends TaxiSimEngine {
	private DepotArrivalDepartureCharger dac;
	private RankTaxiOptimizer optimizer;

	public ElectricTaxiSimEngine(Netsim netsim, MatsimVrpData data,
			RankTaxiOptimizer optimizer, DepotArrivalDepartureCharger dac) {
		super(netsim, data, optimizer);
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
