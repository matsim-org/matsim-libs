package playground.wrashid.PSF.energy.consumption;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

public class EnergyConsumptionInfo {

	/*
	 * In [J]
	 */
	public static double getEnergyConsumption(Link link, double travelTime, int vehicleType){
		double freeSpeed=link.getFreespeed(Time.UNDEFINED_TIME); //[m/s]
		double vehicleSpeed=link.getLength()/travelTime; //[m/s]
		
		// TODO: read some data a file. For example for different velocities there is a different
		// energy consumption
		
		// how much energy in Jules is consumed by driving the vehicleType one meter
		// with the given speed
		
		// see playground.wrashid.PHEV.Utility.AverageSpeedEnergyConsumption
		
		// TODO: this is speed dependent!!!
		double energyConsumptionPerMeter=4.784535E+02; // [J/m] 
		
		// TODO: change this, the speed should get a role here!!!
		return link.getLength()*energyConsumptionPerMeter;
	}
	
	/**
	 * TODO: This info should be read from a file initially
	 * TODO: a person can have multiple cars, therefore a new interface may be needed later,
	 * where we can give the car as parameter, which is being driven for a leg.
	 * 
	 * @param personId
	 * @return
	 */
	public static int getVehicleType(Id personId){
		return 0;
	}
	
	
}
