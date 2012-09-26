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
package org.matsim.contrib.freight.vrp.algorithms.rr;

import java.util.Collection;

import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.ServiceProviderAgent;
import org.matsim.contrib.freight.vrp.basics.Job;

/**
 * 
 * @author stefan schroeder
 * 
 */

public interface RecreationStrategy {

	/**
	 * Assigns the unassigned jobs to service-providers
	 * 
	 * @param serviceProviders
	 * @param unassignedJobs
	 * @param result2beat
	 */
	public void recreate(
			Collection<? extends ServiceProviderAgent> serviceProviders,
			Collection<Job> unassignedJobs, double result2beat);

}
