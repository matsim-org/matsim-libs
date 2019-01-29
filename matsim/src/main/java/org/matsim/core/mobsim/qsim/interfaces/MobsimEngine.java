/* *********************************************************************** *
 * project: org.matsim.*
 * SimEngine
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
package org.matsim.core.mobsim.qsim.interfaces;

import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

/**
 * Design thoughs:<ul>
 * <li> This is an engine that is plugged into the Mobsim.  Thus the name. 
 * <li> The main difference between a MobsimEngine and, say, a {@link MobsimBeforeSimStepListener}, is that the MobsimEngine obtains
 * the {@link InternalInterface}, whose main functionality is arrangeNextAgentState, i.e. it allows to move the agents forward.
 * </ul>
 * 
 * @author dgrether, nagel
 */
public interface MobsimEngine extends Steppable, QSimComponent {

  /**
   * called in a predefined Order when the simulation is started
   */
  void onPrepareSim();
 
  /**
   * Do some clean up.
   */
  void afterSim();
  
  void setInternalInterface(InternalInterface internalInterface);

}
