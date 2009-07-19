/* *********************************************************************** *
 * project: org.matsim.*																															*
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

package org.matsim.api.basic.v01.network;

import java.io.Serializable;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.ToplevelContainer;


/**
 * A topological network representation.
 */
public interface BasicNetwork<N extends BasicNode, L extends BasicLink> extends ToplevelContainer,Serializable{
		/**
		 * Returns the builder for network elements
		 * @return
		 */
		public BasicNetworkBuilder getBuilder();
	
    /**
     * Returns a set of this network's nodes. This set might be empty, but it
     * should not be <code>null</code>.
     *
     * @return a set of this network's nodes
     */
    public Map<Id, ? extends N> getNodes();

    /**
     * Returns a set of this network's links. This set might be empty, but it
     * should not be <code>null</code>.
     *
     * @return a set of this network's links
     */
    public Map<Id, ? extends L> getLinks();
    
    /**
     * Returns the time period over which
     * the capacity of the given links has been measured. 
     * The default is set to one hour, i.e. 3600.0 sec.
     * @return the time period in seconds, default 3600.0 sec.
     */
    public double getCapacityPeriod();
    
    
    /**
     * Returns the lane width of the network's links.
     * @return the lane width in meter
     */
    public double getEffectiveLaneWidth();
    

    
    
}