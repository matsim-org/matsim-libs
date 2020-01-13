/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.run;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModes {
	public static DvrpMode mode(String mode) {
		return new DvrpModeImpl(mode);
	}

	public static <T> Key<T> key(Class<T> type, String mode) {
		return Key.get(type, mode(mode));
	}

	public static void registerDvrpMode(Binder binder, String mode) {
		Multibinder.newSetBinder(binder, DvrpMode.class).addBinding().toInstance(DvrpModes.mode(mode));
	}
}
