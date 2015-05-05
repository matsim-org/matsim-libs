/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TransitRouterModule.java
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

package org.matsim.pt.router;

import org.matsim.core.controler.AbstractModule;

import javax.inject.Provider;

public class TransitRouterModule extends AbstractModule {

    @Override
    public void install() {
        if (getConfig().scenario().isUseTransit()) {
            bind(TransitRouter.class).toProvider(TransitRouterImplFactory.class);
        } else {
            bind(TransitRouter.class).toProvider(DummyTransitRouterFactory.class);
        }
    }

    static class DummyTransitRouterFactory implements Provider<TransitRouter> {
        @Override
        public TransitRouter get() {
            throw new RuntimeException("Transit not enabled.");
        }
    }

}
