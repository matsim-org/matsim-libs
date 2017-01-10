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

package org.matsim.contrib.signals.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.router.InvertedNetworkRoutingGuiceModule.InvertedNetworkRoutingModuleProvider;


public class InvertedNetworkWithSignalsRoutingGuiceModule
    extends AbstractModule
{
    @Override
    public void install()
    {
        bind(NetworkTurnInfoBuilder.class).to(NetworkWithSignalsTurnInfoBuilder.class);
        addRoutingModuleBinding(TransportMode.car)
                .toProvider(new InvertedNetworkRoutingModuleProvider(TransportMode.car));
    }
}
