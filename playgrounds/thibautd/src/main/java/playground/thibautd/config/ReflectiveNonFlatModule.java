/* *********************************************************************** *
 * project: org.matsim.*
 * ReflectiveNonFlatModule.java
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
package playground.thibautd.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.experimental.ReflectiveModule.StringGetter;
import org.matsim.core.config.experimental.ReflectiveModule.StringSetter;

/**
 * This is a dirty copy-paste from ReflectiveModule, Changing only the extends.
 * @author thibautd
 */
public abstract class ReflectiveNonFlatModule extends NonFlatModule {
	private static final Logger log =
		Logger.getLogger(ReflectiveNonFlatModule.class);

	private final boolean storeUnknownParameters;

	private final Map<String, Method> setters;
	private final Map<String, Method> stringGetters;

	// /////////////////////////////////////////////////////////////////////////
	// Construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Creates an instance which will crash if an unknown parameter name
	 * is given.
	 *
	 * @param name the name of the module in the config file.
	 */
	public ReflectiveNonFlatModule(final String name) {
		this( name , false );
	}

	/**
	 * Creates an instance, giving the choice on whether unknown parameter names result
	 * in a crash or just in the parameter value being stored as a String.
	 *
	 * @param name the name of the module in the config file.
	 * @param storeUnknownParametersAsStrings if true, when no annotated getter
	 * or setter is found for a parameter name, the parameters are stored using
	 * the default {@link Module} behavior. This is not that safe, so be careful.
	 */
	public ReflectiveNonFlatModule(
			final String name,
			final boolean storeUnknownParametersAsStrings) {
		super(name);
		this.storeUnknownParameters = storeUnknownParametersAsStrings;
		setters = getSetters();
		stringGetters = getStringGetters();

		if ( !setters.keySet().equals( stringGetters.keySet() ) ) {
			throw new InconsistentModuleException( "setters and getters inconsistent" );
		}
	}

	private Map<String, Method> getStringGetters() {
		final Map<String, Method> gs = new HashMap<String, Method>();
		final Class<? extends ReflectiveNonFlatModule> c = getClass();

		final Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			final StringGetter annotation = m.getAnnotation( StringGetter.class );
			if ( annotation != null ) {
				checkGetterValidity( m );
				final Method old = gs.put( annotation.value() , m );
				if ( old != null ) {
					throw new InconsistentModuleException( "several string getters for "+annotation.value() );
				}
			}
		}

