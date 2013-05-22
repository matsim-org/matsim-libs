package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;


public class CarrierService  {

	public static class Builder {
		
		public static Builder newInstance(Id id, Id locationLinkId){
			return new Builder(id,locationLinkId);
		}
		
		private Id id;
		private Id locationLinkId;
		private String name = "service";
		
		private double serviceTime = 0.0;
		private TimeWindow timeWindow = TimeWindow.newInstance(0.0, Double.MAX_VALUE);
		private int capacityDemand = 0;
		
		private Builder(Id id, Id locationLinkId) {
			super();
			this.id = id;
			this.locationLinkId = locationLinkId;
		}
		
		public Builder setName(String name){
			this.name = name;
			return this;
		}
		
		public Builder setServiceTime(double serviceTime){
			this.serviceTime = serviceTime;
			return this;
		}
		
		public Builder setTimeWindow(TimeWindow tw){
			this.timeWindow = tw;
			return this;
		}
		
		public CarrierService build(){
			return new CarrierService(this);
		}

		public Builder setCapacityDemand(int value) {
			this.capacityDemand = value;
			return this;
		}
		
	}
	
	
	private final Id id;

	private final Id locationId;
	
	private final String name;
	
	private final double serviceTime;

	private final TimeWindow timeWindow;

	private final int demand;

	private CarrierService(Builder builder){
		id = builder.id;
		locationId = builder.locationLinkId;
		serviceTime = builder.serviceTime;
		timeWindow = builder.timeWindow;
		demand = builder.capacityDemand;
		name = builder.name;
	}

	public Id getId() {
		return id;
	}

	public Id getLocationLinkId() {
		return locationId;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public TimeWindow getTimeWindow(){
		return timeWindow;
	}
	
	public int getCapacityDemand() {
		return demand;
	}
	
	/**
	 * @return the name
	 */
	public String getType() {
		return name;
	}

	@Override
	public String toString() {
		return "[id=" + id + "][locationId=" + locationId + "][capacityDemand=" + demand + "][serviceTime=" + serviceTime + "][timeWindow=" + timeWindow + "]";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CarrierService other = (CarrierService) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	
	
}
