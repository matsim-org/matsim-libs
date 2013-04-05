package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleCostInformation;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * The carrier vehicle type.
 * 
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus physical and
 * non physical attributes are used. This is likely to be changed in future.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleType extends VehicleTypeImpl {

	/**
	 * A builder building the type.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * 
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id typeId){
			return new Builder(typeId);
		}
		
		private Id typeId;
		private double fix = 0.0;
		private double perDistanceUnit = 1.0;
		private double perTimeUnit = 0.0;
		private String description;
		private EngineInformation engineInfo;
		
		public Builder(Id typeId){
			this.typeId = typeId;
		}
		
		public Builder setFixCost(double fix){
			this.fix = fix;
			return this;
		}
		
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.perDistanceUnit = perDistanceUnit;
			return this;
		}
		
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.perTimeUnit = perTimeUnit;
			return this;
		}
		
		public Builder setDescription(String description){
			this.description = description;
			return this;
		}
		
		public CarrierVehicleType build(){
			return new CarrierVehicleType(this);
		}

		public Builder setVehicleCostInformation(VehicleCostInformation info) {
			fix = info.fix;
			perDistanceUnit = info.perDistanceUnit;
			perTimeUnit = info.perTimeUnit;
			return this;
		}

		public Builder setEngineInformation(EngineInformation engineInfo) {
			this.engineInfo = engineInfo;
			return this;
		}
	}
	
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

	private VehicleCostInformation vehicleCostInformation = new VehicleCostInformation(0.0, 1.0, 0.0);

//	private int capacity;
	
	private CarrierVehicleType(Id typeId) {
		super(typeId);
	}
	
	private CarrierVehicleType(Builder builder){
		super(builder.typeId);
		this.vehicleCostInformation = new VehicleCostInformation(builder.fix, builder.perDistanceUnit, builder.perTimeUnit);
		if(builder.engineInfo != null) this.setEngineInformation(builder.engineInfo);
		if(builder.description != null) this.setDescription(builder.description);
	}

	public void setVehicleCostParams(VehicleCostInformation vehicleCosts) {
		this.vehicleCostInformation = vehicleCosts;
	}

	/**
	 * Returns the cost values for this vehicleType.
	 * 
	 * If cost values are not explicitly set, the defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
	 * 
	 * @return vehicleCostInformation
	 */
	public VehicleCostInformation getVehicleCostInformation() {
		return vehicleCostInformation;
	}
	
}
