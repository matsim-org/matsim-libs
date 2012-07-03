package org.matsim.contrib.freight.carrier;

import org.matsim.contrib.freight.carrier.CarrierVehicleTypeImpl.VehicleCostInformation;
import org.matsim.vehicles.VehicleType;


public interface CarrierVehicleType extends VehicleType{
	
	public double getAllowableTotalWeight();
	
	public void setAllowableTotalWeight(double value);
	
	public double getTotalPayload();
	
	public void setTotalPayload(double value);
	
	public int getFreightCapacity();
	
	public void setFreightCapacity(int value);

	public void setVehicleCostParams(VehicleCostInformation currentVehicleCosts);
	
	public VehicleCostInformation getVehicleCostParams();

}
