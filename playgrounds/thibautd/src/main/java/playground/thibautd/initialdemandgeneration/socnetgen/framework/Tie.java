/* *********************************************************************** *
 * project: org.matsim.*
 * Tie.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
public class Tie {
	private final Id firstId;
	private final Id secondId;

	public Tie(final Id firstId, final Id secondId) {
		this.firstId = firstId;
		this.secondId = secondId;
	}

	public Id getFirstId() {
		return firstId;
	}

	public Id getSecondId() {
		return secondId;
	}
}

