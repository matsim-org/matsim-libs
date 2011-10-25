/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionMakerFactory.java
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * @author thibautd
 */
public interface DecisionMakerFactory extends MatsimFactory {
	public DecisionMaker createDecisionMaker(Person agent);
}

