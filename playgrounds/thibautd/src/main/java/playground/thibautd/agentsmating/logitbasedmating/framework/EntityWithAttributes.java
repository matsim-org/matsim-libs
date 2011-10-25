/* *********************************************************************** *
 * project: org.matsim.*
 * EntityWithAttributes.java
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
package playground.thibautd.agentsmating.logitbasedmating.framework;

import java.util.Map;

/**
 * Represents any entity which is associated attributes, that is, decision
 * maker or alternative.
 *
 * @author thibautd
 */
public interface EntityWithAttributes {
	/**
	 * @return the double value of the given attribute, for direct use in a utility
	 * function
	 * @throws UnexistingAttributeException if the requested attribute is unknown
	 */
	public double getAttribute(String attribute) throws UnexistingAttributeException;

	/**
	 * @return a maping of attribute names with their values.
	 */
	public Map<String, Object> getAttributes();
}

