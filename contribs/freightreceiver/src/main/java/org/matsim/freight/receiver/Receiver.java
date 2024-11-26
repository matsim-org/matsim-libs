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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.Collection;
import java.util.List;

/**
 * A receiver.
 *
 * @author wlbean
 */

public interface Receiver extends HasPlansAndId<ReceiverPlan, Receiver>, Attributable {


	Id<Link> getLinkId();

	/**
	 * Set the link Id from which the receiver is accessed. This is an easy-option
	 * (similar to using a Builder) to assign multiple characteristics.
	 */
	public Receiver setLinkId(Id<Link> linkId);

	/**
	 * Gets a collection of receiver orders.
	 */
	@Override
	List<ReceiverPlan> getPlans();

	Receiver addProduct( ReceiverProduct product );

	/**
	 * Gets a collection of receiver products.
	 */
	Collection<ReceiverProduct> getProducts();

	/**
	 * Gets a specific product for the receiver.
	 *
	 * @param productType
	 * @return
	 */
	public abstract ReceiverProduct getProduct(Id<ProductType> productType);


	/**
	 * Sets the receiver collaboration status
	 * @return
	 */
//	public abstract Receiver setCollaborationStatus(boolean status);

	/**
	 * Gets the receiver collaboration status
	 */
//	public abstract boolean getCollaborationStatus();

	/**
	 * Sets the receiver's initial cost (in order to calculate the coalition cost allocations)
	 */
	public abstract void setInitialCost(double cost);

	/**
	 * Gets the receiver collaboration status
	 */
	public abstract double getInitialCost();


}

