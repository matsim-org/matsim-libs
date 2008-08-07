/* *********************************************************************** *
 * project: org.matsim.*
 * SocializingOpportunityGeneratorI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.socialnetworks.interactions;

import java.util.Collection;

import org.matsim.population.Population;

public interface SocialActGeneratorI {

	// A SocializingOpportunityGenerator generates SocializingOpportunity based on
	// the persons and their plans (or knowledge, i.e. memories)
	
	Collection<SocialAct> generateValues( Population plans );
}
