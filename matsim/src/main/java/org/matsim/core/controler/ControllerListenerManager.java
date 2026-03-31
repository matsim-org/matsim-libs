/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerListenerManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.controler;

import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.controler.listener.ControllerListener;

public interface ControllerListenerManager extends MatsimManager {

	void addControllerListener(ControllerListener controllerListener);

	@Deprecated(since = "2025-07-19")
	default void addControlerListener(ControllerListener controllerListener) {
		addControllerListener(controllerListener);
	};

}
