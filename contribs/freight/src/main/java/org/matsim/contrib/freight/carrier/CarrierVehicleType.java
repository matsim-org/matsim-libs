package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.utils.objectattributes.attributable.Attributes;
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
public class CarrierVehicleType {
	// this is now really only a name space for the builder method.  There are two options where this could go:
	// (1) into CarrierUtils; then it could keep its freight specific syntax
	// (2) into VehicleUtils; then it would need to lose its freight specific syntax
	// However, note that when moving it, the class here still needs to be available since otherwise a lot of outside code will break.
	// kai, sep'19

	private CarrierVehicleType(){} // do not instantiate

	/**
	 * A builder building the type.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		VehicleType delegate ;
		
		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * 
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTimeUnit=0.0].
		 * 
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId){
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
		public static Builder newInstance(Id<VehicleType> typeId, CarrierVehicleType carrierVehicleType){
//			return new Builder(typeId)
//					.setDescription(carrierVehicleType.getDescription())
//					.setEngineInformation(carrierVehicleType.getEngineInformation())
//					.setCapacity(carrierVehicleType.getCarrierVehicleCapacity())
//					.setMaxVelocity(carrierVehicleType.getMaximumVelocity())
//					.setVehicleCostInformation(carrierVehicleType.getVehicleCostInformation());
			throw new RuntimeException("not implemented") ;
		}
		
		private Builder(Id<VehicleType> typeId){
			this.delegate = VehicleUtils.getFactory().createVehicleType( typeId ) ;
		}
		
		/**
		 * Sets fixed costs of vehicle.
		 * 
		 * <p>By default it is 0.
		 * @param fix
		 * @return
		 */
		public Builder setFixCost(double fix){
			this.delegate.getCostInformation().setFixedCost( fix ) ;
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
			this.delegate.getCostInformation().setCostsPerMeter( perDistanceUnit ) ;
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
			this.delegate.getCostInformation().setCostsPerSecond( perTimeUnit ) ;
			return this;
		}
		
		/**
		 * Sets description.
		 * 
		 * @param description
		 * @return this builder
		 */
		public Builder setDescription(String description){
			this.delegate.setDescription( description ) ;
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
		public Builder setCapacity(int capacity){
			this.delegate.getCapacity().setOther( capacity );
			return this;
		}
		
		/**
		 * Builds the type.
		 * 
		 * @return {@link CarrierVehicleType}
		 */
		public VehicleType build(){
			return delegate ;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.delegate.setMaximumVelocity( veloInMeterPerSeconds ) ;
			return this;
		}
	}


}
