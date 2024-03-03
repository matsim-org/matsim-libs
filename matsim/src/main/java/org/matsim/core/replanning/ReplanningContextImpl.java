/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ReplanningContextImpl.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.replanning;

import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class ReplanningContextImpl implements ReplanningContext, IterationStartsListener {

    private int iteration;

    @Inject
    ReplanningContextImpl(ControlerListenerManager controlerListenerManager) {
        controlerListenerManager.addControlerListener(this);
    }

    @Override
    public int getIteration() {
        return iteration;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        this.iteration = event.getIteration();
    }
}
