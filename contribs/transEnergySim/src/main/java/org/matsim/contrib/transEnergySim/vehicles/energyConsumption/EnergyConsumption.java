package org.matsim.contrib.transEnergySim.vehicles.energyConsumption;

import java.io.Serializable;

/**
 * container for energy consumption for driving at given given speed.
 * 
 * @author rashid_waraich
 */
public class EnergyConsumption implements Comparable<EnergyConsumption>,Serializable {
	private double speedInMeterPerSecond=0;
	private double energyConsumptionInJoule=0; // consumed energy in [J] (by driving one meter with the given speed)
	
	public EnergyConsumption(double speed, double energyConsumption){
		this.speedInMeterPerSecond=speed;
		this.energyConsumptionInJoule=energyConsumption;
	}
	
	public double getSpeedDifference(double otherSpeed){
		return Math.abs(speedInMeterPerSecond-otherSpeed);
	}
	
	public double getEnergyConsumption(){
		return energyConsumptionInJoule;
	}
	
	public double getSpeed(){
		return speedInMeterPerSecond;
	}

	public int compareTo(EnergyConsumption otherConsumption) {
		if (this.speedInMeterPerSecond<otherConsumption.speedInMeterPerSecond){
			return -1;
		} else if (this.speedInMeterPerSecond>otherConsumption.speedInMeterPerSecond){
			return 1;
		}
		return 0;
	}
}
