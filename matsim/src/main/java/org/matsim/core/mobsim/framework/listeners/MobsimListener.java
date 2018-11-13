/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationListener
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

import java.util.EventListener;

import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.mobsim.qsim.components.QSimComponent;

/**
 * A marker interface that all QueueSimulationListeners must extend.
 * <p></p>
 * Example(s):<ul>
 * <li> {@link tutorial.programming.example22MobsimListener.RunMobsimListenerExample}
 * </ul>
 *
 * @author dgrether
 */
public interface MobsimListener extends EventListener, MatsimExtensionPoint, QSimComponent {

}
