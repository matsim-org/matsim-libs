/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.jsprit;

import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.controler.CarrierTimeAndSpaceTourRouter;

/**
 * Router that routes {@link CarrierPlan}.
 * 
 * @author stefan schröder
 *
 */
public class NetworkRouter {
	
	/**
	 * Routes the {@link CarrierPlan} with the router defined in {@link NetworkBasedTransportCosts}.
	 * 
	 * <p>Note that this changes the plan, i.e. it adds routes to the input-plan.
	 * 
	 * @param {@link CarrierPlan}
	 * @param {@link NetworkBasedTransportCosts}
	 */
	public static void routePlan(CarrierPlan plan, VRPTransportCosts freightTransportCosts){
		if( plan == null) throw new IllegalStateException("plan is missing.");
		for( ScheduledTour tour : plan.getScheduledTours()){
			new CarrierTimeAndSpaceTourRouter( freightTransportCosts.getRouter(), freightTransportCosts.getNetwork(), freightTransportCosts.getTravelTime()).route(tour );
		}
	}

}
