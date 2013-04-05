package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

/**
 * 
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicle {

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
	public static CarrierVehicle newInstance(Id vehicleId, Id locationId){
		return new CarrierVehicle(vehicleId,locationId);
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
		public static Builder newInstance(Id vehicleId, Id locationId){
			return new Builder(vehicleId,locationId);
		}
		
		private Id location;
		private Id vehicleId;
		private CarrierVehicleType type;
		private Id typeId;
		private int capacity = 0;
		private double earliestStart = 0.0;
		private double latestEnd = Integer.MAX_VALUE;
		
		
		public Builder(Id vehicleId, Id locationId){
			this.location = locationId;
			this.vehicleId = vehicleId;
		}
		
		public Builder setType(CarrierVehicleType type){
			this.type=type;
			return this;
		}
		
		public Builder setTypeId(Id typeId){
			this.typeId = typeId;
			return this;
		}
		
		/**
		 * capacity should be part of vehicleType
		 */
		public Builder setCapacity(int capacity){
			this.capacity = capacity;
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
	
	private final Id location;

	private final Id vehicleId;
	
	private Id typeId;

	private CarrierVehicleType vehicleType;

	private int capacity;

	private boolean active = true;

	private double earliestStartTime;

	private double latestEndTime;

	private CarrierVehicle(final Id vehicleId, final Id location) {
		this.vehicleId = vehicleId;
		this.location = location;
		capacity = 0;
		earliestStartTime = 0.0;
		latestEndTime = Integer.MAX_VALUE;
	}
	
	private CarrierVehicle(Builder builder){
		vehicleId = builder.vehicleId;
		location=builder.location;
		vehicleType = builder.type;
		capacity = builder.capacity;
		earliestStartTime = builder.earliestStart;
		latestEndTime = builder.latestEnd;
		typeId = builder.typeId;
	}

	public Id getLocation() {
		return location;
	}

	public Id getVehicleId() {
		return vehicleId;
	}

	public int getCapacity() {
		return capacity;
	}

	@Deprecated
	public boolean isActive() {
		return active;
	}

//	public void setCapacity(int capacity) {
//		this.capacity = capacity;
//	}

	@Override
	public String toString() {
		return vehicleId + " stationed at " + location;
	}

//	public void setLatestEndTime(double endTime) {
//		this.latestEndTime = endTime;
//	}
//
//	public void setActive(boolean active) {
//		this.active = active;
//	}

	public CarrierVehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(CarrierVehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}
//
//	public void setEarliestStartTime(double startTime) {
//		this.earliestStartTime = startTime;
//	}

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

	public Id getVehicleTypeId() {
		return typeId;
	}

}
