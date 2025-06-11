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
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

/**
 * This implements the product types used among all {@link Receivers}.
 * <p>
 * TODO (JWJ, WLB, April 2018: think about how we can/should convert (seemlessly)
 * between volume and weight. Consider the IATA Dimensional Weight Factor. Think
 * about expressing a factor "percentage of cube(meter)-ton". A cubic meter of
 * toilet paper will have a factor of 1.0, and at the same time a brick-size
 * weighing a ton will also have a factor of 1.0. This is necessary as jsprit
 * can only work with ONE unit throughout its optimisation, i.e. and associate
 * it with vehicle capacity.
 *
 * @author wlbean
 */
public class ProductType implements Identifiable<ProductType>, Attributable {

	private final Attributes attributes = new AttributesImpl();

	/**
	 * Set default values.
	 */
	private String descr = "";
	private double reqCapacity = 1;
	private final Id<ProductType> typeId;
	private final Id<Link> originLinkId;

	/**
	 * The constructor should not be visible from outside the package. Use
	 * {@link ReceiverUtils#createAndGetProductType(Receivers, Id, Id)} to
	 * instantiate.
	 *
	 * @param typeId the type {@link Id};
	 * @param originLinkId the {@link Id<Link>} from which this product type
	 *                     is used. Since each product type represents a unique
	 *                     stock keeping unit (SKU), it is assumed the same
	 *                     product sourced from two different locations should
	 *                     be discernible.
	 */
	ProductType(final Id<ProductType> typeId, Id<Link> originLinkId){
		this.typeId = typeId;
		this.originLinkId = originLinkId;
	}

	public void setDescription(String description){
		this.descr = description;
	}

	public void setRequiredCapacity(double reqCapacity){
		this.reqCapacity = reqCapacity;
	}

	public String getDescription(){
		return descr;
	}

	public double getRequiredCapacity(){
		return reqCapacity;
	}

	public Id<ProductType> getId(){
		return typeId;
	}

	public Attributes getAttributes() {
		return this.attributes;
	}

	public Id<Link> getOriginLinkId(){
		return this.originLinkId;
	}
}
