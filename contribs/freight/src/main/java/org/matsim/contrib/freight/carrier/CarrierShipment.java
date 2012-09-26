package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

public final class CarrierShipment {

	public static class TimeWindow {

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
	
	private final Id from;

	private final Id to;

	private final int size;

	private final TimeWindow pickupTimeWindow;

	private final TimeWindow deliveryTimeWindow;

	private double pickupServiceTime;

	private double deliveryServiceTime;

	public CarrierShipment(final Id from, final Id to, final int size,
			final TimeWindow pickupTimeWindow,
			final TimeWindow deliveryTimeWindow) {
		super();
		this.from = from;
		this.to = to;
		this.size = size;
		this.pickupTimeWindow = pickupTimeWindow;
		this.deliveryTimeWindow = deliveryTimeWindow;
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
