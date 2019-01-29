/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationBeforeCleanupListener
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

package org.matsim.core.mobsim.framework.listeners;

import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;

/**
 * This interface can be implemented when a notification is needed after the
 * QueueSimulation has performed a simulation step.
 *
 * @author mrieser
 */
public interface MobsimAfterSimStepListener extends MobsimListener {

	public void notifyMobsimAfterSimStep(final MobsimAfterSimStepEvent e);

}
