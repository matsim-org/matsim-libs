/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.shipment;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LogisticChainElement;

/**
 * A single entry in a shipment's plan history ({@link LspShipmentPlan}). Each element records one
 * activity applied to the shipment while it travels through the logistics network - a load, an
 * unload, a transport leg, or an in-hub handling operation. The four concrete variants are produced
 * by the builders in {@link LspShipmentUtils}.
 */
public interface LspShipmentPlanElement {

	/** @return the logistics-chain element (resource) this plan element was executed at. */
	LogisticChainElement getLogisticChainElement();

	/** @return the id of the resource the activity was carried out at. */
	Id<LSPResource> getResourceId();

	/**
	 * Kind of a {@link LspShipmentPlanElement}. Its string representation ({@link #toString()}) is used in the
	 * plans file and in the ids of the plan elements, see {@link LspShipmentUtils#createPlanElementId}.
	 */
	enum ElementType {LOAD, TRANSPORT, UNLOAD, HANDLING}

	/**
	 * @return the kind of this plan element.
	 * <p>
	 * Use this for describing the element, e.g. when writing it to a file or when creating ids. Do not use it for
	 * specifying the type within a decision logic; prefer the class hierarchy instead (Bloch, Effective Java, Item 23:
	 * Prefer class hierarchies to tagged classes). kmt/kn jan'25
	 */
	ElementType getElementType();

	/** @return the simulation time at which the activity started. */
	double getStartTime();

	/** @return the simulation time at which the activity ended. */
	double getEndTime();

	/** @return the link the activity was carried out at (the destination link for a transport). */
	Id<Link> getLinkId();
}
