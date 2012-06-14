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

public class DriverCostFunction {
	
	public final DriverCostParams driverCostParams;
	
	private double transportTime = 0.0;
	
	private double transportDistance = 0.0;
	
	private boolean inService = false;
	
	public double getTransportTime() {
		return transportTime;
	}

	public DriverCostFunction(DriverCostParams driverCostParams) {
		super();
		this.driverCostParams = driverCostParams;
	}
	
	public void reset(){
		transportTime = 0.0;
		transportDistance = 0.0;
		inService(false);
	}
	
	public void inService(boolean inService){
		this.inService=inService;
	}
	
	public void addTransportDistance(double distance){
		transportDistance += distance;
	}
	
	public void addTransportTime(double time){
		transportTime += time;
	}
	
	public double getTotalCosts() {
		if(!inService){
			return 0.0;
		}
		return driverCostParams.fixCost_per_vehicleService + transportTime*driverCostParams.transportCost_per_second + transportDistance*driverCostParams.transportCost_per_meter;
	}

}
