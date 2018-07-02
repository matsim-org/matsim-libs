package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * A shipment from one location to another, with certain size and other constraints such as time-windows and service-times.
 * 
 * <p>Use the builder to build a shipment. 
 * @code CarrierShipment.Builder.newInstance(from,to,size) 
 * 
 * @author sschroeder
 *
 */
public final class CarrierShipment {

	/**
	 * A builder that builds shipments.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new shipment builder.
		 * 
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 * 
		 * @param from
		 * @param to
		 * @param size
		 * @return the builder
		 */
		public static Builder newInstance(Id<Link> from, Id<Link> to, int size){
			return new Builder(from,to,size);
		}
		
		/**
		 * Returns a new shipment builder.
		 * 
		 * <p> The builder is init with the shipment's origin (from), destination (to) and with the shipment's size.
		 * The default-value for serviceTime is 0.0. The default-value for a timeWindow is [start=0.0, end=Double.maxValue()].
		 * 
		 * @param id
		 * @param from
		 * @param to
		 * @param size
		 * @return the builder
		 */
		public static Builder newInstance(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size){
			return new Builder(id, from,to,size);
		}
		
		Id<CarrierShipment> id;
		Id<Link> from;
		Id<Link> to;
		int size;
		TimeWindow pickTW = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		TimeWindow delTW = TimeWindow.newInstance(0.0, Integer.MAX_VALUE);
		double pickServiceTime = 0.0;
		double delServiceTime = 0.0;
		
		public Builder(Id<Link> from, Id<Link> to, int size) {
			super();
			this.from = from;
			this.to = to;
			this.size = size;
		}
		
		public Builder(Id<CarrierShipment> id, Id<Link> from, Id<Link> to, int size) {
			super();
			this.id = id;
			this.from = from;
			this.to = to;
			this.size = size;
		}
		
		public Builder setPickupTimeWindow(TimeWindow pickupTW){
			this.pickTW = pickupTW;
			return this;
		}
		
		public Builder setDeliveryTimeWindow(TimeWindow deliveryTW){
			this.delTW = deliveryTW;
			return this;
		}
		
		public Builder setPickupServiceTime(double pickupServiceTime){
			this.pickServiceTime = pickupServiceTime;
			return this;
		}
		
		public Builder setDeliveryServiceTime(double deliveryServiceTime){
			this.delServiceTime = deliveryServiceTime;
			return this;
		}
		
		public CarrierShipment build(){
			return new CarrierShipment(this);
		}
	}
	
	private final Id<CarrierShipment> id;
	
	private final Id<Link> from;

	private final Id<Link> to;

	private final int size;

	private final TimeWindow pickupTimeWindow;

	private final TimeWindow deliveryTimeWindow;

	private double pickupServiceTime;

	private double deliveryServiceTime;

//	public CarrierShipment(final Id from, final Id to, final int size, final TimeWindow pickupTimeWindow, final TimeWindow deliveryTimeWindow) {
//		super();
//		this.from = from;
//		this.to = to;
//		this.size = size;
//		this.pickupTimeWindow = pickupTimeWindow;
//		this.deliveryTimeWindow = deliveryTimeWindow;
//	}

	private CarrierShipment(Builder builder) {
		id = builder.id;
		from = builder.from;
		to = builder.to;
		size = builder.size;
		pickupServiceTime = builder.pickServiceTime;
		deliveryServiceTime = builder.delServiceTime;
		pickupTimeWindow = builder.pickTW;
		deliveryTimeWindow = builder.delTW;
	}

	public double getPickupServiceTime() {
		return pickupServiceTime;
	}

	public void setPickupServiceTime(double pickupServiceTime) {
		this.pickupServiceTime = pickupServiceTime;
	}

	public double getDeliveryServiceTime() {
		return deliveryServiceTime;
	}

	public void setDeliveryServiceTime(double deliveryServiceTime) {
		this.deliveryServiceTime = deliveryServiceTime;
	}

	public Id<CarrierShipment> getId() {
		return id;
	}
	public Id<Link> getFrom() {
		return from;
	}

	public Id<Link> getTo() {
		return to;
	}

	public int getSize() {
		return size;
	}

	public TimeWindow getPickupTimeWindow() {
		return pickupTimeWindow;
	}

	public TimeWindow getDeliveryTimeWindow() {
		return deliveryTimeWindow;
	}

	@Override
	public String toString() {
		return "[from=" + from.toString() + "][to=" + to.toString() + "][size=" + size + "][pickupServiceTime=" + pickupServiceTime + "]" +
				"[deliveryServiceTime="+deliveryServiceTime+"][pickupTimeWindow="+pickupTimeWindow+"][deliveryTimeWindow="+deliveryTimeWindow+"]";
	}



}
