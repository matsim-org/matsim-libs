/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

/**
 * {@link QSimComponent}s of type {@link ActivityHandler} are added into a specific registry in {@link QSimProvider#get()}.  This registry is later used in {@link org.matsim.core.mobsim.qsim.QSim#arrangeAgentActivity(MobsimAgent)} (not public, therefore cannot be referenced from javadoc).
 */
public interface ActivityHandler extends QSimComponent {

	boolean handleActivity(MobsimAgent agent);

	void rescheduleActivityEnd(MobsimAgent agent);

}
