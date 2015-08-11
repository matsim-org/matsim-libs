/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.optimizer;

import java.util.Collection;
import java.util.Set;

import org.matsim.contrib.dvrp.data.Vehicle;

import playground.michalm.taxi.data.TaxiRequest;
import playground.michalm.taxi.optimizer.AbstractTaxiOptimizer;

/**
 * @author  jbischoff
 *
 */
public class TaxibusOptimizer extends AbstractTaxiOptimizer {
	
    private Set<Vehicle> possibleVehicles;

	

	public TaxibusOptimizer(TaxibusOptimizerConfiguration optimConfig, Collection<TaxiRequest> unplannedRequests,
			boolean doUnscheduleAwaitingRequests) {
		super(optimConfig, unplannedRequests, doUnscheduleAwaitingRequests);
	}

	@Override
	protected void scheduleUnplannedRequests() {
		initPossibleVehicles();
		
	}
	
	private void initPossibleVehicles(){
		//check if vehicle is either freely available or can take more passengers
	}
	

}
