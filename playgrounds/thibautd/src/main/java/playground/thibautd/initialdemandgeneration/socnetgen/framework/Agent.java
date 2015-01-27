/* *********************************************************************** *
 * project: org.matsim.*
 * Agent.java
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

import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;

/**
 * Represents an agent with id and (socio-demographic) caracteristics.
 * There is volontarily not default characteristics (thus, this is mainly a
 * marker interface)
 * @author thibautd
 */
public interface Agent extends Identifiable<Person> {
}

