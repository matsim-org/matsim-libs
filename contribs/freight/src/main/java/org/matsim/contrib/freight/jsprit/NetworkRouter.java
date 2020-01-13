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
import org.matsim.contrib.freight.router.TimeAndSpacePlanRouter;

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
	public static void routePlan(CarrierPlan plan, NetworkBasedTransportCosts netbasedTransportCosts){
		new TimeAndSpacePlanRouter(netbasedTransportCosts.getRouter(), netbasedTransportCosts.getNetwork(), netbasedTransportCosts.getTravelTime()).run(plan);
	}

}
