package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * 
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicle implements Vehicle {

	/**
	 * Returns a new instance of carrierVehicle.
	 * 
	 * The default values for other fields (being implicitly set) are [capacity=0][earliestStart=0.0][latestEnd=Integer.MaxValue()].
	 * 
	 * @param vehicleId
	 * @param locationId
	 * @return CarrierVehicle
	 * @see CarrierVehicle
	 */
	public static CarrierVehicle newInstance(Id<Vehicle> vehicleId, Id<Link> locationId){
		return new CarrierVehicle(vehicleId, locationId);
	}
	@Deprecated // refactoring device, please inline
	public Id<Vehicle> getVehicleId(){
		return getId() ;
	}
	@Deprecated // refactoring device, please inline
	public void setVehicleType( VehicleType collectionType ){
		setType( collectionType );
	}

	/**
	 * Builder to build vehicles.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a builder with vehicleId and locationId.
		 * 
		 * The default values for other fields (being implicitly set) are [capacity=0][earliestStart=0.0][latestEnd=Integer.MaxValue()].
		 * 
		 * @param vehicleId
		 * @param locationId
		 * @return a new vehicle builder
		 */
		public static Builder newInstance(Id<Vehicle> vehicleId, Id<Link> locationId){
			return new Builder(vehicleId,locationId);
		}
		
		private Id<Link> locationId;
		private Id<Vehicle> vehicleId;
		private VehicleType type;
		private Id<org.matsim.vehicles.VehicleType> typeId;
		private double earliestStart = 0.0;
		private double latestEnd = Integer.MAX_VALUE;
		
		
		public Builder(Id<Vehicle> vehicleId, Id<Link> locationId){
			this.locationId = locationId;
			this.vehicleId = vehicleId;
		}
		
		public Builder setType( VehicleType type ){
			this.type=type;
			return this;
		}
		
		
		public Builder setTypeId(Id<org.matsim.vehicles.VehicleType> typeId ){
			this.typeId = typeId;
			return this;
		}
		
		
		public Builder setEarliestStart(double earliestStart){
			this.earliestStart=earliestStart;
			return this;
		}
		
		public Builder setLatestEnd(double latestEnd){
			this.latestEnd = latestEnd;
			return this;
		}
		
		public CarrierVehicle build(){
			return new CarrierVehicle(this);
		}
	}
	
	private final Id<Link> locationId;

	private final Id<Vehicle> vehicleId;
	
	private Id<org.matsim.vehicles.VehicleType> typeId;

	private VehicleType vehicleType;

	private final Attributes attributes = new Attributes();

	private double earliestStartTime;

	private double latestEndTime;

	private CarrierVehicle(final Id<Vehicle> vehicleId, final Id<Link> location) {
		this.vehicleId = vehicleId;
		this.locationId = location;
		earliestStartTime = 0.0;
		latestEndTime = Integer.MAX_VALUE;
	}
	
	private CarrierVehicle(Builder builder){
		vehicleId = builder.vehicleId;
		locationId = builder.locationId;
		vehicleType = builder.type;
		earliestStartTime = builder.earliestStart;
		latestEndTime = builder.latestEnd;
		typeId = builder.typeId;
	}

	public Id<Link> getLocation() {
		return locationId;
	}
	@Override
	public Id<Vehicle> getId() {
		return vehicleId;
	}
	
	@Override
	public String toString() {
		return vehicleId + " stationed at " + locationId;
	}
	@Override
	public VehicleType getType() {
		return vehicleType;
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}


	/**
	 * @deprecated -- set in builder and do not change afterwards.  kai, nov'20
	 */
	public CarrierVehicle setType( VehicleType vehicleType ) {
		this.vehicleType = vehicleType;
		return this;
	}

	/**
	 * Returns the earliest time a vehicle can be deployed (and thus can departure from its origin).
	 * 
	 * The default value is 0.0;
	 * 
	 * @return the earliest start time
	 */
	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	/**
	 * Returns the latest time a vehicle has to be back in the depot (and thus has to arrive at its final destination).
	 * 
	 * The default value is Integer.MaxValue().
	 * 
	 * @return latest arrival time
	 */
	public double getLatestEndTime() {
		return latestEndTime;
	}

	
	public Id<org.matsim.vehicles.VehicleType> getVehicleTypeId() {
		return typeId;
	}

}
