/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.run;

import java.util.*;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.pt.config.TransitConfigGroup;

import com.google.inject.Provides;


public class DynQSimModule<T extends Mobsim>
    extends AbstractModule
{
    private Class<? extends javax.inject.Provider<? extends T>> providerClass;


    public DynQSimModule(Class<? extends javax.inject.Provider<? extends T>> providerClass)
    {
        this.providerClass = providerClass;
    }


    @Override
    public void install()
    {
        bind(Mobsim.class).toProvider(providerClass);
    }


    @Provides
    Collection<AbstractQSimPlugin> provideQSimPlugins(TransitConfigGroup transitConfigGroup,
            NetworkConfigGroup networkConfigGroup, Config config)
    {
        final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
        plugins.add(new MessageQueuePlugin(config));
        plugins.add(new DynActivityEnginePlugin(config));
        plugins.add(new QNetsimEnginePlugin(config));
        if (networkConfigGroup.isTimeVariantNetwork()) {
            plugins.add(new NetworkChangeEventsPlugin(config));
        }
        if (transitConfigGroup.isUseTransit()) {
            plugins.add(new TransitEnginePlugin(config));
        }
        plugins.add(new TeleportationPlugin(config));
        plugins.add(new PopulationPlugin(config));
        return plugins;
    }
}
