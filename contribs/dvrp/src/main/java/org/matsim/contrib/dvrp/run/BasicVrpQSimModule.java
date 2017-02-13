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
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.*;


public class BasicVrpQSimModule
    extends AbstractModule
{
    private final String mode;
    private final Fleet fleet;
    private final Class<? extends VrpOptimizer> vrpOptimizerClass;
    private final Class<? extends PassengerRequestCreator> passengerRequestCreatorClass;
    private final Class<? extends DynActionCreator> dynActionCreatorClass;


    public BasicVrpQSimModule(String mode, Fleet fleet,
            Class<? extends VrpOptimizer> vrpOptimizerClass,
            Class<? extends PassengerRequestCreator> passengerRequestCreatorClass,
            Class<? extends DynActionCreator> dynActionCreatorClass)
    {
        this.mode = mode;
        this.fleet = fleet;
        this.vrpOptimizerClass = vrpOptimizerClass;
        this.passengerRequestCreatorClass = passengerRequestCreatorClass;
        this.dynActionCreatorClass = dynActionCreatorClass;
    }


    @Override
    public void install()
    {
        addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));
        bind(Fleet.class).toInstance(fleet);
    }


    @Provides
    private Collection<AbstractQSimPlugin> provideQSimPlugins(Config config)
    {
        final Collection<AbstractQSimPlugin> plugins = DynQSimModule.createQSimPlugins(config);
        plugins.add(new PassengerEnginePlugin(config, mode));
        plugins.add(new VrpAgentSourcePlugin(config));
        plugins.add(new VrpOptimizerPlugin(config));
        return plugins;
    }


    private class VrpOptimizerPlugin
        extends AbstractQSimPlugin
    {
        private VrpOptimizerPlugin(Config config)
        {
            super(config);
        }


        @Override
        public Collection<? extends Module> modules()
        {
            Collection<Module> result = new ArrayList<>();
            result.add(new com.google.inject.AbstractModule() {
                @Override
                protected void configure()
                {
                    bind(VrpOptimizer.class).to(vrpOptimizerClass).asEagerSingleton();
                    bind(PassengerRequestCreator.class).to(passengerRequestCreatorClass)
                            .asEagerSingleton();
                    bind(DynActionCreator.class).to(dynActionCreatorClass).asEagerSingleton();
                }
            });
            return result;
        }


        @SuppressWarnings("unchecked")
        @Override
        public Collection<Class<? extends MobsimListener>> listeners()
        {
            Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
            if (MobsimListener.class.isAssignableFrom(vrpOptimizerClass)) {
                result.add((Class<? extends MobsimListener>)vrpOptimizerClass);
            }
            return result;
        }
    }
}
