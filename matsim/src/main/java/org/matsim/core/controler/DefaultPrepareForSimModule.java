/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import com.google.inject.multibindings.Multibinder;

/**
 * Created by amit on 10.07.17.
 */

public class DefaultPrepareForSimModule extends AbstractModule {
    // probably, rename it.
    @Override
    public void install() {
        bind(PrepareForSim.class).to(PrepareForSimImpl.class);
		Multibinder.newSetBinder(binder(), PersonPrepareForSimAlgorithm.class);
    }
}
