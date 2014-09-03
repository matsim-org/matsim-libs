/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
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

package playground.balmermi.modules;

import org.matsim.api.core.v01.population.Person;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonStupidDeleteKnowledgeForStreamingModule extends AbstractPersonAlgorithm {

	private final Object knowledges;

	public PersonStupidDeleteKnowledgeForStreamingModule(Object knowledges) {
		super();
		this.knowledges = knowledges;
	}

	@Override
	public void run(final Person person) {
        throw new RuntimeException("Knowledges are no more.");

    }
}
