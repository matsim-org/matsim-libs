/* *********************************************************************** *
 * project: org.matsim.*
 * DrawableAgentI.java
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

package org.matsim.utils.vis.netvis;


public interface DrawableAgentI {

    /**
     * Returns this agent's position in its current link measured in meters from
     * the link's upstream end.
     * 
     * @return this agent's position in its current link
     */
    public double getPosInLink_m();

    /**
     * Returns this agent's lane in its current link. The rightmost lane is
     * numbered as 1, the leftmost lane is numbered with the link's total lane
     * count.
     * 
     * @return this agent's lane in its current link
     */
    public int getLane();

}
