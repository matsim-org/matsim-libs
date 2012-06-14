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

public class DriverCostParams implements CostParams {


	public double transportCost_per_second;
	
	public double transportCost_per_meter;
	
	public double waitingCost_per_second;
	
	public double serviceCost_per_second;
	
	public double penality_per_secondTooLate;
	
	public double fixCost_per_vehicleService;

	public DriverCostParams() {
		super();
	}

	public DriverCostParams(double transportCostPerSecond,
			double transportCostPerMeter, double waitingCostPerSecond,
			double serviceCostPerSecond, double penalityPerSecondTooLate,
			double costPerVehicle) {
		super();
		transportCost_per_second = transportCostPerSecond;
		transportCost_per_meter = transportCostPerMeter;
		waitingCost_per_second = waitingCostPerSecond;
		serviceCost_per_second = serviceCostPerSecond;
		penality_per_secondTooLate = penalityPerSecondTooLate;
		fixCost_per_vehicleService = costPerVehicle;
	}
	
	
}
