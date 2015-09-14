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
import org.matsim.vis.snapshotwriters.SnapshotWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                binder.requireExplicitBindings(); // For now, we are conservative
                binder.disableCircularProxies(); // and disable any kind of magic.
                binder.bind(Config.class).toInstance(config);
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
        com.google.inject.Injector realInjector = bootstrapInjector.createChildInjector(guiceModules);
        System.out.flush() ; System.err.flush(); 
        for (Map.Entry<Key<?>, Binding<?>> entry : realInjector.getBindings().entrySet()) {
      	  Level level = Level.INFO ;
//            logger.debug(String.format("%s\n-> %s", entry.getKey(), entry.getValue()));
      	  if ( entry.getKey().toString().contains("type=org.matsim") ) {
//      		  logger.log( Level.WARN, entry.getKey() ); System.err.flush();
//      		  logger.log( Level.WARN, entry.getKey().getAnnotation() ); System.err.flush();
//      		  logger.log( Level.WARN, entry.getKey().getAnnotationType() ); System.err.flush();
      		  logger.log( level, entry.getKey().getTypeLiteral() ); System.err.flush();
//      		  logger.log( Level.WARN, entry.getKey().withoutAttributes() ); System.err.flush();
      		  logger.log( level, "   -> " + entry.getValue().getProvider() ) ; System.out.flush(); 
      		  
//      		  logger.log( level, "   -> annotation type: " + entry.getValue().getKey().getAnnotationType() ) ;
//      		  logger.log( level, "   -> annotation: " + entry.getValue().getKey().getAnnotation() ) ;

//      		  try {
//      			  logger.log( level, "   -> " + entry.getValue().getProvider().get().getClass() ) ; System.out.flush();
//      		  } catch ( Exception ee ) {
//      			  logger.log( level, "  -> not provided (only a problem if this is truly needed later)" ) ;
//      		  }
      		  // the above _instantiates_ the class, which is not what we want ( we just want the name ). 
      		  
      	  }
        }
        return fromGuiceInjector(realInjector);
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
