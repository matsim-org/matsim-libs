/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.framework;


public interface AgentSource {

    /**
     * If you add an AgentSource into the QSim, this will be called during the initialization phase.
     * The AgentSource, however, is not just a source, but needs to insert the agents into the mobsim itself. 
     * This seems to work in the following cases:
     * <ul>
     * <li> Agents are inserted into activities.
     * <li> Agents are kept "in limbo", as one can do with the AdapterAgent.
     * <ul>
     * In contrast, it does NOT seem to be possible to insert agents into car legs.  The reason is that the need a vehicle,
     * which at this point is not yet there.  And there is no non-invasive way to insert that vehicle. (This may be changed,
     * I don't know.)
     */
    public void insertAgentsIntoMobsim();

}
