/* *********************************************************************** *
 * project: org.matsim.*
 * AttributesMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.HashMap;
import java.util.Map;

import playground.thibautd.agentsmating.logitbasedmating.framework.EntityWithAttributes;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * Helper to use as delegate.
 * @author thibautd
 */
public class AttributesMap implements EntityWithAttributes {
	private final Map<String, Object> attributes = new HashMap<String, Object>();

	@Override
	public double getAttribute(final String attributeName)
			throws UnexistingAttributeException {
		Object attribute = attributes.get(attributeName);

		if (attribute == null) throw new UnexistingAttributeException();

		double value = Double.NaN;

		if (attribute instanceof Number) {
			value = ((Number) attribute).doubleValue();
		}
		else if (attribute instanceof Boolean) {
			boolean isTrue = ((Boolean) attribute).booleanValue();
			value = (isTrue ? 1 : 0);
		}
		else {
			throw new RuntimeException("Unhandled attribute type found");
		}

		return value;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttribute(final String name, final Object value) {
		this.attributes.put(name, value);
	}

	public void setAttributes(final Map<String, ? extends Object> attributes) {
		this.attributes.putAll(attributes);
	}

	@Override
	public boolean equals(final Object object) {
		if ( !(object instanceof AttributesMap) ) return false;

		return attributes == ( (AttributesMap) object ).attributes;
	}

	@Override
	public int hashCode() {
		return attributes.hashCode();
	}

	@Override
	public String toString() {
		return "attributes={"+attributes+"}";
	}
}

