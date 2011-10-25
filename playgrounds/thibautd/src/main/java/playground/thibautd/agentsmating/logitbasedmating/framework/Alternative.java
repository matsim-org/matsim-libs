/* *********************************************************************** *
 * project: org.matsim.*
 * Alternative.java
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

/**
 * Represents a mode alternative in a choice model.
 * Implementations of this interface MUST provide pertinent implementations
 * of equals and hashCode
 *
 * @author thibautd
 */
public interface Alternative extends EntityWithAttributes {
	/**
	 * @return the name of the alternative
	 */
	public String getMode();
}

