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
package org.matsim.contrib.freight.vrp.algorithms.rr.recreate;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.RouteAgent;
import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

/**
 * 
 * @author stefan schroeder
 * 
 */

public interface RecreationStrategy {

	public class Insertion {
		
		public RouteAgent getAgent() {
			return agent;
		}

		public InsertionData getInsertionData() {
			return insertionData;
		}

		private RouteAgent agent;
		
		private InsertionData insertionData;

		public Insertion(RouteAgent agent, InsertionData insertionData) {
			super();
			this.agent = agent;
			this.insertionData = insertionData;
		}
		
	}

	
	/**
	 * Assigns the unassigned jobs to service-providers
	 * 
	 * @param vehicleRoutes
	 * @param unassignedJobs
	 * @param result2beat
	 */
	public void recreate(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs, double result2beat);

}
