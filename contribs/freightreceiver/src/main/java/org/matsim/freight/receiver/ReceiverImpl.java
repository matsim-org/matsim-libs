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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This returns a receiver that has characteristics and orders.
 *
 * @author wlbean, jwjoubert
 */
class ReceiverImpl implements Receiver {
	final private Logger log = LogManager.getLogger(Receiver.class);

	private final Attributes attributes = new AttributesImpl();
	private Id<Link> location;

	private final Id<Receiver> id;
	private final List<ReceiverPlan> plans;
	private final List<ReceiverProduct> products;
	private ReceiverPlan selectedPlan;
	private double moneyBalance = 0.0;

	ReceiverImpl(final Id<Receiver> id){
		super();
		this.id = id;
		this.plans = new ArrayList<>();
		this.products = new ArrayList<>();
	}


	@Override
	public boolean addPlan(ReceiverPlan plan) {
		if(!plans.contains(plan)) {
			plans.add(plan);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ReceiverPlan createCopyOfSelectedPlanAndMakeSelected() {
		ReceiverPlan plan = selectedPlan.createCopy();

		this.setSelectedPlan(plan);
		// (implicitly adds if not yet added)

		return plan;
	}


	/*
	 * Removes an order from the receiver's list of orders.
	 */

	@Override
	public boolean removePlan(ReceiverPlan plan) {
		return this.plans.remove(plan);
	}

	/*
	 * Returns the receiver id.
	 */

	@Override
	public Id<Receiver> getId() {
		return id;
	}

	/*
	 * Returns a list of the receiver's orders.
	 */

	@Override
	public List<ReceiverPlan> getPlans() {
		return plans;
	}
	@Override
	public Receiver addProduct( ReceiverProduct product ) {
		if ( products.contains( product ) ) {
			throw new IllegalArgumentException( "receiver already has product " + product + "; not adding it a second time." ) ;
		}
		products.add( product ) ;
		return this ;
	}

	/*
	 * Returns a list of the receiver's products.
	 */
	@Override
	public List<ReceiverProduct> getProducts() {
		return Collections.unmodifiableList( products ) ;
	}

	@Override
	public ReceiverPlan getSelectedPlan() {
		return this.selectedPlan;
	}


	/**
	 * Sets the selected receiver plan.
	 */
	@Override
	public void setSelectedPlan(ReceiverPlan selectedPlan) {
		/* Unselected all other plans. */
		for(ReceiverPlan plan : this.plans) {
			plan.setSelected(false);
		}

		selectedPlan.setSelected(true);
		if(!plans.contains(selectedPlan)) plans.add(selectedPlan);
		this.selectedPlan = selectedPlan;
	}


	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}


	/**
	 * Returns the link from which the receiver is accessed.
	 * TODO One may consider changing this so that it is a list of links.
	 */
	@Override
	public Id<Link> getLinkId() {
		return this.location;
	}


	@Override
	public Receiver setLinkId(Id<Link> linkId) {
		this.location = linkId;
		return this;
	}

	@Override
	public ReceiverProduct getProduct(Id<ProductType> productType) {
		ReceiverProduct product = null;
		Iterator<ReceiverProduct> iterator = this.products.iterator();
		while(product == null & iterator.hasNext()) {
			ReceiverProduct thisProduct = iterator.next();
			if(thisProduct.getProductType().getId().equals(productType)) {
				product = thisProduct;
			}
		}
		if(product == null) {
			log.warn("Receiver \"" + this.id.toString()
			+ "\" does not have the requested product type \"" + productType.toString() + "\". Returning null.");
		}
		return product;
	}

	@Override
	public void setInitialCost(double cost) {
		this.moneyBalance = cost;
	}

	@Override
	public double getInitialCost() {
		return this.moneyBalance;
	}

}
