/* *********************************************************************** *
 * project: org.matsim.*
 * SSReordering.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.Objects;

/**
 * Basic implementation of an (s,S) reordering policy. That is, as soon as the
 * current inventory level reaches a value below 's', the product is replenished
 * by a quantity that will get the inventory back up to 'S'.
 *
 * @author jwjoubert
 */
final class SSReorderPolicy implements ReorderPolicy {
	final String policyName = "(s,S)";
	final private String attributeMinimumLevel = "s";
	final private String attributeMaximumLevel = "S";
	final private Attributes attributes = new AttributesImpl();

	/**
	 * This class should be instantiated through {@link ReceiverUtils#createSSReorderPolicy(double, double)}.
	 */
	SSReorderPolicy(double minLevel, double maxLevel) {
		this.attributes.putAttribute(attributeMinimumLevel, minLevel);
		this.attributes.putAttribute(attributeMaximumLevel, maxLevel);
		// (these need to be attributes since they are written to file and read back in.  Yet, they are policy specific, so
		// there is no reason to make them truly typed. kai, jan'19
	}

	/**
	 * This method should (ideally) only be used by the {@link ReceiversReader}.
	 */
	SSReorderPolicy() {
	}

	/**
	 * This method assumes that the stock on hand, and the reordering policy's
	 * quantities are expressed in the same unit-of-measure.
	 */
	@Override
	public double calculateOrderQuantity(double onHand) {
		double s = getMinimumStockLevel();
		double S = getMaximumStockLevel();
		if(onHand <= s) {
			return S - onHand;
		} else {
			return 0;
		}
	}

	/**
	 * Get the lower limit 's' of the (s,S) stock ordering policy.
	 */
	public double getMinimumStockLevel() {
		return Double.parseDouble(Objects.requireNonNull(attributes.getAttribute(attributeMinimumLevel)).toString());
	}

	/**
	 * Get the upper limit 'S' of the (s,S) stock ordering policy.
	 */
	public double getMaximumStockLevel() {
		return Double.parseDouble(Objects.requireNonNull(attributes.getAttribute(attributeMaximumLevel)).toString());
	}

	@Override
	public String getPolicyName() {
		return policyName;
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

}
