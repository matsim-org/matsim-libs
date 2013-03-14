package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

public final class CarrierShipment {

	public static class TimeWindow {
		
		public static TimeWindow newInstance(double start, double end){
			return new TimeWindow(start,end);
		}

		private final double start;

		private final double end;

		public TimeWindow(final double start, final double end) {
			this.start = start;
			this.end = end;
		}

		public double getStart() {
			return start;
		}

		public double getEnd() {
			return end;
		}

		@Override
		public String toString() {
			return "[start=" + start + ", end=" + end + "]";
		}

	}
	
	public static class Builder {
		
		public static Builder newInstance(Id from, Id to, int size){
			return new Builder(from,to,size);
		}
		
		Id from;
		Id to;
		int size;
		TimeWindow pickTW = TimeWindow.newInstance(0.0, Double.MAX_VALUE);
		TimeWindow delTW = TimeWindow.newInstance(0.0, Double.MAX_VALUE);
		double pickServiceTime = 0.0;
		double delServiceTime = 0.0;
		
		public Builder(Id from, Id to, int size) {
			super();
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
	
	private final Id from;

	private final Id to;

	private final int size;

	private final TimeWindow pickupTimeWindow;

	private final TimeWindow deliveryTimeWindow;

	private double pickupServiceTime;

	private double deliveryServiceTime;

	public CarrierShipment(final Id from, final Id to, final int size, final TimeWindow pickupTimeWindow, final TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickupTimeWindow = pickupTimeWindow;
		this.deliveryTimeWindow = deliveryTimeWindow;
	}

	private CarrierShipment(Builder builder) {
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

	public Id getFrom() {
		return from;
	}

	public Id getTo() {
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
		return "[from=" + from.toString() + "][to=" + to.toString() + "][size="
				+ size + "]";
	}

}
