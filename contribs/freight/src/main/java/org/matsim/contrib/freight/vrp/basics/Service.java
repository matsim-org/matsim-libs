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


public class Service implements Job{
	
	private String id;
	
	private String locationId;
	
	private double serviceTime;

	public Service(String id, String locationId) {
		super();
		this.locationId = locationId;
		this.id = id;
	}

	public String getLocationId() {
		return locationId;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	@Override
	public String getId() {
		return id;
	}
	
}
