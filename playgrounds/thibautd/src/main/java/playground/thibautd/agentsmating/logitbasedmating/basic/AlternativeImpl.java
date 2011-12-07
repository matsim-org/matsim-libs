/* *********************************************************************** *
 * project: org.matsim.*
 * AlternativeImpl.java
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

import java.util.Map;

import playground.thibautd.agentsmating.logitbasedmating.framework.Alternative;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * Default implementation of an alternative
 * @author thibautd
 */
public class AlternativeImpl implements Alternative {
	private final AttributesMap attributesMap = new AttributesMap();
	private final String mode;

	public AlternativeImpl(final String mode, final Map<String, ? extends Object> attributes) {
		this.mode = mode;
		attributesMap.setAttributes(attributes);
	}

	@Override
	public double getAttribute(final String attribute)
			throws UnexistingAttributeException {
		return attributesMap.getAttribute(attribute);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributesMap.getAttributes();
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public boolean equals(
			final Object object) {
		if ( !(object instanceof Alternative) ) return false;

		Alternative other = (Alternative) object;

		return mode.equals( other.getMode() ) &&
			getAttributes().equals( other.getAttributes() );
	}

	@Override
	public int hashCode() {
		return mode.hashCode() + attributesMap.hashCode();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"{mode="+mode+", "+attributesMap+"}";
	}
}

