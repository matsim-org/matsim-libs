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

	// yyyy "type" feels like this makes it a tagged class.  These should be avoided (Effective Java
	// 2018, Item 23).  It is, however, probably not
	// used as a type, but rather as a description.  Rename?
	/**
	 * @deprecated //see bloch item 23: Prefer class hierarchies to tagged classes.
	 * Mixing class tagging and class hierarchies is a bad idea.
	 * Getting the type is ok for writing it somewhere, but do NOT use it for specifying the type within a decision logic!
	 * So maybe better use something like getActivityType() | getElementDescription -- analogous to e.g. PersonEntersVehicleEvent.class. kmt/kn jan'25
	 */
	@Deprecated
	String getElementType();

	/** @return the simulation time at which the activity started. */
	double getStartTime();

	/** @return the simulation time at which the activity ended. */
	double getEndTime();

	/** @return the link the activity was carried out at (the destination link for a transport). */
	Id<Link> getLinkId();
}
