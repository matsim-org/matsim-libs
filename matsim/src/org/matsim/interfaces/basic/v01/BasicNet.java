/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNetI.java
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

package org.matsim.interfaces.basic.v01;

import java.util.Map;

import org.matsim.basic.v01.Id;

/**
 * A topological network representation.
 */
public interface BasicNet<N extends BasicNode, L extends BasicLink> {

    /**
     * Connects this network in the following sense: After a call to this
     * function, all contained nodes and links must know about all of their
     * adjacent network elements.
     */
    public void connect();

    /**
     * Returns a set of this network's nodes. This set might be empty, but it
     * must not be <code>null</code>.
     *
     * @return a set of this network's nodes
     */
		public Map<Id, N> getNodes();

    /**
     * Returns a set of this network's links. This set might be empty, but it
     * must not be <code>null</code>.
     *
     * @return a set of this network's links
     */
    public Map<Id, L> getLinks();

}