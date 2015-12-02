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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

import java.lang.reflect.ParameterizedType;

/**
 * @author thibautd
 */
public class InjectionUtils {
	private InjectionUtils() {}

	// As far as I can see, this is safe, even though unchecked: I do not see a way to create something else than the
	// return type.
	@SuppressWarnings( "unchecked" )
	public static <N extends Enum<N>> NestedLogitAccessibilityCalculator<N> createCalculator(
				final TypeLiteral<N> nestType,
				final AbstractModule... modules ) {
		Injector injector = Guice.createInjector( modules );

		final ParameterizedType newType =
				Types.newParameterizedType(
						NestedLogitAccessibilityCalculator.class,
						nestType.getType() );

		return (NestedLogitAccessibilityCalculator<N>) injector.getInstance( Key.get( newType ) );
	}
}
