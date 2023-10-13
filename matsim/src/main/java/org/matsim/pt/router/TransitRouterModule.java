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

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.core.controler.AbstractModule;

public class TransitRouterModule extends AbstractModule {

    @Override
    public void install() {
        if (getConfig().transit().isUseTransit()) {
            switch (getConfig().transit().getRoutingAlgorithmType()) {
                case DijkstraBased:
                    throw new RuntimeException("'DijkstraBased' is no longer supported as a transit routing algorithm. Use 'SwissRailRaptor' instead.");
                case SwissRailRaptor:
                    install(new SwissRailRaptorModule());
                    break;
                default:
                    throw new RuntimeException("Unsupported transit routing algorithm type: " + getConfig().transit().getRoutingAlgorithmType().name());
            }
        }
    }

}
