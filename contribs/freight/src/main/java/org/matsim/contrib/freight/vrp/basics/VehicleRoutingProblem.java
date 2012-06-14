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
package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.Map;

import org.matsim.contrib.freight.vrp.constraints.Constraints;



public interface VehicleRoutingProblem{
	
	public Collection<Vehicle> getVehicles();
	
	public Map<String,Job> getJobs();
	
	public Constraints getGlobalConstraints();
	
	public Costs getCosts();
	
	public Locations getLocations();

}
