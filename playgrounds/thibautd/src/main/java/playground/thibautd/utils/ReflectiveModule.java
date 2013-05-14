/* *********************************************************************** *
 * project: org.matsim.*
 * ReflectiveModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.config.Module;

/**
 * A module using reflection for easy implementation of config groups.
 * <br>
 * This class takes care of all the housekeeping tasks that you normally have
 * to manually implement when extending module (usually forgetting half of it).
 * <br>
 * For each field in the xml file, just implement a setter taking a String,
 * and a getter returning a value which String representation must be writen to
 * the xml file, and annotate them with the {@link StringSetter} and
 * {@link StringGetter} annotation
 * types.
 * <br>
 * Those annotations take a mandatory String argument, which is the parameter name
 * in the xml.
 * <br>
 * If something is wrong (missing setter or getter, wrong parameter or return type),
 * an {@link InconsistentModuleException} will be thrown at construction.
 *
 * @author thibautd
 */
public abstract class ReflectiveModule extends Module {
	private static final Logger log =
		Logger.getLogger(ReflectiveModule.class);

	private final Map<String, Method> setters;
	private final Map<String, Method> stringGetters;

	// /////////////////////////////////////////////////////////////////////////
	// Construction
	// /////////////////////////////////////////////////////////////////////////
	public ReflectiveModule(final String name) {
		super(name);
		setters = getSetters();
		stringGetters = getStringGetters();

		if ( !setters.keySet().equals( stringGetters.keySet() ) ) {
			throw new InconsistentModuleException( "setters and getters inconsistent" );
		}
	}

	private Map<String, Method> getStringGetters() {
		final Map<String, Method> gs = new HashMap<String, Method>();
		final Class<? extends ReflectiveModule> c = getClass();

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
		final Class<? extends ReflectiveModule> c = getClass();

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
					Float.TYPE, Double.TYPE, Integer.TYPE, Long.TYPE, Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE);;
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
		Method setter = setters.get( param_name );

		if (setter == null) {
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
		Method getter = stringGetters.get( param_name );

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

			log.warn( "no getter found for param "+param_name+": trying parent method" );
			return super.getValue( param_name );
		} catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public final Map<String, String> getParams() {
		final Map<String, String> map = super.getParams();

		for (String f : setters.keySet()) {
			addParameterToMap( map , f );
		}

		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// annotations
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * use to annotate the methods which should be used to read the string
	 * values.
	 * The methods must take one string parameter.
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	public static @interface StringSetter {
		/**
		 * the name of the field in the XML document
		 */
		String value();
	}

	/**
	 * use to annotate the methods which should be used to get the string
	 * values.
	 * the methods must take no parameter and return an Object or primitive
	 * data type which string representations is the one which should appear in the
	 * xml.
	 */
	@Documented
	@Retention( RetentionPolicy.RUNTIME )
	public static @interface StringGetter {
		/**
		 * the name of the field in the XML document
		 */
		String value();
	}

	public static class InconsistentModuleException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private InconsistentModuleException(final String msg) {
			super( msg );
		}
	}
}

