/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLinkI.java
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

import org.matsim.utils.identifiers.IdI;

/**
 * A topological representation of a network link.
 */
public interface BasicLinkI {

  /**
   * Returns a non-<code>null</code> instance of <code>IdI</code> that
   * uniquely identifies this object.
   *
   * @return this object's identifier
   */
  public IdI getId();

    /**
     * Sets this link's non-<code>null</code> upstream node.
     *
     * @param node
     *            the <code>BasicNodeI</code> to be set
     *
     * @return <true> if <code>node</code> has been set and <code>false</code>
     *         otherwise
     *
     * @throws IllegalArgumentException
     *             if <code>node</code> is <code>null</code>
     */
    public boolean setFromNode(BasicNodeI node);

    /**
     * Sets this link's non-<code>null</code> downstream node.
     *
     * @param node
     *            the <code>BasicNodeI</code> to be set
     *
     * @return <code>true</code> if <code>node</code> has been set and
     *         <code>false</code> otherwise
     *
     * @throws IllegalArgumentException
     *             if <code>node</code> is <code>null</code>
     */
    public boolean setToNode(BasicNodeI node);

    /**
     * Returns this link's upstream node. Must not return <code>null</code>.
     *
     * @return this link's upstream node
     */
    public BasicNodeI getFromNode();

    /**
     * Returns this link's downstream node. Must not return <code>null</code>.
     *
     * @return this link's downstream node
     */
    public BasicNodeI getToNode();


    public double getLength();

    public int getLanes();

    public double getFreespeed(final double time);

    /**
  	 * This method returns the capacity as set in the xml defining the network. Be aware
  	 * that this capacity is not normalized in time, it depends on the period set
  	 * in the network file (the capperiod attribute).
  	 * @return the capacity per network's capperiod timestep
  	 */
    public double getCapacity();

    public void setFreespeed(double freespeed);

    public void setLength(double length);

  	public void setLanes(int lanes);

  	public void setCapacity(double capacity);


}