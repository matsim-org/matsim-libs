/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.nestedlogitaccessibility.framework;

import com.google.inject.*;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.util.Modules;
import com.google.inject.util.Types;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class InjectionUtils {
	private InjectionUtils() {
	}

	public static AbstractModule override(
			final Module base,
			final Module override ) {
		return override(
				Collections.singleton( base ),
				Collections.singleton( override ) );
	}


	public static AbstractModule override(
			final Iterable<? extends Module> base,
			final Module override ) {
		return override(
				base,
				Collections.singleton( override ) );
	}

	public static AbstractModule override(
			final Module base,
			final Iterable<? extends Module> override ) {
		return override(
				Collections.singleton( base ),
				override );
	}

	public static AbstractModule override(
			final Iterable<? extends Module> base,
			final Iterable<? extends Module> override ) {
		return override(
				new AbstractModule() {
					@Override
					public void install() {
						for ( Module m : base ) install( m );
					}
				},
				new AbstractModule() {
					@Override
					public void install() {
						for ( Module m : override ) install( m );
					}
				} );
	}

	public static AbstractModule override( AbstractModule base , AbstractModule override ) {
		return AbstractModule.override( Collections.singleton( base ) , override );
	}

	// As far as I can see, this is safe, even though unchecked: I do not see a way to create something else than the
	// return type.
	@SuppressWarnings("unchecked")
	public static <N extends Enum<N>> NestedLogitAccessibilityCalculator<N> createCalculator(
			final Config config,
			final TypeLiteral<N> nestType,
			final Module... modules ) {
		// Otherwise MATSim AbstractModules cannot be used...
		// And the MATSim Injector limits the injector methods one has access to
		final Injector bootstrapInjector = Guice.createInjector(
				new Module() {
					@Override
					public void configure( Binder binder ) {
						binder.bind( Config.class ).toInstance( config );
					}
				} );

		for ( Module m : modules ) bootstrapInjector.injectMembers( m );

		Injector injector = bootstrapInjector.createChildInjector( insertMapBindings( modules ) );

		final ParameterizedType newType =
				Types.newParameterizedType(
						NestedLogitAccessibilityCalculator.class,
						nestType.getType() );

		return (NestedLogitAccessibilityCalculator<N>) injector.getInstance( Key.get( newType ) );
	}

	/**
	 * copy from matsim injector...
	 *
	 * @param guiceModules
	 * @return
	 */
	private static Module insertMapBindings( Module... guiceModules ) {
		final Set<String> modes = new HashSet<>();
		for ( Element element : Elements.getElements( guiceModules ) ) {
			element.acceptVisitor(
					new DefaultElementVisitor<Object>() {
						@Override
						public <T> Object visit( Binding<T> binding ) {
							if ( binding.getKey().getTypeLiteral().getRawType().equals( RoutingModule.class ) ) {
								modes.add( ( (com.google.inject.name.Named) binding.getKey().getAnnotation() ).value() );
							}
							return null;
						}
					} );
		}
		return Modules.combine(
				Modules.combine( guiceModules ), new com.google.inject.AbstractModule() {
					@Override
					protected void configure() {
						MapBinder<String, RoutingModule> routingModuleMultibinder = MapBinder.newMapBinder(
								binder(),
								String.class,
								RoutingModule.class );
						for ( String mode : modes ) {
							routingModuleMultibinder.addBinding( mode ).to(
									Key.get(
											RoutingModule.class,
											Names.named( mode ) ) );
						}
					}
				} );
	}
}
