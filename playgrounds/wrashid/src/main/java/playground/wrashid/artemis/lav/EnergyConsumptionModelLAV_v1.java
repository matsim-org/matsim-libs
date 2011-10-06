package playground.wrashid.artemis.lav;

import org.matsim.api.core.v01.network.Link;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionModel;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.artemis.lav.EnergyConsumptionRegressionModel.EnergyConsumptionModelRow;
import playground.wrashid.lib.DebugLib;

public class EnergyConsumptionModelLAV_v1 {

	private EnergyConsumptionRegressionModel energyConsumptionRegressionModel;

	public EnergyConsumptionRegressionModel getRegressionModel(){
		return energyConsumptionRegressionModel;
	}
	
	public EnergyConsumptionModelLAV_v1(String fileNameEnergyConsumptionRegressionModel){
		energyConsumptionRegressionModel = new EnergyConsumptionRegressionModel(fileNameEnergyConsumptionRegressionModel);
	}
	
	public double getEnergyConsumptionForLinkInJoule(VehicleTypeLAV vehicle, double timeSpentOnLink, Link link) {
		double freespeedInKmPerHour = link.getFreespeed()/1000*3600;
		EnergyConsumptionModelRow vehicleEnergyConsumptionModel = energyConsumptionRegressionModel.getVehicleEnergyConsumptionModel(vehicle, freespeedInKmPerHour);
		
		double averageSpeedDrivenOnLinkInMeterPerSecond=link.getLength()/timeSpentOnLink;
		double energyConsumptionInJoulePerMeter = vehicleEnergyConsumptionModel.getEnergyConsumptionInJoulePerMeter(averageSpeedDrivenOnLinkInMeterPerSecond);
		
		
		
		return energyConsumptionInJoulePerMeter*link.getLength();
	}

}
