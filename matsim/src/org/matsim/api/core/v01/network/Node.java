/* *********************************************************************** *
 * project: org.matsim.*
 * Node.java
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

package org.matsim.api.core.v01.network;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicNode;

/**
 * This interface deliberately does NOT have a back pointer ...
 * ... since, at this level, one should be able to get the relevant container from
 * the context.
 * (This becomes clear if you think about a nodeId/linkId given by person.)
 */
public interface Node extends BasicNode
//, Comparable<Node> 
{

    /**
     * Returns this node's set of ingoing links. This set might be empty, but it
     * should not be <code>null</code>.
     *
     * @return this node's ingoing links
     */
    public Map<Id, ? extends Link> getInLinks();

    /**
     * Returns this node's set of outgoing links. This set might be empty, but
     * it should not be <code>null</code>.
     *
     * @return this node's outgoing links
     */
    public Map<Id, ? extends Link> getOutLinks();

	
}
