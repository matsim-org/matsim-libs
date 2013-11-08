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
public interface EnergyCalculator{
		
	public double getEnergyConsumption(double maxSize, double currentLoad);

	public class EnergyCalculatorImpl implements EnergyCalculator{
		
		private double additional;
		private double baseLoad;
		private double td;

		/**
		 * 
		 */
		public EnergyCalculatorImpl(double td, double baseLoadPerPerson, double additionalLoadPerPerson) {
			this.td = td;
			this.baseLoad = baseLoadPerPerson;
			this. additional = additionalLoadPerPerson;
		}
		
		@Override
		public double getEnergyConsumption(double maxSize, double currentLoad) {
			double baseload = this.baseLoad * maxSize;
			double additionalLoad = this.additional * maxSize;
			
			return (td * (baseload + additionalLoad * (1 - Math.exp(-currentLoad / maxSize))));
		}
		
	}
	
}

