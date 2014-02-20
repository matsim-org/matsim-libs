/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.thibautd.socnetsim.population;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;

public interface SocialNetwork {

	public static final String ELEMENT_NAME = "SocialNetwork";

	/**
	 * Must be called before adding ties.
	 * This allows to make sure that isolated agents (without social contacts)
	 * are identified as such in the network.
	 * This is necessary for analysing the topology of the network, but also
	 * for more safety in simulation (fail if the social network does not cover the
	 * whole population).
	 */
	public void addEgo(Id id);

	public void addEgos(Iterable<? extends Id> ids);

	public void addBidirectionalTie(Id id1, Id id2);

	public void addMonodirectionalTie(Id ego, Id alter);

	public Set<Id> getAlters(Id ego);

	public Set<Id> getEgos();

	public Map<Id, Set<Id>> getMapRepresentation();

	public boolean isReflective();

	/**
	 * retrieve the metadata
	 */
	public Map<String, String> getMetadata();

	/**
	 * add metadata. Metadata associates attribute names to values,
	 * and can be used to store any information useful to organize data:
	 * date of generation, source, author, etc.
	 */
	public void addMetadata(final String att, final String value);
}