		return gs;
	}

	private static void checkGetterValidity(final Method m) {
		if ( m.getParameterTypes().length > 0 ) {
			throw new InconsistentModuleException( "getter "+m+" has parameters" );
		}

		if ( m.getReturnType().equals( Void.TYPE ) ) {
			throw new InconsistentModuleException( "getter "+m+" has void return type" );
		}
	}

	private Map<String, Method> getSetters() {
		final Map<String, Method> ss = new HashMap<String, Method>();
		final Class<? extends ReflectiveNonFlatModule> c = getClass();

		final Method[] allMethods = c.getDeclaredMethods();

		for (Method m : allMethods) {
			final StringSetter annotation = m.getAnnotation( StringSetter.class );
			if ( annotation != null ) {
				checkSetterValidity( m );
				final Method old = ss.put( annotation.value() , m );
				if ( old != null ) {
					throw new InconsistentModuleException( "several string setters for "+annotation.value() );
				}
			}
		}

		return ss;
	}

	private static void checkSetterValidity(final Method m) {
		final Class<?>[] params = m.getParameterTypes();

		if (params.length != 1) {
			throw new InconsistentModuleException( "setter "+m+" has "+params.length+" parameters instead of one" );
		}

		final Collection<Class<?>> allowedParameterTypes =
			Arrays.<Class<?>>asList(
					String.class,
					Float.class, Double.class, Integer.class, Long.class, Boolean.class, Character.class, Byte.class, Short.class,
					Float.TYPE, Double.TYPE, Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE);
        if ( !allowedParameterTypes.contains( params[ 0 ] ) ) {
			throw new InconsistentModuleException( "setter "+m+" gets a "+params[ 0 ]+". Valid types are String, primitive types and their wrapper classes." );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// module methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public final void addParam(
			final String param_name,
			final String value) {
		final Method setter = setters.get( param_name );

		if (setter == null) {
			if ( !storeUnknownParameters ) {
				throw new IllegalArgumentException(
						"Module "+getName()+" of type "+getClass().getName()+
						" doesn't accept unkown parameters. Parameter "+param_name+
						" is not part of the valid parameters: "+setters.keySet() );
			}
			log.warn( "unknown parameter "+param_name+" for group "+getName()+". Here are the valid parameter names: "+setters.keySet() );
			log.warn( "Only the string value will be remembered" );
			super.addParam( param_name , value );
			return;
		}

		try {
			invokeSetter( setter , value );
			log.trace( "value "+value+" successfully set for field "+param_name+" for group "+getName() );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private void invokeSetter(
			final Method setter,
			final String value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		// do not care about access modifier:
		// if a method is tagged with the StringSetter
		// annotation, we are supposed to access it.
		// This *is* safe.
		final boolean accessible = setter.isAccessible();
		setter.setAccessible( true );

		final Class<?>[] params = setter.getParameterTypes();
		assert params.length == 1; // already checked at constr.

		final Class<?> type = params[ 0 ];

		if ( type.equals( String.class ) ) {
			setter.invoke( this , value );
		}
		else if ( type.equals( Float.class ) || type.equals( Float.TYPE ) ) {
			setter.invoke( this , Float.parseFloat( value ) );
		}
		else if ( type.equals( Double.class ) || type.equals( Double.TYPE ) ) {
			setter.invoke( this , Double.parseDouble( value ) );
		}
		else if ( type.equals( Integer.class ) || type.equals( Integer.TYPE ) ) {
			setter.invoke( this , Integer.parseInt( value ) );
		}
		else if ( type.equals( Long.class ) || type.equals( Long.TYPE ) ) {
			setter.invoke( this , Long.parseLong( value ) );
		}
		else if ( type.equals( Boolean.class ) || type.equals( Boolean.TYPE ) ) {
			setter.invoke( this , Boolean.parseBoolean( value ) );
		}
		else if ( type.equals( Character.class ) || type.equals( Character.TYPE ) ) {
			if ( value.length() != 1 ) throw new IllegalArgumentException( value+" is not a single char!" );
			setter.invoke( this , value.toCharArray()[ 0 ] );
		}
		else if ( type.equals( Byte.class ) || type.equals( Byte.TYPE ) ) {
			setter.invoke( this , Byte.parseByte( value ) );
		}
		else if ( type.equals( Short.class ) || type.equals( Short.TYPE ) ) {
			setter.invoke( this , Short.parseShort( value ) );
		}

		setter.setAccessible( accessible );
	}

	@Override
	public final String getValue(final String param_name) {
		final Method getter = stringGetters.get( param_name );

		try {
			if (getter != null) {
				// do not care about access modifier:
				// if a method is tagged with the StringGetter
				// annotation, we are supposed to access it.
				// This *is* safe.
				final boolean accessible = getter.isAccessible();
				getter.setAccessible( true );
				final String value =  ""+getter.invoke( this );
				getter.setAccessible( accessible );

				return value;
			}
		} catch (Exception e) {
			throw new RuntimeException( e );
		}

		if ( !storeUnknownParameters ) {
			throw new IllegalArgumentException(
					"Module "+getName()+" of type "+getClass().getName()+
					" doesn't store unkown parameters. Parameter "+param_name+
					" is not part of the valid parameters: "+stringGetters.keySet() );
		}

		log.warn( "no getter found for param "+param_name+": trying parent method" );
		return super.getValue( param_name );
	}

	@Override
	public final Map<String, String> getParams() {
		final Map<String, String> map = super.getParams();

		for (String f : setters.keySet()) {
			addParameterToMap( map , f );
		}

		return map;
	}

	public static class InconsistentModuleException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InconsistentModuleException(final String msg) {
			super( msg );
		}
	}
}

