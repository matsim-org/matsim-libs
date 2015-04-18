/* *********************************************************************** *
 * project: org.matsim.*
 * Simulation
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

/**
 * Interface to make a simulation work together with
 * simulation events and a Control(l)er
 * <p/>
 * Comments:<ul>
 * <li> The Mobsim interface exists twice.  This one should be renamed e.g. "RunnableMobsim", but I don't want to combine this
 * with the set of changes that I am currently working on. 
 * </ul>
 *
 * @author dgrether
 */
public interface RunnableMobsim {

  /**
   * Start the mobility simulation
   */
  public void run();

}