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

package org.matsim.contrib.dvrp.run;

import java.util.*;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerEnginePlugin;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.*;


public class VrpQSimModule
    extends AbstractModule
{
    private final String mode;
    private final Fleet fleet;
    private final Module module;
    private final Class<? extends MobsimListener>[] listeners;


    @SafeVarargs
    public VrpQSimModule(String mode, Module module, Class<? extends MobsimListener>... listeners)
    {
        this(mode, null, module, listeners);
    }


    @SafeVarargs
    public VrpQSimModule(String mode, Fleet fleet, Module module,
            Class<? extends MobsimListener>... listeners)
    {
        this.mode = mode;
        this.fleet = fleet;
        this.module = module;
        this.listeners = listeners;
    }


    @Override
    public void install()
    {
        addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));
        if (fleet != null) {
            bind(Fleet.class).toInstance(fleet);
        }
    }


    @Provides
    private Collection<AbstractQSimPlugin> provideQSimPlugins(Config config)
    {
        final Collection<AbstractQSimPlugin> plugins = DynQSimModule.createQSimPlugins(config);
        plugins.add(new PassengerEnginePlugin(config, mode));
        plugins.add(new VrpAgentSourcePlugin(config));
        plugins.add(new SimplifiedQSimPlugin(config));
        return plugins;
    }


    private class SimplifiedQSimPlugin
        extends AbstractQSimPlugin
    {
        public SimplifiedQSimPlugin(Config config)
        {
            super(config);
        }


        @Override
        public Collection<? extends Module> modules()
        {
            Collection<Module> result = new ArrayList<>();
            result.add(module);
            return result;
        }


        @Override
        public Collection<Class<? extends MobsimListener>> listeners()
        {
            Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
            for (Class<? extends MobsimListener> l : listeners) {
                result.add(l);
            }
            return result;
        }
    }
}
