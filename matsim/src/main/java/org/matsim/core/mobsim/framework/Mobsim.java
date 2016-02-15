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

import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * Example(s):<ul>
 * <li> {@link  tutorial.programming.ownMobsimAgentUsingRouter.RunOwnMobsimAgentUsingRouterExample}
 * <li> {@link tutorial.programming.ownMobsimAgentWithPerception.RunOwnMobsimAgentWithPerceptionExample}
 *</ul>
 */
public interface Mobsim extends MatsimExtensionPoint {

  /**
   * Start the mobility simulation
   */
  public void run();

}