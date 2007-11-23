/* *********************************************************************** *
 * project: org.matsim.*
 * BasicNodeI.java
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

package org.matsim.interfaces.networks.basicNet;

import org.matsim.utils.identifiers.IdentifiedI;

/**
 * A topological representation of an network node.
 */
public interface BasicNodeI extends IdentifiedI {

    /**
     * Adds a non-<code>null</code> link to this node's set of ingoing links.
     * 
     * @param link
     *            the <code>BasicLinkI</code> to be added
     * 
     * @return <code>true</code>> if <code>link</code> has been added and
     *         <code>false</code> otherwise
     * 
     * @throws IllegalArgumentException
     *             if <code>link</code> is <code>null</code>
     */
    public boolean addInLink(BasicLinkI link);

    /**
     * Adds a non-<code>null</code> link to this node's set of outgoing
     * links.
     * 
     * @param link
     *            the <code>BasicLinkI</code> to be added
     * 
     * @return <code>true</code> if <code>link</code> has been added and
     *         <code>false</code> otherwise
     * 
     * @throws IllegalArgumentException
     *             if <code>link</code> is <code>null</code>
     */
    public boolean addOutLink(BasicLinkI link);

    /**
     * Returns this node's set of ingoing links. This set might be empty, but it
     * must not be <code>null</code>.
     * 
     * @return this node's ingoing links
     */
    public BasicLinkSetI getInLinks();

    /**
     * Returns this node's set of outgoing links. This set might be empty, but
     * it must not be <code>null</code>.
     * 
     * @return this node's outgoing links
     */
    public BasicLinkSetI getOutLinks();

}