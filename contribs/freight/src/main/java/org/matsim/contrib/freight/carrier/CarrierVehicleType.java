package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.*;

/**
 * The carrier vehicle type.
 * 
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus physical and
 * non physical attributes are used. This is likely to be changed in future.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleType extends VehicleType {

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
		public static Builder newInstance(Id<org.matsim.vehicles.VehicleType> typeId){
			return new Builder(typeId);
		}
		
		/**
		 * Returns a new instance of builder initialized with the typeId and the values the given from existing CarrierVehicleType.
		 * 
		 * Can be used for create a new, modified CarrierVehicleType basing on an existing one. 
		 * Values can be changed within the builder afterwards.
		 * 
		 * @param carrierVehicleType
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<org.matsim.vehicles.VehicleType> typeId, CarrierVehicleType carrierVehicleType){
			return new Builder(typeId)
					.setDescription(carrierVehicleType.getDescription())
					.setEngineInformation(carrierVehicleType.getEngineInformation())
					.setCapacityWeightInTons( carrierVehicleType.getCapacity().getWeightInTons() )
					.setMaxVelocity(carrierVehicleType.getMaximumVelocity())
					.setVehicleCostInformation(carrierVehicleType.getCostInformation());
		}
		
		private Id<org.matsim.vehicles.VehicleType> typeId;
		private double fix = 0.0;
		private double perDistanceUnit = 1.0;
		private double perTimeUnit = 0.0;
		private String description;
		private EngineInformation engineInfo;
		private double weightInTons = 0;
		private double maxVeloInMeterPerSeconds = Double.MAX_VALUE;
		
		
		private Builder(Id<org.matsim.vehicles.VehicleType> typeId){
			this.typeId = typeId;
		}
		
		/**
		 * Sets fixed costs of vehicle.
		 * 
		 * <p>By default it is 0.
		 * @param fix
		 * @return
		 */
		public Builder setFixCost(double fix){
			this.fix = fix;
			return this;
		}
		
		/**
		 * Sets costs per distance-unit.
		 * 
		 * <p>By default it is 1.
		 * 
		 * @param perDistanceUnit
		 * @return
		 */
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.perDistanceUnit = perDistanceUnit;
			return this;
		}
		
		/**
		 * Sets costs per time-unit.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perTimeUnit
		 * @return
		 */
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.perTimeUnit = perTimeUnit;
			return this;
		}
		
		/**
		 * Sets description.
		 * 
		 * @param description
		 * @return this builder
		 */
		public Builder setDescription(String description){
			this.description = description;
			return this;
		}
		
		/**
		 * Sets the capacity of vehicle-type.
		 * 
		 * <p>By defaul the capacity is 0.
		 * 
		 * @param capacity
		 * @return this builder
		 */
		public Builder setCapacityWeightInTons( double capacity ){
			this.weightInTons = capacity;
			return this;
		}
		
		/**
		 * Builds the type.
		 * 
		 * @return {@link CarrierVehicleType}
		 */
		public CarrierVehicleType build(){
			return new CarrierVehicleType(this);
		}

		/**
		 * Sets {@link CostInformation}
		 * 
		 * <p>The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param info
		 * @return this builder
		 */
		public Builder setVehicleCostInformation(CostInformation info) {
			fix = info.getFixedCosts();
			perDistanceUnit = info.getCostsPerMeter();
			perTimeUnit = info.getCostsPerSecond();
			return this;
		}

		/**
		 * Sets {@link EngineInformation}
		 * 
		 * @param engineInfo
		 * @return this builder
		 */
		public Builder setEngineInformation(EngineInformation engineInfo) {
			this.engineInfo = engineInfo;
			return this;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.maxVeloInMeterPerSeconds  = veloInMeterPerSeconds;
			return this;
		}
	}

	private CarrierVehicleType(Builder builder){
		super(builder.typeId);
		super.setCostInformation(new CostInformationImpl(builder.fix, builder.perDistanceUnit, builder.perTimeUnit));
		if(builder.engineInfo != null) super.setEngineInformation(builder.engineInfo);
		if(builder.description != null) super.setDescription(builder.description);

//		capacity = builder.capacity;
		VehicleCapacity aCapacity = new VehicleCapacityImpl() ;
		aCapacity.setWeightInTons( builder.weightInTons );
		super.setCapacity( aCapacity );

		super.setMaximumVelocity(builder.maxVeloInMeterPerSeconds);
	}
	

	/**
	 * Returns the capacity of carrierVehicleType.
	 * 
	 * <p>This might be replaced in future by a more complex concept of capacity (considering volume and different units).
	 * 
	 * @return integer
	 */
	@Deprecated // use "inline method" in your code. kai, aug'19
	public Double getCarrierVehicleCapacity(){
		return this.getCapacity().getWeightInTons() ;
	}
	
}
