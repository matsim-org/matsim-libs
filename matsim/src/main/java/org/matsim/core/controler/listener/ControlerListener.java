/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerListener.java
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

package org.matsim.core.controler.listener;

import java.util.EventListener;

import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.controler.Controler;

/**
 * ControlerListeners are notified at specific points in the {@link Controler} loop.  See sub-interfaces for more information
 * and specific usages.
 * <p>
 * Example(s):<ul>
 * <li> {@link tutorial.programming.example07ControlerListener.RunControlerListenerExample}
 * </ul>
 *
 * @author dgrether
 */
public interface ControlerListener extends EventListener, MatsimExtensionPoint {

	/**
	 * Return the priority of this listener. Listeners with higher priority are executed first. The default priority is 0.
	 */
	default double priority() {
		return 0;
	}

}
