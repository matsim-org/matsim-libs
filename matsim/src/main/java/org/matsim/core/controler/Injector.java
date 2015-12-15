/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Injector.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.core.controler;

import com.google.inject.*;

import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.GenericPlanSelector;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import javax.inject.Named;
import java.util.*;

public class Injector {

    private static Logger logger = Logger.getLogger(Injector.class);

    private com.google.inject.Injector injector;

    private Injector(com.google.inject.Injector injector) {
        this.injector = injector;
    }

    public static Injector createInjector(final Config config, AbstractModule... modules) {
        com.google.inject.Injector bootstrapInjector = Guice.createInjector(new Module() {
            @Override
            public void configure(Binder binder) {
                binder.requireExplicitBindings(); // For now, we are conservative and disable this kind of magic.
                binder.install(new ExplodedConfigModule(config));
            }
        });
        // A MATSim module needs the config at configuration time in order to decide what
        // features to provide. So we create a bootstrapInjector which already has the config
        // and provides it to the MATSim modules.
        List<com.google.inject.Module> guiceModules = new ArrayList<>();
        for (AbstractModule module : modules) {
            bootstrapInjector.injectMembers(module);
            guiceModules.add(module);
        }
        com.google.inject.Injector realInjector = bootstrapInjector.createChildInjector(insertMapBindings(guiceModules));
        for (Map.Entry<Key<?>, Binding<?>> entry : realInjector.getBindings().entrySet()) {
      	  Level level = Level.INFO ;
      	  if ( entry.getKey().toString().contains("type=org.matsim") ) {
      		  logger.log( level, entry.getKey().getTypeLiteral() );
      		  logger.log(level, "   -> " + entry.getValue().getProvider());
      	  }
        }
        return fromGuiceInjector(realInjector);
    }

    private static Module insertMapBindings(List<Module> guiceModules) {
        com.google.inject.AbstractModule routingModuleBindings = createMapBindingsForType(guiceModules, RoutingModule.class);
        com.google.inject.AbstractModule travelTimeBindings = createMapBindingsForType(guiceModules, TravelTime.class);
        com.google.inject.AbstractModule travelDisutilityFactoryBindings = createMapBindingsForType(guiceModules, TravelDisutilityFactory.class);
        return Modules.combine(Modules.combine(guiceModules), routingModuleBindings, travelTimeBindings, travelDisutilityFactoryBindings);
    }

    private static <T> com.google.inject.AbstractModule createMapBindingsForType(List<Module> guiceModules, final Class<T> aClass) {
        final Set<String> modes = new HashSet<>();
        for (Element element : Elements.getElements(guiceModules)) {
            element.acceptVisitor(new DefaultElementVisitor<Object>() {
                @Override
                public <T> Object visit(Binding<T> binding) {
                    if (binding.getKey().getTypeLiteral().getRawType().equals(aClass)) {
                        if (binding.getKey().getAnnotation() instanceof com.google.inject.name.Named) {
                            modes.add(((com.google.inject.name.Named) binding.getKey().getAnnotation()).value());
                        }
                    }
                    return null;
                }
            });
        }
        return new com.google.inject.AbstractModule() {
            @Override
            protected void configure() {
                MapBinder<String, T> routingModuleMultibinder = MapBinder.newMapBinder(binder(), String.class, aClass);
                for (String mode : modes) {
                    routingModuleMultibinder.addBinding(mode).to(Key.get(aClass, Names.named(mode)));
                }
            }
        };
    }

    public static Injector fromGuiceInjector(com.google.inject.Injector injector) {
        return new Injector(injector);
    }

    /**
     *
     * Returns an instance of a specified infrastructure class or interface.
     * This so-called binding needs to have been explicitly declared in a Module at startup time.
     * If the binding is unknown, an exception will be thrown.
     *
     */
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    /**
     *
     * Returns an instance of the specified class and injects it with infrastructure.
     * The class needs to have either a constructor without arguments, or exactly one constructor
     * annotated with @Inject whose parameter types are all known to this injector, i.e. it would
     * return an instance if getInstance was called with this type.
     * Member variables annotated with @Inject are also injected.
     *
     */
    public <T> T getJITInstance(final Class<T> type) {
        return injector.createChildInjector(new com.google.inject.AbstractModule() {
            @Override
            protected void configure() {
                bind(type);
            }
        }).getInstance(type);
    }

    Set<EventHandler> getEventHandlersDeclaredByModules() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<EventHandler>>() {
                }
        ));
    }

    Set<ControlerListener> getControlerListenersDeclaredByModules() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<ControlerListener>>() {
                }
        ));
    }

    public Map<String, PlanStrategy> getPlanStrategies() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Map<String, PlanStrategy>>() {
                }
        ));
    }

    public Map<String, GenericPlanSelector<Plan, Person>> getPlanSelectorsForRemoval() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Map<String, GenericPlanSelector<Plan, Person>>>() {
                }
        ));
    }

    public Set<MobsimListener> getMobsimListeners() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<MobsimListener>>() {
                }
        ));
    }

    public Set<SnapshotWriter> getSnapshotWriters() {
        return injector.getInstance(Key.get(
                new TypeLiteral<Set<SnapshotWriter>>() {
                }
        ));
    }

    public <T> Provider<T> getProvider(Class<T> type) {
        return injector.getProvider(type);
    }
}
