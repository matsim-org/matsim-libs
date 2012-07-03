package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleTypeImpl;


public class CarrierVehicleTypeImpl extends VehicleTypeImpl implements CarrierVehicleType{

	public static class VehicleCostInformation {
		
		public final double fix;
		public final double perDistanceUnit;
		public final double perTimeUnit;
		
		public VehicleCostInformation(double fix, double perDistanceUnit, double perTimeUnit) {
			super();
			this.fix = fix;
			this.perDistanceUnit = perDistanceUnit;
			this.perTimeUnit = perTimeUnit;
		}
		
	}
	
	private double allowableWeight = 12;
	
	private double payload = 12;
	
	private int freightCap = 0;
	
	private VehicleCostInformation vehicleCostInformation = new VehicleCostInformation(0.0, 1.0, 0.0);
	
	public CarrierVehicleTypeImpl(Id typeId) {
		super(typeId);
	}

	public void setVehicleCostParams(VehicleCostInformation vehicleCosts) {
		this.vehicleCostInformation = vehicleCosts;
	}

	@Override
	public double getAllowableTotalWeight() {
		return allowableWeight;
	}

	@Override
	public void setAllowableTotalWeight(double value) {
		this.allowableWeight = value;
	}

	@Override
	public double getTotalPayload() {
		return payload;
	}

	@Override
	public void setTotalPayload(double value) {
		this.payload = value;
	}

	@Override
	public int getFreightCapacity() {
		return freightCap;
	}



	@Override
	public void setFreightCapacity(int value) {
		this.freightCap = value;
	}

	@Override
	public VehicleCostInformation getVehicleCostParams() {
		return vehicleCostInformation;
	}


}
