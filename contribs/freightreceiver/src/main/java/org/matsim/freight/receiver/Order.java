/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.freight.receiver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;

/**
 * A concrete assignment of a receiver product, order quantity, delivery
 * time windows, delivery service time and delivery location.
 *
 * Default value for delivery service time is 1 hour and daily order quantity is
 * 1000 units (5000 units per week, 5 deliveries per week).
 *
 * @author wlbean
 *
 * FIXME Receiver is both at Order and ReceiverOrder level. Necessary?
 */

public final class Order {
	final private Logger log = LogManager.getLogger(Order.class);

	private Id<Order> orderId;
	private Receiver receiver;
	private ReceiverProduct receiverProduct;
	private Double orderQuantity;
	private Double dailyOrderQuantity;
	private Double serviceTime;
	private double numDel = 5;

	@Override
	public String toString() {
		StringBuilder strb = new StringBuilder(  ) ;
		strb.append( "[orderId=" ).append( orderId ) ;

		strb.append("; serviceTime=").append( serviceTime ) ;

		strb.append( "]" ) ;
		return strb.toString() ;
	}


	/* protected */
	/*Order(Id<Order> orderId, Receiver receiver,
			ReceiverProduct receiverProduct, Double orderQuantity, Double serviceTime){
		this.orderId = orderId;
		this.receiver = receiver;
		this.receiverProduct = receiverProduct;
		this.orderQuantity = orderQuantity;
		this.serviceTime = serviceTime;
	}*/

	private Order(Builder builder){
		this.orderId = builder.orderId;
		this.orderQuantity = builder.orderQuantity;
		this.receiver = builder.receiver;
		this.receiverProduct = builder.receiverProduct;
		this.serviceTime = builder.serviceTime;
		this.numDel = builder.numDel;
		this.dailyOrderQuantity = builder.dOrderQuantity;
	}


	/**
	 * Returns the receiver product being ordered.
	 */

	public ReceiverProduct getProduct(){
		return receiverProduct;
	}

	/**
	 * Returns the receiver of this order.
	 */

	public Receiver getReceiver(){
		return receiver;
	}

	/**
	 * Returns the order id.
	 */

	public Id<Order> getId(){
		return orderId;
	}

	/**
	 * Returns the delivery service duration for a particular receiver order.
	 * @return
	 */
	public double getServiceDuration(){
		if(this.serviceTime == null) {
			log.warn("No service time set. Returning default of 00:01:00");
			return Time.parseTime("00:01:00");
		}
		return serviceTime;
	}

	/**
	 * Returns the order name.
	 * @return
	 */


	/**
	 * Returns the weekly order quantity in units.
	 * @return
	 */

	public double getOrderQuantity(){
		return orderQuantity;
	}

	/**
	 * Returns the daily order quantity in units.
	 * @return
	 */

	public double getDailyOrderQuantity(){
		return dailyOrderQuantity;
	}



	/**
	 * Returns the number of weekly deliveries of a receiver. The default is set to 5 since MATSim runs only for a single day, we assume there is a delivery of the specified Order quantity each day.
	 * @return
	 */
	public double getNumberOfWeeklyDeliveries(){
		return numDel;
	}

	/**
	 * Returns a single carrier service containing the order information.
	 *
	 * FIXME I (jwj) removed the time window since it is no longer a single
	 * value, but rather a list of time windows.
	 */
/*	@Override
	public String toString(){
		return "[id=" + orderId + "][linkId=" + receiver.getLinkId() + "][capacityDemand=" + dailyOrderQuantity + "][serviceDuration=" + Time.writeTime(serviceTime) + "]";
	}*/

	public Order createCopy() {
		return Builder.newInstance(orderId, receiver, receiverProduct)
				.setOrderQuantity(getOrderQuantity())
				.setServiceTime(getServiceDuration())
				.setNumberOfWeeklyDeliveries(getNumberOfWeeklyDeliveries())
				.setDailyOrderQuantity(getDailyOrderQuantity())
				.build();
	}

	/**
	 * A builder building a receiver order for one product type.
	 *
	 * @author wlbean
	 *
	 */

	public static class Builder {


		/**
		 * Returns a new instance of a receiver order.
		 */

		 public static Builder newInstance(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct){
			return new Builder(orderId, receiver, receiverProduct);
		}


		private Receiver receiver;
		private Id<Order> orderId;
		private ReceiverProduct receiverProduct;
		private Double serviceTime = null;
		private Double orderQuantity = null;
		private Double dOrderQuantity = null;
		private double numDel = 5;


		private Builder(final Id<Order> orderId, final Receiver receiver, final ReceiverProduct receiverProduct){
			this.orderId = orderId;
			this.receiver = receiver;
			this.receiverProduct = receiverProduct;
		}


		/**
		 * Sets the delivery service time.
		 * @param serviceTime
		 * @return
		 */
		public Builder setServiceTime(double serviceTime){
			this.serviceTime = serviceTime;
			return this;
		}


		public Builder setOrderQuantity(Double quantity) {
			this.orderQuantity = quantity;
			return this;
		}

		public Builder setNumberOfWeeklyDeliveries(double d) {
			this.numDel = d;
			return this;
		}

		public Builder setDailyOrderQuantity(Double quantity) {
			this.dOrderQuantity = quantity;
			return this;
		}



		/**
		 * Determines the order quantity in units based on the receivers min and max inventory levels (in units) and create a new order.
		 *
		 * This should be expanded later on when including demand rate for products.
		 */
		public Builder calculateOrderQuantity(){
			if(this.receiverProduct == null) {
				throw new RuntimeException("Cannot calculate order quantity before ProductType is set.");
			}

			ReorderPolicy policy = this.receiverProduct.getReorderPolicy();
			double orderQuantity = policy.calculateOrderQuantity(receiverProduct.getStockOnHand());
			//			int minLevel = receiverProduct.getMinLevel();
			//			int maxLevel = receiverProduct.getMaxLevel();
			//			double orderQuantity = (maxLevel - minLevel)*receiverProduct.getRequiredCapacity();
			this.orderQuantity = orderQuantity;
			this.dOrderQuantity = orderQuantity/this.numDel;
			return this;
		}


		public Order build(){
			return new Order(this);
		}
		/*public Order build(){
			return new Order(orderId, orderName, receiver, receiverProduct, orderQuantity, serviceTime);
		}
		*/


		public Order buildWithCalculatedOrderQuantity() {
			this.calculateOrderQuantity();
			return build();
		}
	}

	public Order setServiceDuration(double duration) {
		this.serviceTime = duration;
		return this;

	}


	public void setOrderQuantity(double sdemand) {
		this.orderQuantity = sdemand;

	}


	public void setNumberOfWeeklyDeliveries(double newNumDel) {
		this.numDel = newNumDel;

	}


	public void setDailyOrderQuantity(double sdemand) {
		this.dailyOrderQuantity = sdemand;

	}


}
