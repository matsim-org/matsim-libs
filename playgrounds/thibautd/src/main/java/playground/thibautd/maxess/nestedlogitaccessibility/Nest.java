/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.maxess.nestedlogitaccessibility;

import org.matsim.api.core.v01.Id;

import java.util.List;

/**
 * @author thibautd
 */
public class Nest {
	private final Id<Nest> name;
	private final double mu_n;
	// if need exists, could easily be made generic (with alternatives type as a class parameter)
	private final List<Alternative> alternatives;

	public Nest( Id<Nest> name, double mu_n, List<Alternative> alternatives ) {
		this.name = name;
		this.mu_n = mu_n;
		this.alternatives = alternatives;
	}

	public Id<Nest> getNestId() {
		return name;
	}

	public List<Alternative> getAlternatives() {
		return alternatives;
	}

	public double getMu_n() {
		return mu_n;
	}
}
