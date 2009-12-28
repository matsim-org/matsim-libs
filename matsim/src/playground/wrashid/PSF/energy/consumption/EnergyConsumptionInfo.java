package playground.wrashid.PSF.energy.consumption;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

import playground.wrashid.PSF.ParametersPSF;

public class EnergyConsumptionInfo {

	/*
	 * In [J]
	 */
	public static double getEnergyConsumption(Link link, double travelTime, int vehicleType){
		
		// testing scenrario
		if (ParametersPSF.isTestingModeOn()){
			return ParametersPSF.getTestingEnergyConsumptionPerLink();
		}
		
		
		// TODO: Also consider different types of vehciles here...
		
		double freeSpeed=link.getFreespeed(Time.UNDEFINED_TIME); //[m/s]
		double vehicleSpeed=link.getLength()/travelTime; //[m/s]
		
		return ParametersPSF.getAverageEnergyConsumptionBins().getEnergyConsumption(vehicleSpeed, link.getLength());
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
