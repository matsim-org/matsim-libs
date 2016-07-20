/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package contrib.publicTransitMapping.mapping.pseudoRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import contrib.publicTransitMapping.mapping.PseudoRouting;

import java.util.Set;

/**
 * Container class for artificial links created
 * in {@link PseudoRouting}.
 *
 * @author polettif
 */
public interface ArtificialLink {

	Id<Node> getToNodeId();

	Id<Node> getFromNodeId();

	Coord getFromNodeCoord();

	Coord getToNodeCoord();

	double getFreespeed();

	double getLength();

	boolean equals(Object obj);

	double getCapacity();

	Set<String> getAllowedModes();
}
