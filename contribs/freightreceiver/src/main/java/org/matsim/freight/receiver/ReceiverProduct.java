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

/**
 * Returns a new instance of a receiver product with associated information,
 * such as location, order policy parameters (min and max levels) and possibly
 * demand rate (to be included later).
 * <p><p>
 * The default values are: min level = 1000 units, max level = 5000 units.
 *
 * @author wlbean, jwjoubert
 */
public class ReceiverProduct {

	private final ReorderPolicy policy;
	private final double stockOnHand;
	private final ProductType productType;


	private ReceiverProduct(Builder builder){
		this.productType = builder.productType;
		this.policy = builder.policy;
		this.stockOnHand = builder.onHand;
	}


	/**
	 * Returns receiver product type.
	 */
	public ProductType getProductType(){
		return productType;
	}


	public ReorderPolicy getReorderPolicy() {
		return this.policy;
	}

	public double getStockOnHand() {
		return stockOnHand;
	}


	/**
	 * A builder that is used to build the product instance for the receiver.
	 * <p>
	 * FIXME There are multiple ways to create/set things. And, reading from the
	 * XML file means that you must read the ReceiverProduct first, BEFORE you
	 * get to the ReorderPolicy.
	 */

	public static class Builder {

		/**
		 * This returns a builder with locationId.
		 */
		public static Builder newInstance(){
			return new Builder();
		}

		private ReorderPolicy policy = ReceiverUtils.createSSReorderPolicy(1000, 5000);
		private double onHand = 0.0;
		private ProductType productType;


		/**
		 * Set relevant receiver product types.
		 */
		public Builder setProductType(ProductType productType){
			this.productType = productType;
			return this;
		}

		/**
		 * Set relevant product type id.
		 */
		public Builder setReorderingPolicy(ReorderPolicy policy) {
			this.policy = policy;
			return this;
		}


		/**
		 * Set the current (opening) inventory for the product at the receiver. Defaults to 0 units on hand.
		 */
		public Builder setStockOnHand(double stockOnHand) {
			this.onHand = stockOnHand;
			return this;
		}


		public ReceiverProduct build(){
			return new ReceiverProduct(this);
		}


	}


}
