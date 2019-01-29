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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.spi.LinkedKeyBinding;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.internal.BindingImpl;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;

public final class Injector {
	private Injector(){} // namespace only, do not instantiate

	private static Logger logger = Logger.getLogger(Injector.class);

	public static com.google.inject.Injector createInjector(final Config config, Module... modules) {
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
		for (Module module : modules) {
			bootstrapInjector.injectMembers(module);
			guiceModules.add(module);
		}
		com.google.inject.Injector realInjector = bootstrapInjector.createChildInjector(insertMapBindings(guiceModules));
		printInjector(realInjector, logger);
		return realInjector;
	}

	public static void printInjector(com.google.inject.Injector injector, Logger log) {
		Level level = Level.INFO ;
		log.log(level,"=== printInjector start ===") ;
		for (Map.Entry<Key<?>, Binding<?>> entry : injector.getBindings().entrySet()) {
			if ( entry.getKey().toString().contains("type=org.matsim") ) {
				Annotation annotation = entry.getKey().getAnnotation();
				log.log( level, entry.getKey().getTypeLiteral() + " " + (annotation != null ? annotation.toString() : ""));
				log.log(level, "  --> provider: " + entry.getValue().getProvider());
				log.log(level, "  --> source: " + entry.getValue().getSource() );
				if ( entry.getValue() instanceof BindingImpl ) {
					log.log( level, "  --> scope: " + ((BindingImpl<?>)entry.getValue()).getScoping() ) ;
				}
				if ( entry.getValue() instanceof LinkedKeyBinding) {
					log.log( level, "  --> target: " + ((LinkedKeyBinding) entry.getValue()).getLinkedKey() ) ;
				}
				log.log(level, "  ==full==> " + entry.getValue() );
				// yy could probably format the above in a better way. kai, may'16
				log.log(level,  "" );
			}
		}
		log.log(level,"=== printInjector end ===") ;
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

}
