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
package playground.vsp.buildingEnergy.energyCalculation;




/**
 * @author droeder
 *
 */
public interface EnergyConsumptionCalculator{
	
	/**
	 * 
	 * @param maxSize
	 * @param currentOccupancy
	 * @return energy consumption in kWh
	 */
	public double getEnergyConsumption_kWh(double maxSize, double currentOccupancy);

	
	public class OfficeEnergyConsumptionCalculatorImpl implements EnergyConsumptionCalculator{
		
		private double additional;
		private double baseLoad;
		private double td;
		private double someCoefficient;

		/**
		 * 
		 * @param td, duration timeslice [s]
		 * @param baseLoadPerPerson [kW]
		 * @param additionalLoadPerPerson [kW]
		 */
		public OfficeEnergyConsumptionCalculatorImpl(double td, double baseLoadPerPerson, double additionalLoadPerPerson, double someCoefficient) {
			this.td = td;
			this.baseLoad = baseLoadPerPerson;
			this. additional = additionalLoadPerPerson;
			this.someCoefficient = someCoefficient;
		}
		
		@Override
		public double getEnergyConsumption_kWh(double maxSize, double currentOccupancy) {
			if(currentOccupancy > maxSize) throw new RuntimeException("more persons on the link than expected");
			if(maxSize == 0) return 0.;
			double baseload = this.baseLoad * maxSize;
			double additionalLoad = this.additional * maxSize;
			return (td / 3600. * (baseload + additionalLoad * (1 - Math.exp(-1.0 * currentOccupancy / maxSize * someCoefficient))));
		}
		
	}
	
}

